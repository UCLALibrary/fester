
package edu.ucla.library.iiif.fester.verticles;

import static edu.ucla.library.iiif.fester.ObjectType.COLLECTION;
import static edu.ucla.library.iiif.fester.ObjectType.PAGE;
import static edu.ucla.library.iiif.fester.ObjectType.WORK;

import java.io.IOException;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import info.freelibrary.iiif.presentation.Canvas;
import info.freelibrary.iiif.presentation.Collection;
import info.freelibrary.iiif.presentation.ImageContent;
import info.freelibrary.iiif.presentation.ImageResource;
import info.freelibrary.iiif.presentation.Manifest;
import info.freelibrary.iiif.presentation.Sequence;
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
import edu.ucla.library.iiif.fester.CsvParsingException;
import edu.ucla.library.iiif.fester.ImageInfoLookup;
import edu.ucla.library.iiif.fester.LockedManifest;
import edu.ucla.library.iiif.fester.ManifestNotFoundException;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.CodeUtils;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
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

    private String myHost;

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
            final Path filePath = Paths.get(messageBody.getString(Constants.CSV_FILE_PATH));
            final Optional<String> imageHost = Optional.ofNullable(messageBody.getString(Constants.IIIF_HOST));

            if (myHost == null) {
                myHost = messageBody.getString(Constants.FESTER_HOST);
            }

            try (Reader reader = Files.newBufferedReader(filePath); CSVReader csvReader = new CSVReader(reader)) {
                final Map<String, List<String[]>> pages = new HashMap<>();
                final Map<String, List<Collection.Manifest>> works = new HashMap<>();
                final List<String[]> worksMetadata = new ArrayList<>();
                final CsvMetadata csvMetadata = new CsvMetadata(works, worksMetadata, pages);

                CsvHeaders csvHeaders = null;
                Collection collection = null;

                // Read through the CSV data and create store info about collections, works, and pages
                for (final String[] row : csvReader.readAll()) {
                    // The first row should be our headers row
                    if (csvHeaders == null) {
                        // Throw a CsvParsingException here if one of our 'required' headers is missing
                        csvHeaders = new CsvHeaders(row);
                    } else {
                        final int objectTypeIndex = csvHeaders.getObjectTypeIndex();

                        if (COLLECTION.equals(row[objectTypeIndex])) {
                            collection = getCollection(row, csvHeaders);
                        } else if (WORK.equals(row[objectTypeIndex])) {
                            extractWorkMetadata(row, csvHeaders, works, worksMetadata);
                        } else if (PAGE.equals(row[objectTypeIndex])) {
                            extractPageMetadata(row, csvHeaders, pages);
                        }
                    }
                }

                // If we have a collection record in the CSV we're processing, create a collection manifest
                if (collection != null) {
                    LOGGER.debug(MessageCodes.MFS_122, filePath, collection);
                    buildCollectionManifest(collection, csvHeaders, csvMetadata, imageHost, message);
                } else if (worksMetadata.size() > 0) {
                    final String collectionID = worksMetadata.get(0)[csvHeaders.getParentArkIndex()];

                    LOGGER.debug(MessageCodes.MFS_043, filePath);
                    updateWorks(collectionID, csvHeaders, csvMetadata, imageHost, message);
                } else if (pages.size() > 0) {
                    // All our page-only CSVs, at this point, have pages from only one work
                    final String workID = pages.keySet().iterator().next();
                    final List<String[]> pagesList = pages.values().iterator().next();

                    LOGGER.debug(MessageCodes.MFS_069, filePath);
                    updatePages(workID, csvHeaders, pagesList, imageHost, message);
                } else {
                    final CsvParsingException details = new CsvParsingException(MessageCodes.MFS_042);

                    LOGGER.error(details, details.getMessage());
                    message.fail(CodeUtils.getInt(MessageCodes.MFS_000), details.getMessage());
                }
            } catch (final IOException details) {
                LOGGER.error(details, details.getMessage());
                message.fail(CodeUtils.getInt(MessageCodes.MFS_000), details.getMessage());
            } catch (final CsvParsingException details) {
                LOGGER.error(details, details.getMessage());
                message.fail(CodeUtils.getInt(MessageCodes.MFS_000), details.getMessage());
            } catch (final CsvException details) {
                LOGGER.error(details, details.getMessage());
                message.fail(CodeUtils.getInt(MessageCodes.MFS_000), details.getMessage());
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
    private void updatePages(final String aWorkID, final CsvHeaders aCsvHeaders, final List<String[]> aPagesList,
            final Optional<String> aImageHost, final Message<JsonObject> aMessage) {
        final Promise<LockedManifest> promise = Promise.promise();
        final String encodedWorkID = URLEncoder.encode(aWorkID, StandardCharsets.UTF_8);

        promise.future().setHandler(handler -> {
            if (handler.succeeded()) {
                final LockedManifest lockedManifest = handler.result();
                final Manifest manifest = lockedManifest.getWork();
                final List<Sequence> sequences = manifest.getSequences();
                final Sequence sequence;

                // If the work doesn't already have any sequences, create one for it
                if (sequences.size() == 0) {
                    final String sequenceID = StringUtils.format(SEQUENCE_URI, myHost, encodedWorkID);

                    sequence = new Sequence().setID(sequenceID);
                    manifest.addSequence(sequence);
                } else {
                    // For now we're just dealing with single sequence works
                    sequence = sequences.get(0);
                }

                aPagesList.sort(new ItemSequenceComparator(aCsvHeaders.getItemSequenceIndex()));

                try {
                    addPages(aCsvHeaders, aPagesList, sequence, aImageHost, encodedWorkID);

                    sendMessage(manifest.toJSON(), S3BucketVerticle.class.getName(), send -> {
                        if (send.succeeded()) {
                            aMessage.reply(Op.SUCCESS);
                        } else {
                            final Throwable details = send.cause();
                            final String errorMessage = details.getMessage();

                            LOGGER.error(details, MessageCodes.MFS_052, errorMessage);
                            aMessage.fail(CodeUtils.getInt(MessageCodes.MFS_052), errorMessage);
                        }
                    });
                } catch (final IOException details) {
                    aMessage.fail(CodeUtils.getInt(MessageCodes.MFS_052), details.getMessage());
                } finally {
                    lockedManifest.release();
                }
            } else {
                aMessage.fail(CodeUtils.getInt(MessageCodes.MFS_052), handler.cause().getMessage());
            }
        });

        getLockedManifest(aWorkID, false, promise);
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
        promise.future().setHandler(handler -> {
            if (handler.succeeded()) {
                final LockedManifest lockedManifest = handler.result();
                final Collection collectionToUpdate = lockedManifest.getCollection();
                final JsonObject manifestJSON = updateCollection(collectionToUpdate, aCsvMetadata.getWorksMap());

                sendMessage(manifestJSON, S3BucketVerticle.class.getName(), update -> {
                    lockedManifest.release();

                    if (update.succeeded()) {
                        processWorks(aCsvHeaders, aCsvMetadata, aImageHost, aMessage);
                    } else {
                        aMessage.fail(CodeUtils.getInt(MessageCodes.MFS_052), update.cause().getMessage());
                    }
                });
            } else {
                aMessage.fail(CodeUtils.getInt(MessageCodes.MFS_052), handler.cause().getMessage());
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
        final List<Collection.Manifest> manifestList = aCollection.getManifests();
        final String collectionID = IDUtils.decode(aCollection.getID(), Constants.COLLECTIONS_PATH);
        final List<Collection.Manifest> manifests = aWorksMap.get(collectionID);

        for (int manifestIndex = 0; manifestIndex < manifests.size(); manifestIndex++) {
            final Collection.Manifest manifest = manifests.get(manifestIndex);
            final String manifestID = IDUtils.decode(manifest.getID());

            boolean found = false;

            for (int listIndex = 0; listIndex < manifestList.size(); listIndex++) {
                final Collection.Manifest existingManifest = manifestList.get(listIndex);
                final String existingID = IDUtils.decode(existingManifest.getID());

                if (existingID.equals(manifestID)) {
                    final String removedID = manifestList.remove(listIndex).getID().toString();

                    manifestList.add(listIndex, manifest);
                    found = true;

                    LOGGER.debug(MessageCodes.MFS_132, removedID, manifest.getID().toString());
                    break;
                }
            }

            if (!found) {
                manifestList.add(manifest);
            }
        }

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
                    String id = URLEncoder.encode(aID, StandardCharsets.UTF_8);

                    // If we have a collection manifest, add a directory path to it
                    if (aCollDoc) {
                        id = StringUtils.format(SIMPLE_URI, Constants.COLLECTIONS_PATH, id);
                    }

                    getS3Manifest(id, S3BucketVerticle.class.getName(), handler -> {
                        if (handler.succeeded()) {
                            final JsonObject manifest = handler.result().body();
                            final Lock lock = lockRequest.result();

                            aPromise.complete(new LockedManifest(manifest, aCollDoc, lock));
                        } else {
                            lockRequest.result().release();
                            aPromise.fail(handler.cause());
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
        final String collectionID = IDUtils.decode(aCollection.getID(), Constants.COLLECTIONS_PATH);
        final List<Collection.Manifest> manifests = aCsvMetadata.getWorksMap().get(collectionID);
        final Promise<Void> promise = Promise.promise();

        // If we have work manifests, add them to the collection manifest
        if (manifests != null) {
            manifestList.addAll(manifests);
        } else {
            LOGGER.warn(MessageCodes.MFS_118, collectionID);
        }

        // Create a handler to handle generating work manifests after the collection manage has been uploaded
        promise.future().setHandler(handler -> {
            if (handler.succeeded()) {
                processWorks(aCsvHeaders, aCsvMetadata, aImageHost, aMessage);
            } else {
                final int failCode = CodeUtils.getInt(MessageCodes.MFS_125);
                final String failMessage = handler.cause().getMessage();

                LOGGER.error(MessageCodes.MFS_125, failMessage);
                aMessage.fail(failCode, failMessage);
            }
        });

        // Send collection manifest to S3
        sendMessage(aCollection.toJSON(), S3BucketVerticle.class.getName(), send -> {
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
        CompositeFuture.all(futures).setHandler(worksHandler -> {
            if (worksHandler.succeeded()) {
                aMessage.reply(LOGGER.getMessage(MessageCodes.MFS_126));
            } else {
                final Throwable cause = worksHandler.cause();
                final String message = LOGGER.getMessage(MessageCodes.MFS_131, cause.getMessage());

                LOGGER.error(cause, message);
                aMessage.fail(CodeUtils.getInt(MessageCodes.MFS_131), message);
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
        final String manifestID = StringUtils.format(MANIFEST_URI, myHost, urlEncodedWorkID);
        final Manifest manifest = new Manifest(manifestID, workLabel);
        final String sequenceID = StringUtils.format(SEQUENCE_URI, myHost, urlEncodedWorkID);
        final Sequence sequence = new Sequence().setID(sequenceID);

        try {
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

            if (aPages.containsKey(workID)) {
                final List<String[]> pageList = aPages.get(workID);

                manifest.addSequence(sequence);
                pageList.sort(new ItemSequenceComparator(aCsvHeaders.getItemSequenceIndex()));

                addPages(aCsvHeaders, pageList, sequence, aImageHost, urlEncodedWorkID);
            }

            sendMessage(manifest.toJSON(), S3BucketVerticle.class.getName(), send -> {
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
            final String canvasID = StringUtils.format(CANVAS_URI, myHost, aWorkID, idPart);
            final String pageURI = StringUtils.format(SIMPLE_URI, imageHost, encodedPageID);
            final String annotationURI = StringUtils.format(ANNOTATION_URI, myHost, aWorkID, idPart);
            final String resourceURI = pageURI + DEFAULT_IMAGE_URI; // Copying Samvera's default image link
            final ImageResource imageResource = new ImageResource(resourceURI, new ImageInfoService(pageURI));
            final ImageContent imageContent;

            Canvas canvas;

            try {
                final ImageInfoLookup infoLookup = new ImageInfoLookup(pageURI);

                canvas = new Canvas(canvasID, pageLabel, infoLookup.getWidth(), infoLookup.getHeight());
            } catch (final ManifestNotFoundException details) {
                final int width;
                final int height;

                // First check the last canvas that we've processed (if there is one)
                if (lastCanvas != null) {
                    width = lastCanvas.getWidth();
                    height = lastCanvas.getHeight();
                } else {
                    // If we've not processed any, check to sequence to find one
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
                final String viewingHint = StringUtils.trimToNull(columns[aCsvHeaders.getTitleIndex()]);

                if (viewingHint != null) {
                    canvas.setViewingHint(new ViewingHint(viewingHint));
                }
            }

            aSequence.addCanvas(canvas);
        }
    }

    /**
     * Add a Work manifest.
     *
     * @param aRow A CSV row representing a Work
     * @param aHeaders The CSV headers
     * @param aWorksMap A collection of Work manifests
     * @throws CsvParsingException If there is trouble getting the necessary info from the CSV
     */
    private void extractWorkMetadata(final String[] aRow, final CsvHeaders aHeaders,
            final Map<String, List<Collection.Manifest>> aWorksMap, final List<String[]> aWorksList)
            throws CsvParsingException {
        final String id = StringUtils.trimToNull(aRow[aHeaders.getItemArkIndex()]);
        final String parentID = StringUtils.trimToNull(aRow[aHeaders.getParentArkIndex()]);
        final String label = StringUtils.trimToNull(aRow[aHeaders.getTitleIndex()]);

        // Store the work data for full manifest creation
        aWorksList.add(aRow);

        // Create a brief work manifest for inclusion in the collection manifest
        if (id != null && label != null) {
            final Collection.Manifest manifest = new Collection.Manifest(IDUtils.encode(myHost, id), label);

            LOGGER.debug(MessageCodes.MFS_119, id, parentID);

            if (parentID != null) {
                if (aWorksMap.containsKey(parentID)) {
                    aWorksMap.get(parentID).add(manifest);
                } else {
                    final List<Collection.Manifest> manifests = new ArrayList<>();

                    manifests.add(manifest);
                    aWorksMap.put(parentID, manifests);
                }
            } else {
                throw new CsvParsingException(MessageCodes.MFS_107);
            }
        } else {
            throw new CsvParsingException(MessageCodes.MFS_108);
        }
    }

    /**
     * Add a page's metadata to our pages map for later processing.
     *
     * @param aRow A metadata row
     * @param aHeaders A CSV headers object
     * @param aPageMap A map of pages
     * @throws CsvParsingException
     */
    private void extractPageMetadata(final String[] aRow, final CsvHeaders aHeaders,
            final Map<String, List<String[]>> aPageMap) throws CsvParsingException {
        final String parentID = StringUtils.trimToNull(aRow[aHeaders.getParentArkIndex()]);

        if (parentID != null) {
            if (aPageMap.containsKey(parentID)) {
                aPageMap.get(parentID).add(aRow);
            } else {
                final List<String[]> page = new ArrayList<>();

                page.add(aRow);
                aPageMap.put(parentID, page);
            }
        } else {
            throw new CsvParsingException(MessageCodes.MFS_121);
        }
    }

    /**
     * Get the collection object from the collection row.
     *
     * @param aRow A row of collection metadata
     * @param aHeaders A CSV headers object
     * @return A collection
     */
    private Collection getCollection(final String[] aRow, final CsvHeaders aHeaders) throws CsvParsingException {
        final String id = StringUtils.trimToNull(aRow[aHeaders.getItemArkIndex()]);

        if (id != null) {
            final String label = StringUtils.trimToNull(aRow[aHeaders.getTitleIndex()]);

            if (label != null) {
                return new Collection(IDUtils.encode(myHost, Constants.COLLECTIONS_PATH, id), label);
            } else {
                throw new CsvParsingException(MessageCodes.MFS_104);
            }
        } else {
            throw new CsvParsingException(MessageCodes.MFS_106);
        }
    }
}
