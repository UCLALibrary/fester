
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

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import info.freelibrary.iiif.presentation.Canvas;
import info.freelibrary.iiif.presentation.Collection;
import info.freelibrary.iiif.presentation.ImageContent;
import info.freelibrary.iiif.presentation.ImageResource;
import info.freelibrary.iiif.presentation.Manifest;
import info.freelibrary.iiif.presentation.Sequence;
import info.freelibrary.iiif.presentation.services.ImageInfoService;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.CsvHeaders;
import edu.ucla.library.iiif.fester.CsvParsingException;
import edu.ucla.library.iiif.fester.ImageInfo;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.CodeUtils;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * A creator of manifests (collection and work).
 */
public class ManifestVerticle extends AbstractFesterVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestVerticle.class, Constants.MESSAGES);

    private static final String SEQUENCE_URI = "{}/{}/manifest/sequence/normal";

    private static final String CANVAS_URI = "{}/{}/manifest/canvas/{}";

    private static final String ANNOTATION_URI = "{}/{}/annotation/{}";

    private static final String IMAGE_URI = "{}/{}";

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

            if (myHost == null) {
                myHost = messageBody.getString(Constants.FESTER_HOST);
            }

            try (Reader reader = Files.newBufferedReader(filePath); CSVReader csvReader = new CSVReader(reader)) {
                final Map<String, List<String[]>> pages = new HashMap<>();
                final Map<String, List<Collection.Manifest>> works = new HashMap<>();
                final List<String[]> worksData = new ArrayList<>();

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
                            addWork(row, csvHeaders, works, worksData);
                        } else if (PAGE.equals(row[objectTypeIndex])) {
                            addPage(row, csvHeaders, pages);
                        }
                    }
                }

                // If we have a collection record in the CSV we're processing, create a collection manifest
                if (collection != null) {
                    LOGGER.debug(MessageCodes.MFS_122, filePath, collection);
                    buildCollectionManifest(collection, works, worksData, pages, csvHeaders, message);
                } else { // We don't have a collection manifest, just works and/or pages
                    // TODO
                    message.reply("No collection record");
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
     * Builds the collection manifest.
     *
     * @param aCollection A collection
     * @param aWorksMap A map of works
     * @param aWorksDataList Metadata about the works in our map
     * @param aPageMap A map of pages
     * @param aHeaders CSV header information
     * @param aMessage The verticle response message
     */
    private void buildCollectionManifest(final Collection aCollection,
            final Map<String, List<Collection.Manifest>> aWorksMap, final List<String[]> aWorksDataList,
            final Map<String, List<String[]>> aPageMap, final CsvHeaders aHeaders,
            final Message<JsonObject> aMessage) {
        final List<Collection.Manifest> manifestList = aCollection.getManifests(); // Empty list
        final String collectionID = IDUtils.decode(aCollection.getID(), Constants.COLLECTIONS_PATH);
        final List<Collection.Manifest> manifests = aWorksMap.get(collectionID);
        final Promise<Void> promise = Promise.promise();
        final CsvHeaders finalizedCsvHeaders = aHeaders;

        // If we have work manifests, add them to the collection manifest
        if (manifests != null) {
            manifestList.addAll(manifests);
        } else {
            LOGGER.warn(MessageCodes.MFS_118, collectionID);
        }

        // Create a handler to handle the result of that
        promise.future().setHandler(handler -> {
            if (handler.succeeded()) {
                final Promise<Void> workManifestsPromise = Promise.promise();

                // Create a handler for building the work manifests
                workManifestsPromise.future().setHandler(workManifestsHandler -> {
                    if (workManifestsHandler.succeeded()) {
                        // On success, let the class that called us know we've succeeded
                        aMessage.reply(LOGGER.getMessage(MessageCodes.MFS_126, collectionID));
                    } else {
                        final int failCode = CodeUtils.getInt(MessageCodes.MFS_131);
                        final Throwable cause = workManifestsHandler.cause();
                        final String causeMessage = cause.getMessage();
                        final String message = LOGGER.getMessage(MessageCodes.MFS_131, collectionID, causeMessage);

                        LOGGER.error(cause, message);
                        aMessage.fail(failCode, message);
                    }
                });

                // Build the related work manifests, passing a promise to complete when done
                queueWorkManifests(finalizedCsvHeaders, aWorksDataList, aPageMap, workManifestsPromise);
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

    /**
     * Queue up manifest creation for all the works.
     *
     * @param aHeaders A CSV headers object
     * @param aWorksList A list of work metadata
     * @param aPagesMap A list of page metadata
     * @param aPromise The promise we'll get the work manifests created
     */
    private void queueWorkManifests(final CsvHeaders aHeaders, final List<String[]> aWorksList,
            final Map<String, List<String[]>> aPagesMap, final Promise aPromise) {
        final Iterator<String[]> iterator = aWorksList.iterator();
        final List<Future> futures = new ArrayList<>();

        // Request each work manifest be created
        while (iterator.hasNext()) {
            futures.add(buildWorkManifest(aHeaders, iterator.next(), aPagesMap, Promise.promise()));
        }

        // Keep track of our progress and fail our promise if we don't succeed
        CompositeFuture.all(futures).setHandler(handler -> {
            if (handler.succeeded()) {
                aPromise.complete();
            } else {
                aPromise.fail(handler.cause());
            }
        });
    }

    /**
     * Build an individual work manifest.
     *
     * @param aHeaders The CSV headers
     * @param aWork A metadata array representing the work
     * @param aPages A list of pages
     * @param aPromise A promise we'll create the work manifest
     * @return The future result of our promise
     */
    private Future buildWorkManifest(final CsvHeaders aHeaders, final String[] aWork,
            final Map<String, List<String[]>> aPages, final Promise<Void> aPromise) {
        final String workID = aWork[aHeaders.getItemArkIndex()];
        final String urlEncodedWorkID = URLEncoder.encode(workID, StandardCharsets.UTF_8);
        final String workLabel = aWork[aHeaders.getTitleIndex()];
        final String manifestID = StringUtils.format(MANIFEST_URI, myHost, urlEncodedWorkID);
        final Manifest manifest = new Manifest(manifestID, workLabel);
        final String sequenceID = StringUtils.format(SEQUENCE_URI, myHost, urlEncodedWorkID);
        final Sequence sequence = new Sequence().setID(sequenceID);

        try {
            if (aPages.containsKey(workID)) {
                final List<String[]> pageList = aPages.get(workID);
                final Iterator<String[]> iterator;

                manifest.addSequence(sequence);
                pageList.sort(new ItemSequenceComparator(aHeaders.getItemSequence()));
                iterator = pageList.iterator();

                while (iterator.hasNext()) {
                    final String[] columns = iterator.next();
                    final String pageID = columns[aHeaders.getItemArkIndex()];
                    final String idPart = IDUtils.getLastPart(pageID); // We're just copying Samvera here
                    final String encodedPageID = URLEncoder.encode(pageID, StandardCharsets.UTF_8);
                    final String pageLabel = columns[aHeaders.getTitleIndex()];
                    final String canvasID = StringUtils.format(CANVAS_URI, myHost, urlEncodedWorkID, idPart);
                    final String pageURI = StringUtils.format(IMAGE_URI, myImageHost, encodedPageID);
                    final ImageInfo info = new ImageInfo(pageURI); // May be room for improvement here
                    final Canvas canvas = new Canvas(canvasID, pageLabel, info.getWidth(), info.getHeight());
                    final String annotationURI = StringUtils.format(ANNOTATION_URI, myHost, urlEncodedWorkID, idPart);
                    final ImageContent imageContent = new ImageContent(annotationURI, canvas);
                    final String resourceURI = pageURI + DEFAULT_IMAGE_URI; // Copying Samvera's default image link
                    final ImageResource imageResource = new ImageResource(resourceURI, new ImageInfoService(pageURI));

                    imageContent.addResource(imageResource);
                    canvas.addImageContent(imageContent);
                    sequence.addCanvas(canvas);
                }
            }

            sendMessage(manifest.toJSON(), S3BucketVerticle.class.getName(), send -> {
                if (send.succeeded()) {
                    aPromise.complete();
                } else {
                    aPromise.fail(send.cause());
                }
            });
        } catch (final IOException details) {
            aPromise.fail(details);
        }

        // Return our promise's future result
        return aPromise.future();
    }

    /**
     * Add a Work manifest.
     *
     * @param aRow A CSV row representing a Work
     * @param aHeaders The CSV headers
     * @param aWorksMap A collection of Work manifests
     * @throws CsvParsingException If there is trouble getting the necessary info from the CSV
     */
    private void addWork(final String[] aRow, final CsvHeaders aHeaders,
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
    private void addPage(final String[] aRow, final CsvHeaders aHeaders, final Map<String, List<String[]>> aPageMap)
            throws CsvParsingException {
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
            final String label = StringUtils.trimToNull(aRow[aHeaders.getProjectNameIndex()]);

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
