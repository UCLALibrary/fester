
package edu.ucla.library.iiif.fester.verticles;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.exceptions.CsvException;

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
import edu.ucla.library.iiif.fester.LockedIiifResource;
import edu.ucla.library.iiif.fester.ManifestNotFoundException;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.shareddata.SharedData;

/**
 * A verticle to parse the incoming CSV data and hand the request off to the appropriate manifester.
 */
public class ManifestVerticle extends AbstractFesterVerticle {

    /**
     * An action value that indicates pages should be updated.
     */
    public static final String UPDATE_PAGES = "update-pages";

    /**
     * An action value that indicates pages should be added.
     */
    public static final String ADD_PAGES = "add-pages";

    /**
     * An action value that indicates work metadata should be updated.
     */
    public static final String UPDATE_WORK = "update-work";

    /**
     * An action value that indicates a collection should be updated.
     */
    public static final String UPDATE_COLLECTION = "update-collection";

    /**
     * An action value that indicates a collection should be created.
     */
    public static final String CREATE_COLLECTION = "create-collection";

    /**
     * An action value that indicates a new work should be created.
     */
    public static final String CREATE_WORK = "create-work";

    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestVerticle.class, Constants.MESSAGES);

    private static final long TIMEOUT = Long.MAX_VALUE; // A temporary over the top setting for image lookups

    private String myPlaceholderImage;

    private String myImageHost;

    /**
     * Starts a verticle to handle manifest creation requests.
     */
    @Override
    public void start(final Promise<Void> aPromise) {
        if (myImageHost == null) {
            myImageHost = StringUtils.trimToNull(config().getString(Config.IIIF_BASE_URL));
        }

        if (myPlaceholderImage == null) {
            myPlaceholderImage = StringUtils.trimTo(config().getString(Config.PLACEHOLDER_IMAGE), Constants.EMPTY);
        }

        getJsonConsumer().handler(message -> {
            try {
                final JsonObject body = message.body();
                final String action = message.headers().get(Constants.ACTION);
                final Path filePath = Paths.get(body.getString(Constants.CSV_FILE_PATH));
                final String iiifVersion = body.getString(Constants.IIIF_API_VERSION);
                final String avUrlString = config().getString(Config.AV_URL_STRING, Constants.DEFAULT_AV_STRING);
                final CsvParser csvParser = new CsvParser(avUrlString).parse(filePath, iiifVersion);
                final CsvMetadata csvMetadata = csvParser.getCsvMetadata();

                if (Op.POST_CSV.equals(action)) {
                    final Optional<String> optImageHost = Optional.ofNullable(body.getString(Constants.IIIF_HOST));
                    final Optional<String[]> csvCollection = csvParser.getCsvCollection();
                    final String imageHost = optImageHost.orElse(myImageHost);
                    final CsvHeaders csvHeaders = csvParser.getCsvHeaders();

                    // If we have a collection record in the CSV we're processing, create a collection manifest
                    if (csvCollection.isPresent()) {
                        final Promise<Void> promise = Promise.promise();

                        // On completion of creating the collection doc, check to see if works need to be added
                        promise.future().onComplete(creation -> {
                            if (creation.succeeded()) {
                                createWorks(csvHeaders, csvMetadata, imageHost, iiifVersion, message);
                            } else {
                                error(message, creation.cause(), MessageCodes.MFS_125, creation.cause().getMessage());
                            }
                        });

                        createCollection(promise, csvCollection.get(), csvHeaders, csvMetadata, iiifVersion);
                    } else if (csvMetadata.hasWorks()) {
                        LOGGER.debug(MessageCodes.MFS_043, filePath);
                        updateWorks(csvHeaders, csvMetadata, imageHost, iiifVersion, message);
                    } else if (csvMetadata.hasPages()) {
                        @SuppressWarnings("rawtypes")
                        final List<Future> futures = new ArrayList<>();
                        final Iterator<Entry<String, List<String[]>>> iterator = csvMetadata.getPageIterator();

                        LOGGER.debug(MessageCodes.MFS_069, filePath);

                        while (iterator.hasNext()) {
                            final Entry<String, List<String[]>> pageEntry = iterator.next();
                            final List<String[]> pagesList = pageEntry.getValue();
                            final Promise<Void> promise = Promise.promise();
                            final String workID = pageEntry.getKey();

                            futures.add(promise.future());
                            updatePages(promise, workID, csvHeaders, pagesList, imageHost, iiifVersion);
                        }

                        CompositeFuture.all(futures).onComplete(handler -> {
                            if (handler.succeeded()) {
                                message.reply(Op.SUCCESS);
                            } else {
                                error(message, handler.cause(), MessageCodes.MFS_149, handler.cause().getMessage());
                            }
                        });
                    } else {
                        final Exception details = new CsvParsingException(MessageCodes.MFS_042);

                        LOGGER.error(details, details.getMessage());
                        message.fail(HTTP.BAD_REQUEST, details.getMessage());
                    }
                } else if (Op.POST_UPDATE_CSV.equals(action)) {
                    @SuppressWarnings("rawtypes")
                    final List<Future> futures = new ArrayList<>();

                    csvMetadata.getWorksList().forEach(work -> {
                        final Promise<Void> promise = Promise.promise();

                        futures.add(promise.future());
                        updateWork(promise, csvParser.getCsvHeaders(), work, iiifVersion);
                    });

                    CompositeFuture.all(futures).onComplete(handler -> {
                        if (handler.succeeded()) {
                            message.reply(Op.SUCCESS);
                        } else {
                            error(message, handler.cause(), MessageCodes.MFS_158, handler.cause().getMessage());
                        }
                    });
                } else {
                    final String jsonMsg = message.toString();
                    final String verticleName = getClass().getSimpleName();
                    final String errorMessage = StringUtils.format(MessageCodes.MFS_139, verticleName, jsonMsg, action);

                    message.fail(HTTP.INTERNAL_SERVER_ERROR, errorMessage);
                }
            } catch (final IOException details) {
                LOGGER.error(details, details.getMessage());
                message.fail(HTTP.INTERNAL_SERVER_ERROR, details.getMessage());
            } catch (final CsvParsingException | CsvException details) {
                LOGGER.error(details, details.getMessage());
                message.fail(HTTP.BAD_REQUEST, details.getMessage());
            }
        });

        aPromise.complete();
    }

    /**
     * Updates the work metadata on an already existing work manifest.
     *
     * @param aPromise A promise that the work will be updated
     * @param aCsvHeaders The CSV headers for the work metadata
     * @param aWork The work's metadata
     * @param aApiVersion An IIIF API version to which the metadata conforms
     */
    private void updateWork(final Promise<Void> aPromise, final CsvHeaders aCsvHeaders, final String[] aWork,
            final String aApiVersion) {
        final Promise<LockedIiifResource> promise = Promise.promise();
        final String id = aWork[aCsvHeaders.getItemArkIndex()];

        LOGGER.debug(MessageCodes.MFS_159, id);

        promise.future().onComplete(handler -> {
            if (handler.succeeded()) {
                final LockedIiifResource lockedManifest = handler.result();
                final DeliveryOptions options = new DeliveryOptions();
                final ObjectMapper mapper = new ObjectMapper();
                final JsonObject message = new JsonObject();

                try {
                    options.addHeader(Constants.ACTION, ManifestVerticle.UPDATE_WORK);
                    message.put(Constants.UPDATED_CONTENT, new JsonArray(mapper.writeValueAsString(aWork)));
                    message.put(Constants.CSV_HEADERS, aCsvHeaders.toJSON());
                    message.put(Constants.MANIFEST_CONTENT, lockedManifest.toJSON());
                    message.put(Constants.MANIFEST_ID, id);

                    sendMessage(getManifestVerticleName(aApiVersion), message, options, workUpdate -> {
                        if (workUpdate.succeeded()) {
                            aPromise.complete();
                        } else {
                            aPromise.fail(workUpdate.cause());
                        }

                        lockedManifest.release();
                    });
                } catch (final JsonProcessingException details) {
                    lockedManifest.release();
                    aPromise.fail(details.getCause());
                }
            } else {
                aPromise.fail(handler.cause());
            }
        });

        getLockedIiifResource(id, false, promise);
    }

    /**
     * Creates a collection manifest.
     *
     * @param aPromise A promise of future work to be done
     * @param aCsvCollection A collection ID and label
     * @param aCsvHeaders Headers from a CSV file
     * @param aCsvMetadata Metadata from a CSV file
     * @param aApiVersion The version of the IIIF Presentation API being requested
     */
    private void createCollection(final Promise<Void> aPromise, final String[] aCsvCollection,
            final CsvHeaders aCsvHeaders, final CsvMetadata aCsvMetadata, final String aApiVersion)
            throws CsvParsingException {
        final String collectionID = aCsvCollection[aCsvHeaders.getItemArkIndex()];
        final List<String[]> csvWorks = aCsvMetadata.getWorksMap().get(collectionID);
        final DeliveryOptions options = new DeliveryOptions();
        final ObjectMapper mapper = new ObjectMapper();
        final JsonObject message = new JsonObject();

        try {
            options.addHeader(Constants.ACTION, ManifestVerticle.CREATE_COLLECTION);
            message.put(Constants.COLLECTION_CONTENT, new JsonArray(mapper.writeValueAsString(aCsvCollection)));
            message.put(Constants.COLLECTION_NAME, collectionID);
            message.put(Constants.CSV_HEADERS, aCsvHeaders.toJSON());

            if (csvWorks != null) {
                message.put(Constants.MANIFEST_CONTENT, mapper.writeValueAsString(csvWorks));
            } else {
                LOGGER.debug(MessageCodes.MFS_118, collectionID);
            }

            LOGGER.debug(MessageCodes.MFS_122, collectionID);

            sendMessage(getManifestVerticleName(aApiVersion), message, options, collectionCreation -> {
                if (collectionCreation.succeeded()) {
                    aPromise.complete();
                } else {
                    aPromise.fail(collectionCreation.cause());
                }
            });
        } catch (final JsonProcessingException details) {
            aPromise.fail(details);
        }
    }

    /**
     * Updates work manifest pages with data from an uploaded CSV.
     *
     * @param aPromise A promise
     * @param aWorkID A work ID
     * @param aCsvHeaders Headers from the supplied CSV file
     * @param aPagesList A list of pages
     * @param aImageHost An image host
     * @param aApiVersion The version of the IIIF Presentation API being requested
     */
    private void updatePages(final Promise<Void> aPromise, final String aWorkID, final CsvHeaders aCsvHeaders,
            final List<String[]> aPagesList, final String aImageHost, final String aApiVersion) {
        final Promise<LockedIiifResource> promise = Promise.promise();

        promise.future().onComplete(handler -> {
            if (handler.succeeded()) {
                final LockedIiifResource lockedManifest = handler.result();
                final DeliveryOptions options = new DeliveryOptions();
                final ObjectMapper mapper = new ObjectMapper();
                final JsonObject message = new JsonObject();

                options.addHeader(Constants.ACTION, ManifestVerticle.UPDATE_PAGES);
                message.put(Constants.MANIFEST_ID, aWorkID);
                message.put(Constants.PLACEHOLDER_IMAGE, myPlaceholderImage);
                message.put(Constants.MANIFEST_CONTENT, lockedManifest.toJSON());
                message.put(Constants.CSV_HEADERS, aCsvHeaders.toJSON());
                message.put(Constants.IIIF_HOST, aImageHost);

                try {
                    message.put(Constants.MANIFEST_PAGES, new JsonArray(mapper.writeValueAsString(aPagesList)));

                    // Override default timeout because we look up image dimensions as a part of this process
                    sendMessage(getManifestVerticleName(aApiVersion), message, options, TIMEOUT, update -> {
                        if (update.succeeded()) {
                            aPromise.complete();
                        } else {
                            aPromise.fail(update.cause());
                        }

                        lockedManifest.release();
                    });
                } catch (final JsonProcessingException details) {
                    lockedManifest.release();
                    aPromise.fail(details);
                }
            } else {
                aPromise.fail(handler.cause());
            }
        });

        getLockedIiifResource(aWorkID, false, promise);
    }

    /**
     * Updates the works associated with a collection.
     *
     * @param aCsvHeaders Headers from the supplied CSV file
     * @param aCsvMetadata Metadata from the supplied CSV file
     * @param aImageHost An image host
     * @param aApiVersion The version of the IIIF Presentation API being requested
     * @param aMessage A message
     */
    private void updateWorks(final CsvHeaders aCsvHeaders, final CsvMetadata aCsvMetadata, final String aImageHost,
            final String aApiVersion, final Message<JsonObject> aMessage) {
        final Promise<LockedIiifResource> promise = Promise.promise();
        final String collectionID = aCsvMetadata.getFirstCollectionID(aCsvHeaders.getParentArkIndex()).get();

        // If we were able to get a lock on the collection, update it with our new works
        promise.future().onComplete(handler -> {
            if (handler.succeeded()) {
                final Map<String, List<String[]>> worksMap = aCsvMetadata.getWorksMap();
                final LockedIiifResource lockedCollection = handler.result();
                final DeliveryOptions options = new DeliveryOptions();
                final ObjectMapper mapper = new ObjectMapper();
                final JsonObject message = new JsonObject();

                try {
                    options.addHeader(Constants.ACTION, ManifestVerticle.UPDATE_COLLECTION);
                    message.put(Constants.COLLECTION_CONTENT, lockedCollection.toJSON());
                    message.put(Constants.COLLECTION_NAME, collectionID);
                    message.put(Constants.MANIFEST_CONTENT, new JsonObject(mapper.writeValueAsString(worksMap)));

                    sendMessage(getManifestVerticleName(aApiVersion), message, options, update -> {
                        lockedCollection.release();

                        if (update.succeeded()) {
                            createWorks(aCsvHeaders, aCsvMetadata, aImageHost, aApiVersion, aMessage);
                        } else {
                            error(aMessage, update.cause(), MessageCodes.MFS_150, update.cause().getMessage());
                        }
                    });
                } catch (final JsonProcessingException details) {
                    aMessage.fail(HTTP.INTERNAL_SERVER_ERROR, details.getMessage());
                }
            } else {
                aMessage.fail(HTTP.INTERNAL_SERVER_ERROR, handler.cause().getMessage());
            }
        });

        getLockedIiifResource(collectionID, true, promise);
    }

    /**
     * Creates manifest records for the supplied works.
     *
     * @param aCsvHeaders Headers from the CSV file
     * @param aCsvMetadata Metadata from the supplied CSV file
     * @param aImageHost The URL of the IIIF image server
     * @param aApiVersion The version of the IIIF Presentation API being requested
     * @param aMessage The event queue message
     */
    private void createWorks(final CsvHeaders aCsvHeaders, final CsvMetadata aCsvMetadata, final String aImageHost,
            final String aApiVersion, final Message<JsonObject> aMessage) {
        final Map<String, List<String[]>> aPagesMap = aCsvMetadata.getPagesMap();
        final List<String[]> aWorksDataList = aCsvMetadata.getWorksList();
        final DeliveryOptions options = new DeliveryOptions();
        final ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("rawtypes")
        final List<Future> futures = new ArrayList<>();

        options.addHeader(Constants.ACTION, ManifestVerticle.CREATE_WORK);

        // Cycle through the works creating a manifest for each
        aWorksDataList.forEach(worksData -> {
            final Promise<Void> promise = Promise.promise();
            final JsonObject message = new JsonObject();

            futures.add(promise.future());

            try {
                message.put(Constants.CSV_HEADERS, aCsvHeaders.toJSON());
                message.put(Constants.MANIFEST_PAGES, new JsonObject(mapper.writeValueAsString(aPagesMap)));
                message.put(Constants.MANIFEST_CONTENT, new JsonArray(mapper.writeValueAsString(worksData)));
                message.put(Constants.IIIF_HOST, aImageHost);

                // This is the call that looks up all the image dimensions; we need to bump default timeout
                sendMessage(getManifestVerticleName(aApiVersion), message, options, TIMEOUT, workCreation -> {
                    if (workCreation.succeeded()) {
                        promise.complete();
                    } else {
                        promise.fail(workCreation.cause());
                    }
                });
            } catch (final JsonProcessingException details) {
                promise.fail(details);
            }
        });

        // Keep track of our progress and fail our promise if we don't succeed
        CompositeFuture.all(futures).onComplete(handler -> {
            if (handler.succeeded()) {
                aMessage.reply(LOGGER.getMessage(MessageCodes.MFS_126));
            } else {
                error(aMessage, handler.cause(), MessageCodes.MFS_131, handler.cause().getMessage());
            }
        });
    }

    /**
     * Tries to lock an S3 manifest or collection so we can update it.
     *
     * @param aID A manifest or collection ID
     * @param aCollDoc Whether the resource is a collection or a manifest ("work")
     * @param aPromise A promise that we'll get a lock
     */
    private void getLockedIiifResource(final String aID, final boolean aCollDoc,
            final Promise<LockedIiifResource> aPromise) {
        final SharedData sharedData = vertx.sharedData();

        // Try to get the lock for a second
        sharedData.getLocalLockWithTimeout(aID, 1000, lockRequest -> {
            if (lockRequest.succeeded()) {
                try {
                    final JsonObject message = new JsonObject();
                    final DeliveryOptions options =
                            new DeliveryOptions().addHeader(Constants.NO_REWRITE_URLS, Boolean.TRUE.toString());

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

                            aPromise.complete(new LockedIiifResource(manifest, aCollDoc, lock));
                        } else {
                            final String type = aCollDoc ? Constants.COLLECTION : Constants.MANIFEST;
                            final Throwable cause = handler.cause();

                            lockRequest.result().release();

                            aPromise.fail(new ManifestNotFoundException(cause, MessageCodes.MFS_146, type, aID));
                        }
                    });
                } catch (final NullPointerException | IndexOutOfBoundsException details) {
                    lockRequest.result().release();
                    aPromise.fail(details);
                }
            } else {
                // If we can't get a lock, keep trying (forever, really?)
                vertx.setTimer(1000, timer -> {
                    getLockedIiifResource(aID, aCollDoc, aPromise);
                });
            }
        });
    }

    /**
     * Gets a manifest verticle for the supplied version, falling back to v2 if the supplied version isn't recognized.
     *
     * @param aApiVersion A version of the IIIF presentation specification
     * @return The name of the manifest verticle that would handle requests for the supplied API version
     */
    private String getManifestVerticleName(final String aApiVersion) {
        if (Constants.IIIF_API_V3.equals(aApiVersion)) {
            return V3ManifestVerticle.class.getName();
        } else {
            return V2ManifestVerticle.class.getName();
        }
    }
}
