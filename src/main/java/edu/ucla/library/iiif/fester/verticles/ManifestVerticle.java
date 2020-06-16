
package edu.ucla.library.iiif.fester.verticles;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.opencsv.exceptions.CsvException;

import info.freelibrary.iiif.presentation.Canvas;
import info.freelibrary.iiif.presentation.Collection;
import info.freelibrary.iiif.presentation.ImageContent;
import info.freelibrary.iiif.presentation.ImageResource;
import info.freelibrary.iiif.presentation.Manifest;
import info.freelibrary.iiif.presentation.Sequence;
import info.freelibrary.iiif.presentation.properties.Attribution;
import info.freelibrary.iiif.presentation.properties.Metadata;
import info.freelibrary.iiif.presentation.properties.ViewingDirection;
import info.freelibrary.iiif.presentation.properties.ViewingHint;
import info.freelibrary.iiif.presentation.services.ImageInfoService;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.CsvHeaders;
import edu.ucla.library.iiif.fester.CsvMetadata;
import edu.ucla.library.iiif.fester.CsvParser;
import edu.ucla.library.iiif.fester.CsvParsingException;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.ImageInfoLookup;
import edu.ucla.library.iiif.fester.ImageNotFoundException;
import edu.ucla.library.iiif.fester.LockedManifest;
import edu.ucla.library.iiif.fester.ManifestNotFoundException;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import edu.ucla.library.iiif.fester.utils.ItemSequenceComparator;
import edu.ucla.library.iiif.fester.utils.ManifestLabelComparator;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.shareddata.SharedData;

/**
 * A creator of manifests (collection and work).
 */
public class ManifestVerticle extends AbstractFesterVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestVerticle.class, Constants.MESSAGES);

    private static final String SEQUENCE_URI = "{}/{}/manifest/sequence/normal";

    private static final String CANVAS_URI = "{}/{}/manifest/canvas/{}";

    private static final String ANNOTATION_URI = "{}/{}/annotation/{}";

    private static final String SIMPLE_URI = "{}/{}";

    private static final String DEFAULT_IMAGE_URI = "/full/600,/0/default.jpg";

    private static final String MANIFEST_URI = "{}/{}/manifest";

    private String myImageHost;

    /**
     * Starts the collection manifester.
     */
    @Override
    public void start(final Promise<Void> aPromise) {
        if (myImageHost == null) {
            myImageHost = config().getString(Config.IIIF_BASE_URL);
        }

        getJsonConsumer().handler(message -> {
            final JsonObject messageBody = message.body();
            final String action = message.headers().get(Constants.ACTION);

            if (Op.POST_CSV.equals(action)) {
                final Path filePath = Paths.get(messageBody.getString(Constants.CSV_FILE_PATH));
                final Optional<String> imageHost = Optional.ofNullable(messageBody.getString(Constants.IIIF_HOST));

                try {
                    final CsvParser csvParser = new CsvParser().parse(filePath);
                    final Optional<Collection> collection = csvParser.getCollection();
                    final CsvHeaders csvHeaders = csvParser.getCsvHeaders();
                    final CsvMetadata csvMetadata = csvParser.getCsvMetadata();

                    // If we have a collection record in the CSV we're processing, create a collection manifest
                    if (collection.isPresent()) {
                        LOGGER.debug(MessageCodes.MFS_122, filePath, collection);
                        buildCollectionManifest(collection.get(), csvHeaders, csvMetadata, imageHost, message);
                    } else if (csvMetadata.hasWorks()) {
                        final Optional<String> id = csvMetadata.getCollectionID(csvHeaders.getParentArkIndex());

                        LOGGER.debug(MessageCodes.MFS_043, filePath);
                        updateWorks(id.get(), csvHeaders, csvMetadata, imageHost, message);
                    } else if (csvMetadata.hasPages()) {
                        final Iterator<Entry<String, List<String[]>>> iterator = csvMetadata.getPageIterator();
                        final List<Future> futures = new ArrayList<>();

                        LOGGER.debug(MessageCodes.MFS_069, filePath);

                        while (iterator.hasNext()) {
                            final Entry<String, List<String[]>> pageEntry = iterator.next();
                            final List<String[]> pagesList = pageEntry.getValue();
                            final String workID = pageEntry.getKey();

                            futures.add(updatePages(workID, csvHeaders, pagesList, imageHost, Promise.promise()));
                        }

                        CompositeFuture.all(futures).onComplete(handler -> {
                            if (handler.succeeded()) {
                                message.reply(Op.SUCCESS);
                            } else {
                                final Throwable throwable = handler.cause();

                                LOGGER.error(throwable, throwable.getMessage());
                                message.fail(HTTP.INTERNAL_SERVER_ERROR, throwable.getMessage());
                            }
                        });
                    } else {
                        final Exception details = new CsvParsingException(MessageCodes.MFS_042);

                        LOGGER.error(details, details.getMessage());
                        message.fail(HTTP.BAD_REQUEST, details.getMessage());
                    }
                } catch (final IOException details) {
                    LOGGER.error(details, details.getMessage());
                    message.fail(HTTP.INTERNAL_SERVER_ERROR, details.getMessage());
                } catch (final CsvParsingException | CsvException details) {
                    LOGGER.error(details, details.getMessage());
                    message.fail(HTTP.BAD_REQUEST, details.getMessage());
                }
            } else {
                final String jsonMsg = message.toString();
                final String verticleName = getClass().getSimpleName();
                final String errorMessage = StringUtils.format(MessageCodes.MFS_139, verticleName, jsonMsg, action);

                message.fail(HTTP.INTERNAL_SERVER_ERROR, errorMessage);
            }
        });

        aPromise.complete();
    }

    /**
     * Update work manifest pages with data from an uploaded CSV.
     *
     * @param aWorkID A work ID
     * @param aPagesList A list of pages
     * @param aMessage A message
     */
    private Future<Void> updatePages(final String aWorkID, final CsvHeaders aCsvHeaders,
            final List<String[]> aPagesList, final Optional<String> aImageHost, final Promise<Void> aPromise) {
        final Promise<LockedManifest> promise = Promise.promise();
        final String encodedWorkID = URLEncoder.encode(aWorkID, StandardCharsets.UTF_8);

        promise.future().onComplete(handler -> {
            if (handler.succeeded()) {
                final LockedManifest lockedManifest = handler.result();
                final Manifest manifest = lockedManifest.getWork();
                final List<Sequence> sequences = manifest.getSequences();
                final Sequence sequence;
                final JsonObject message = new JsonObject();
                final DeliveryOptions options = new DeliveryOptions();

                // If the work doesn't already have any sequences, create one for it
                if (sequences.size() == 0) {
                    final String seqID = StringUtils.format(SEQUENCE_URI, Constants.URL_PLACEHOLDER, encodedWorkID);

                    sequence = new Sequence().setID(seqID);
                    manifest.addSequence(sequence);
                } else {
                    // For now we're just dealing with single sequence works
                    sequence = sequences.get(0);
                }

                sequence.getCanvases().clear(); // overwrite whatever is on the manifest already
                aPagesList.sort(new ItemSequenceComparator(aCsvHeaders.getItemSequenceIndex()));

                try {
                    addPages(aCsvHeaders, aPagesList, sequence, aImageHost, encodedWorkID);

                    message.put(Constants.MANIFEST_ID, aWorkID).put(Constants.DATA, manifest.toJSON());
                    options.addHeader(Constants.ACTION, Op.PUT_MANIFEST);

                    sendMessage(S3BucketVerticle.class.getName(), message, options, send -> {
                        if (send.succeeded()) {
                            aPromise.complete();
                        } else {
                            aPromise.fail(send.cause());
                        }
                    });
                } catch (final IOException details) {
                    aPromise.fail(details);
                } finally {
                    lockedManifest.release();
                }
            } else {
                aPromise.fail(handler.cause());
            }
        });

        getLockedManifest(aWorkID, false, promise);
        return aPromise.future();
    }

    /**
     * Update the works associated with a supplied collection.
     *
     * @param aCollectionID A collection ID
     * @param aCsvHeaders Headers from the supplied CSV file
     * @param aCsvMetadata Metadata from the supplied CSV file
     * @param aImageHost An image host
     * @param aMessage A message
     */
    private void updateWorks(final String aCollectionID, final CsvHeaders aCsvHeaders, final CsvMetadata aCsvMetadata,
            final Optional<String> aImageHost, final Message<JsonObject> aMessage) {
        final Promise<LockedManifest> promise = Promise.promise();

        // If we were able to get a lock on the manifest, update it with our new works
        promise.future().onComplete(handler -> {
            if (handler.succeeded()) {
                final LockedManifest lockedManifest = handler.result();
                final Collection collectionToUpdate = lockedManifest.getCollection();
                final JsonObject manifestJSON = updateCollection(collectionToUpdate, aCsvMetadata.getWorksMap());
                final JsonObject message = new JsonObject();
                final DeliveryOptions options = new DeliveryOptions();

                message.put(Constants.COLLECTION_NAME, aCollectionID).put(Constants.DATA, manifestJSON);
                options.addHeader(Constants.ACTION, Op.PUT_COLLECTION);

                sendMessage(S3BucketVerticle.class.getName(), message, options, update -> {
                    lockedManifest.release();

                    if (update.succeeded()) {
                        processWorks(aCsvHeaders, aCsvMetadata, aImageHost, aMessage);
                    } else {
                        aMessage.fail(HTTP.INTERNAL_SERVER_ERROR, update.cause().getMessage());
                    }
                });
            } else {
                aMessage.fail(HTTP.INTERNAL_SERVER_ERROR, handler.cause().getMessage());
            }
        });

        getLockedManifest(aCollectionID, true, promise);
    }

    /**
     * Update a collection with new works.
     *
     * @param aCollection A collection to be updated
     * @return The updated collection
     */
    private JsonObject updateCollection(final Collection aCollection,
            final Map<String, List<Collection.Manifest>> aWorksMap) {

        // Keep track of the manifests we want to add to, or update on, the IIIF collection
        final Map<URI, Collection.Manifest> manifestMap = new HashMap<>();
        final SortedSet<Collection.Manifest> sortedManifestSet = new TreeSet<>(new ManifestLabelComparator());

        // First, add the old manifests to the map
        manifestMap.putAll(aCollection.getManifests().stream()
                .collect(Collectors.toMap(Collection.Manifest::getID, collection -> collection)));

        // Next, add the new manifests to the map, replacing any that already exist
        manifestMap.putAll(aWorksMap.get(IDUtils.getResourceID(aCollection.getID())).stream()
                .collect(Collectors.toMap(Collection.Manifest::getID, collection -> collection)));

        // Update the manifest list with the manifests in the map, ordered by their label
        sortedManifestSet.addAll(manifestMap.values());

        aCollection.getManifests().clear();
        aCollection.getManifests().addAll(sortedManifestSet);

        return aCollection.toJSON();
    }

    /**
     * Tries to lock an S3 manifest so we can update it.
     *
     * @param aID A manifest ID
     * @param aCollDoc Whether the manifest is a collection (or a work)
     * @param aPromise A promise that we'll get a lock
     */
    private void getLockedManifest(final String aID, final boolean aCollDoc, final Promise<LockedManifest> aPromise) {
        final SharedData sharedData = vertx.sharedData();

        // Try to get the lock for a second
        sharedData.getLocalLockWithTimeout(aID, 1000, lockRequest -> {
            if (lockRequest.succeeded()) {
                try {
                    final JsonObject message = new JsonObject();
                    final DeliveryOptions options = new DeliveryOptions().addHeader(Constants.NO_REWRITE_URLS,
                            Boolean.TRUE.toString());

                    if (aCollDoc) {
                        message.put(Constants.COLLECTION_NAME, aID);
                        options.addHeader(Constants.ACTION, Op.GET_COLLECTION);
                    } else {
                        message.put(Constants.MANIFEST_ID, aID);
                        options.addHeader(Constants.ACTION, Op.GET_MANIFEST);
                    }

                    sendMessage(S3BucketVerticle.class.getName(), message, options, handler -> {
                        if (handler.succeeded()) {
                            final JsonObject manifest = handler.result().body();
                            final Lock lock = lockRequest.result();

                            aPromise.complete(new LockedManifest(manifest, aCollDoc, lock));
                        } else {
                            lockRequest.result().release();
                            aPromise.fail(new ManifestNotFoundException(handler.cause(), MessageCodes.MFS_146,
                                    aCollDoc ? "collection" : "work", aID));
                        }
                    });
                } catch (final NullPointerException | IndexOutOfBoundsException details) {
                    lockRequest.result().release();
                    aPromise.fail(details);
                }
            } else {
                // If we can't get a lock, keep trying (forever, really?)
                vertx.setTimer(1000, timer -> {
                    getLockedManifest(aID, aCollDoc, aPromise);
                });
            }
        });
    }

    /**
     * Builds the collection manifest.
     *
     * @param aCollection A collection
     * @param aCsvHeaders Headers from a CSV file
     * @param aCsvMetadata Metadata from a CSV file
     * @param aMessage The verticle response message
     */
    private void buildCollectionManifest(final Collection aCollection, final CsvHeaders aCsvHeaders,
            final CsvMetadata aCsvMetadata, final Optional<String> aImageHost, final Message<JsonObject> aMessage) {
        final List<Collection.Manifest> manifestList = aCollection.getManifests(); // Empty list
        final String collectionID = IDUtils.getResourceID(aCollection.getID());
        final List<Collection.Manifest> manifests = aCsvMetadata.getWorksMap().get(collectionID);
        final Promise<Void> promise = Promise.promise();
        final JsonObject message = new JsonObject();
        final DeliveryOptions options = new DeliveryOptions();

        // If we have work manifests, add them to the collection manifest
        if (manifests != null) {
            manifestList.addAll(manifests);
        } else {
            LOGGER.warn(MessageCodes.MFS_118, collectionID);
        }

        message.put(Constants.COLLECTION_NAME, collectionID).put(Constants.DATA, aCollection.toJSON());
        options.addHeader(Constants.ACTION, Op.PUT_COLLECTION);

        // Create a handler to handle generating work manifests after the collection manage has been uploaded
        promise.future().onComplete(handler -> {
            if (handler.succeeded()) {
                processWorks(aCsvHeaders, aCsvMetadata, aImageHost, aMessage);
            } else {
                final String failMessage = handler.cause().getMessage();

                LOGGER.error(MessageCodes.MFS_125, failMessage);
                aMessage.fail(HTTP.INTERNAL_SERVER_ERROR, failMessage);
            }
        });

        // Send collection manifest to S3
        sendMessage(S3BucketVerticle.class.getName(), message, options, send -> {
            if (send.succeeded()) {
                promise.complete();
            } else {
                promise.fail(send.cause());
            }
        });
    }

    private void processWorks(final CsvHeaders aHeaders, final CsvMetadata aCsvMetadata,
            final Optional<String> aImageHost, final Message<JsonObject> aMessage) {
        final Map<String, List<String[]>> aPagesMap = aCsvMetadata.getPagesMap();
        final List<String[]> aWorksDataList = aCsvMetadata.getWorksList();
        final Iterator<String[]> iterator = aWorksDataList.iterator();
        final List<Future> futures = new ArrayList<>();

        // Request each work manifest be created
        while (iterator.hasNext()) {
            futures.add(buildWorkManifest(aHeaders, iterator.next(), aPagesMap, aImageHost, Promise.promise()));
        }

        // Keep track of our progress and fail our promise if we don't succeed
        CompositeFuture.all(futures).onComplete(handler -> {
            if (handler.succeeded()) {
                aMessage.reply(LOGGER.getMessage(MessageCodes.MFS_126));
            } else {
                final Throwable cause = handler.cause();
                final String message = LOGGER.getMessage(MessageCodes.MFS_131, cause.getMessage());

                LOGGER.error(cause, message);
                aMessage.fail(HTTP.INTERNAL_SERVER_ERROR, message);
            }
        });
    }

    /**
     * Build an individual work manifest.
     *
     * @param aCsvHeaders The CSV headers
     * @param aWork A metadata array representing the work
     * @param aPages A list of pages
     * @param aPromise A promise we'll create the work manifest
     * @return The future result of our promise
     */
    private Future buildWorkManifest(final CsvHeaders aCsvHeaders, final String[] aWork,
            final Map<String, List<String[]>> aPages, final Optional<String> aImageHost,
            final Promise<Void> aPromise) {
        final String workID = aWork[aCsvHeaders.getItemArkIndex()];
        final String urlEncodedWorkID = URLEncoder.encode(workID, StandardCharsets.UTF_8);
        final String workLabel = aWork[aCsvHeaders.getTitleIndex()];
        final Metadata metadata = new Metadata();
        final String manifestID = StringUtils.format(MANIFEST_URI, Constants.URL_PLACEHOLDER, urlEncodedWorkID);
        final Manifest manifest = new Manifest(manifestID, workLabel);
        final String sequenceID = StringUtils.format(SEQUENCE_URI, Constants.URL_PLACEHOLDER, urlEncodedWorkID);
        final Sequence sequence = new Sequence().setID(sequenceID);
        final JsonObject message = new JsonObject();
        final DeliveryOptions options = new DeliveryOptions();

        try {
            // Add optional properties
            if (aCsvHeaders.hasViewingDirectionIndex()) {
                final String viewingDirection = StringUtils.trimToNull(aWork[aCsvHeaders.getViewingDirectionIndex()]);

                if (viewingDirection != null) {
                    manifest.setViewingDirection(ViewingDirection.fromString(viewingDirection));
                }
            }

            if (aCsvHeaders.hasViewingHintIndex()) {
                final String viewingHint = StringUtils.trimToNull(aWork[aCsvHeaders.getViewingHintIndex()]);

                if (viewingHint != null) {
                    manifest.setViewingHint(new ViewingHint(viewingHint));
                }
            }

            if (aCsvHeaders.hasRepositoryNameIndex()) {
                final String repositoryName = StringUtils.trimToNull(aWork[aCsvHeaders.getRepositoryNameIndex()]);

                if (repositoryName != null) {
                    metadata.add(Constants.REPOSITORY_NAME_METADATA_LABEL, repositoryName);
                }
            }

            if (aCsvHeaders.hasLocalRightsStatementIndex()) {
                final String localRightsStatement = StringUtils.trimToNull(aWork[aCsvHeaders
                        .getLocalRightsStatementIndex()]);

                if (localRightsStatement != null) {
                    manifest.setAttribution(new Attribution(localRightsStatement));
                }
            }

            if (aCsvHeaders.hasRightsContactIndex()) {
                final String rightsContact = StringUtils.trimToNull(aWork[aCsvHeaders.getRightsContactIndex()]);

                if (rightsContact != null) {
                    metadata.add(Constants.RIGHTS_CONTACT_METADATA_LABEL, rightsContact);
                }
            }

            if (metadata.getEntries().size() > 0) {
                manifest.setMetadata(metadata);
            }

            // Check first for pages, then if the work itself is an image
            if (aPages.containsKey(workID)) {
                final List<String[]> pageList = aPages.get(workID);

                manifest.addSequence(sequence);
                pageList.sort(new ItemSequenceComparator(aCsvHeaders.getItemSequenceIndex()));

                addPages(aCsvHeaders, pageList, sequence, aImageHost, urlEncodedWorkID);
            } else if (aCsvHeaders.hasImageAccessUrlIndex()) {
                final String accessURL = StringUtils.trimToNull(aWork[aCsvHeaders.getImageAccessUrlIndex()]);

                if (accessURL != null) {
                    final List<String[]> pageList = new ArrayList<>(1);

                    pageList.add(aWork);
                    manifest.addSequence(sequence);

                    addPages(aCsvHeaders, pageList, sequence, aImageHost, urlEncodedWorkID);
                }
            }

            message.put(Constants.MANIFEST_ID, workID).put(Constants.DATA, manifest.toJSON());
            options.addHeader(Constants.ACTION, Op.PUT_MANIFEST);

            sendMessage(S3BucketVerticle.class.getName(), message, options, send -> {
                if (send.succeeded()) {
                    aPromise.complete();
                } else {
                    aPromise.fail(send.cause());
                }
            });
        } catch (final IllegalArgumentException details) {
            LOGGER.warn(MessageCodes.MFS_074, workID, details.getMessage());
            aPromise.fail(details);
        } catch (final IOException details) {
            aPromise.fail(details);
        }

        // Return our promise's future result
        return aPromise.future();
    }

    /**
     * Adds pages to a sequence from a work manifest.
     *
     * @param aCsvHeaders A CSV headers
     * @param aPageList A list of pages to add
     * @param aSequence A sequence to add pages to
     * @param aImageHost An image host for image links
     * @param aWorkID A URL encoded work ID
     * @throws IOException If there is trouble adding a page
     */
    private void addPages(final CsvHeaders aCsvHeaders, final List<String[]> aPageList, final Sequence aSequence,
            final Optional<String> aImageHost, final String aWorkID) throws IOException {
        final Iterator<String[]> iterator = aPageList.iterator();
        final String imageHost = aImageHost.orElse(myImageHost);

        Canvas lastCanvas = null;

        while (iterator.hasNext()) {
            final String[] columns = iterator.next();
            final String pageID = columns[aCsvHeaders.getItemArkIndex()];
            final String idPart = IDUtils.getLastPart(pageID); // We're just copying Samvera here
            final String encodedPageID = URLEncoder.encode(pageID, StandardCharsets.UTF_8);
            final String pageLabel = columns[aCsvHeaders.getTitleIndex()];
            final String canvasID = StringUtils.format(CANVAS_URI, Constants.URL_PLACEHOLDER, aWorkID, idPart);
            final String pageURI = StringUtils.format(SIMPLE_URI, imageHost, encodedPageID);
            final String annotationURI = StringUtils.format(ANNOTATION_URI, Constants.URL_PLACEHOLDER, aWorkID,
                    idPart);
            final String resourceURI = pageURI + DEFAULT_IMAGE_URI; // Copying Samvera's default image link
            final ImageResource imageResource = new ImageResource(resourceURI, new ImageInfoService(pageURI));
            final ImageContent imageContent;

            Canvas canvas;

            try {
                final ImageInfoLookup infoLookup = new ImageInfoLookup(pageURI);

                // Create a canvas using the width and height of the related image
                canvas = new Canvas(canvasID, pageLabel, infoLookup.getWidth(), infoLookup.getHeight());
            } catch (final ImageNotFoundException details) {
                final int width;
                final int height;

                // Note that we couldn't find the image and are trying to provide a workaround
                LOGGER.debug(MessageCodes.MFS_078);

                // First check the last canvas that we've processed (if there is one)
                if (lastCanvas != null) {
                    width = lastCanvas.getWidth();
                    height = lastCanvas.getHeight();
                } else {
                    // If we've not processed any, check the sequence to find one
                    final List<Canvas> canvases = aSequence.getCanvases();

                    // If there is one use that; else, just use zeros for the w/h values
                    if (canvases.size() != 0) {
                        final Canvas altLastCanvas = canvases.get(canvases.size() - 1);

                        width = altLastCanvas.getWidth();
                        height = altLastCanvas.getHeight();
                    } else {
                        LOGGER.warn(MessageCodes.MFS_073, pageURI);

                        width = 0;
                        height = 0;
                    }
                }

                canvas = new Canvas(canvasID, pageLabel, width, height);
                lastCanvas = canvas;
            }

            imageContent = new ImageContent(annotationURI, canvas);
            imageContent.addResource(imageResource);
            canvas.addImageContent(imageContent);

            if (aCsvHeaders.hasViewingHintIndex()) {
                final String viewingHint = StringUtils.trimToNull(columns[aCsvHeaders.getViewingHintIndex()]);

                if (viewingHint != null) {
                    canvas.setViewingHint(new ViewingHint(viewingHint));
                }
            }

            aSequence.addCanvas(canvas);
        }
    }
}
