
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
import info.freelibrary.iiif.presentation.Manifest;
import info.freelibrary.iiif.presentation.Sequence;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.CSV;
import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.CsvHeaders;
import edu.ucla.library.iiif.fester.CsvParsingException;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.CodeUtils;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

/**
 * A creator of manifests (collection and work).
 */
public class ManifestVerticle extends AbstractFesterVerticle {

    // Swap this out with a getName on the class once the class is merged in to the code base
    private static final String UPLOADER = "edu.ucla.library.iiif.fester.verticles.S3BucketVerticle";

    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestVerticle.class, Constants.MESSAGES);

    private static final String SEQUENCE_URI = "{}/{}/manifest/sequence/normal";

    private static final String CANVAS_URI = "{}/{}/manifest/canvas/{}";

    private static final String IMAGE_URI = "{}/{}";

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
                final CsvHeaders csvHeaders = new CsvHeaders();

                boolean headersIndexed = false;
                Collection collection = null;

                // Read through the CSV data and create store info about collections, works, and pages
                for (final String[] row : csvReader.readAll()) {
                    if (!headersIndexed) {
                        // We will throw a CsvParsingException here if one of our headers isn't found in the CSV
                        headersIndexed = indexHeaders(row, csvHeaders);
                    } else {
                        final int objectTypeIndex = csvHeaders.getObjectTypeIndex();

                        if (COLLECTION.equals(row[objectTypeIndex])) {
                            collection = getCollection(row, csvHeaders);
                        } else if (WORK.equals(row[objectTypeIndex])) {
                            addWork(row, csvHeaders, works, worksData);
                        } else if (PAGE.equals(row[objectTypeIndex])) {
                            final String parentID = StringUtils.trimToNull(row[csvHeaders.getParentArkIndex()]);

                            if (parentID != null) {
                                if (pages.containsKey(parentID)) {
                                    pages.get(parentID).add(row);
                                } else {
                                    final List<String[]> page = new ArrayList<>();

                                    page.add(row);
                                    pages.put(parentID, page);
                                }
                            } else {
                                throw new CsvParsingException(MessageCodes.MFS_121);
                            }
                        }
                    }
                }

                // If we have a collection record in the CSV we're processing
                if (collection != null) {
                    final List<Collection.Manifest> manifestList = collection.getManifests();
                    final String collectionID = IDUtils.decode(collection.getID(), Constants.COLLECTIONS_PATH);
                    final List<Collection.Manifest> manifests = works.get(collectionID);
                    final Promise<Void> promise = Promise.promise();

                    // If we have work manifests, add them to the collection manifest
                    if (manifests != null) {
                        manifestList.addAll(manifests);
                    } else {
                        LOGGER.warn(MessageCodes.MFS_118, collectionID);
                    }

                    // We're going to send to the S3 verticle
                    LOGGER.debug(MessageCodes.MFS_122, filePath, collection.toJSON());

                    // Create a handler to handle the result of that
                    promise.future().setHandler(handler -> {
                        if (handler.succeeded()) {
                            final Promise<Void> workManifestsPromise = Promise.promise();

                            workManifestsPromise.future().setHandler(workManifestsHandler -> {
                                if (workManifestsHandler.succeeded()) {
                                    message.reply(LOGGER.getMessage(MessageCodes.MFS_126, collectionID));
                                } else {
                                    final int failCode = CodeUtils.getInt(MessageCodes.MFS_127);
                                    final String failMessage = handler.cause().getMessage();

                                    LOGGER.error(MessageCodes.MFS_127, failMessage);
                                    message.fail(failCode, failMessage);
                                }
                            });

                            buildWorkManifests(csvHeaders, worksData, pages, workManifestsPromise);
                        } else {
                            final int failCode = CodeUtils.getInt(MessageCodes.MFS_125);
                            final String failMessage = handler.cause().getMessage();

                            LOGGER.error(MessageCodes.MFS_125, failMessage);
                            message.fail(failCode, failMessage);
                        }
                    });

                    // Send collection manifest to S3
                    sendMessage(collection.toJSON(), UPLOADER, send -> {
                        if (send.succeeded()) {
                            promise.complete();
                        } else {
                            promise.fail(send.cause());
                        }
                    });
                } else { // We don't have a collection manifest, just works and/or pages

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

    private void buildWorkManifests(final CsvHeaders aHeaders, final List<String[]> aWorksList,
            final Map<String, List<String[]>> aPagesMap, final Promise aPromise) {
        final Iterator<String[]> iterator = aWorksList.iterator();
        final List<Future> futures = new ArrayList<>();

        while (iterator.hasNext()) {
            futures.add(buildWorkManifest(aHeaders, iterator.next(), aPagesMap, Promise.promise()));
        }

        CompositeFuture.all(futures).setHandler(handler -> {
            if (handler.succeeded()) {
                aPromise.complete();
            } else {
                aPromise.fail(handler.cause());
            }
        });
    }

    private Future buildWorkManifest(final CsvHeaders aHeaders, final String[] aWork,
            final Map<String, List<String[]>> aPages, final Promise<Void> aPromise) {
        final String workID = aWork[aHeaders.getItemArkIndex()];
        final String urlEncodedWorkID = URLEncoder.encode(workID, StandardCharsets.UTF_8);
        final String workLabel = aWork[aHeaders.getTitleIndex()];
        final String manifestID = StringUtils.format(MANIFEST_URI, myHost, urlEncodedWorkID);
        final Manifest manifest = new Manifest(manifestID, workLabel);
        final String sequenceID = StringUtils.format(SEQUENCE_URI, myHost, urlEncodedWorkID);
        final Sequence sequence = new Sequence().setID(sequenceID);

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
                final Canvas canvas = new Canvas(canvasID, pageLabel, 640, 480);
                final String pageURI = StringUtils.format(IMAGE_URI, myImageHost, encodedPageID);
                final ImageContent imageContent = new ImageContent(pageURI, canvas);

                canvas.addImageContent(imageContent);
                sequence.addCanvas(canvas);
            }
        }

        sendMessage(manifest.toJSON(), UPLOADER, send -> {
            if (send.succeeded()) {
                aPromise.complete();
            } else {
                aPromise.fail(send.cause());
            }
        });

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

    /**
     * Index the header positions so they can be used to extract data.
     *
     * @param aRow A headers' row
     * @param aHeaders A CSV headers object
     * @return
     */
    private boolean indexHeaders(final String[] aRow, final CsvHeaders aHeaders) throws CsvParsingException {
        for (int index = 0; index < aRow.length; index++) {
            switch (aRow[index]) {
                case CSV.TITLE:
                    aHeaders.setTitleIndex(index);
                    break;
                case CSV.PROJECT_NAME:
                    aHeaders.setProjectNameIndex(index);
                    break;
                case CSV.ITEM_ARK:
                    aHeaders.setItemArkIndex(index);
                    break;
                case CSV.PARENT_ARK:
                    aHeaders.setParentArkIndex(index);
                    break;
                case CSV.OBJECT_TYPE:
                    aHeaders.setObjectTypeIndex(index);
                    break;
                case CSV.FILE_NAME:
                    aHeaders.setFileNameIndex(index);
                    break;
                case CSV.ITEM_SEQ:
                    aHeaders.setItemSequence(index);
                    break;
                default:
                    // Our default is to ignore things we don't care about
            }
        }

        // Check to make sure we have the data components that we need to build a manifest
        if (!aHeaders.hasItemArkIndex()) {
            throw new CsvParsingException(MessageCodes.MFS_113);
        } else if (!aHeaders.hasParentArkIndex()) {
            throw new CsvParsingException(MessageCodes.MFS_114);
        } else if (!aHeaders.hasProjectNameIndex()) {
            throw new CsvParsingException(MessageCodes.MFS_105);
        } else if (!aHeaders.hasObjectTypeIndex()) {
            throw new CsvParsingException(MessageCodes.MFS_115);
        } else if (!aHeaders.hasTitleIndex()) {
            throw new CsvParsingException(MessageCodes.MFS_111);
        } else if (!aHeaders.hasFileNameIndex()) {
            throw new CsvParsingException(MessageCodes.MFS_112);
        } else if (!aHeaders.hasItemSequence()) {
            throw new CsvParsingException(MessageCodes.MFS_123);
        }

        return true;
    }
}
