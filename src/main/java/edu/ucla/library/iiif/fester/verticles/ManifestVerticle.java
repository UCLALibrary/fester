
package edu.ucla.library.iiif.fester.verticles;

import static edu.ucla.library.iiif.fester.ObjectType.COLLECTION;
import static edu.ucla.library.iiif.fester.ObjectType.PAGE;
import static edu.ucla.library.iiif.fester.ObjectType.WORK;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;

import info.freelibrary.iiif.presentation.Collection;
import info.freelibrary.iiif.presentation.Collection.Manifest;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.CSV;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.CsvHeaders;
import edu.ucla.library.iiif.fester.CsvParsingException;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.CodeUtils;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class ManifestVerticle extends AbstractFesterVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestVerticle.class, Constants.MESSAGES);

    /**
     * Starts the collection manifester.
     */
    @Override
    public void start(final Future<Void> aFuture) {
        getJsonConsumer().handler(message -> {
            final JsonObject messageBody = message.body();
            final Path filePath = Paths.get(messageBody.getString(Constants.CSV_FILE_PATH));
            final String host = messageBody.getString(Constants.FESTER_HOST);
            final String path = messageBody.getString(Constants.COLLECTIONS_PATH);

            try (Reader reader = Files.newBufferedReader(filePath); CSVReader csvReader = new CSVReader(reader)) {
                final Map<String, List<Manifest>> works = new HashMap<>();
                final Map<String, String[]> pages = new HashMap<>();
                final CsvHeaders csvHeaders = new CsvHeaders();

                boolean headersIndexed = false;
                Collection collection = null;

                for (final String[] row : csvReader.readAll()) {
                    if (!headersIndexed) {
                        // We will throw a CsvParsingException here if one of our headers isn't found in the CSV
                        headersIndexed = indexHeaders(row, csvHeaders);
                    } else {
                        final int objectTypeIndex = csvHeaders.getObjectTypeIndex();

                        if (COLLECTION.equals(row[objectTypeIndex])) {
                            collection = getCollection(row, csvHeaders, host, path);
                        } else if (WORK.equals(row[objectTypeIndex])) {
                            addWork(row, csvHeaders, works, host);
                        } else if (PAGE.equals(row[objectTypeIndex])) {
                            // TODO in IIIF-505
                        }
                    }
                }

                if (collection != null) {
                    final List<Manifest> manifestList = collection.getManifests();
                    final String collectionID = IDUtils.decode(collection.getID(), path);
                    final List<Manifest> manifests = works.get(collectionID);

                    // If we have work manifests, add them to the collection manifest
                    if (manifests != null) {
                        manifestList.addAll(manifests);
                    } else {
                        LOGGER.warn(MessageCodes.MFS_118, collectionID);
                    }

                    // We're going to send to the S3 verticle
                    LOGGER.debug(MessageCodes.MFS_000, collection.toJSON());
                    message.reply("Manifest sent to S3 verticle");
                } else {
                    // TODO in IIIF-505
                    message.reply("No collection record");
                }
            } catch (final IOException details) {
                LOGGER.error(details, details.getMessage());
                message.fail(CodeUtils.getInt(MessageCodes.MFS_000), details.getMessage());
            } catch (final CsvParsingException details) {
                LOGGER.error(details, details.getMessage());
                message.fail(CodeUtils.getInt(MessageCodes.MFS_000), details.getMessage());
            }
        });

        aFuture.complete();
    }

    /**
     * Add a Work manifest.
     *
     * @param aRow A CSV row representing a Work
     * @param aHeaders The CSV headers
     * @param aWorksMap A collection of Work manifests
     * @throws CsvParsingException If there is trouble getting the necessary info from the CSV
     */
    private void addWork(final String[] aRow, final CsvHeaders aHeaders, final Map<String, List<Manifest>> aWorksMap,
            final String aHost) throws CsvParsingException {
        final String id = StringUtils.trimToNull(aRow[aHeaders.getItemArkIndex()]);
        final String parentID = StringUtils.trimToNull(aRow[aHeaders.getParentArkIndex()]);
        final String label = StringUtils.trimToNull(aRow[aHeaders.getTitleIndex()]);

        if (id != null && label != null) {
            final Manifest manifest = new Manifest(IDUtils.encode(aHost, id), label);

            LOGGER.debug(MessageCodes.MFS_119, id, parentID);

            if (parentID != null) {
                if (aWorksMap.containsKey(parentID)) {
                    aWorksMap.get(parentID).add(manifest);
                } else {
                    final List<Manifest> manifests = new ArrayList<>();

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
    private Collection getCollection(final String[] aRow, final CsvHeaders aHeaders, final String aHost,
            final String aPath) throws CsvParsingException {
        final String id = StringUtils.trimToNull(aRow[aHeaders.getItemArkIndex()]);

        if (id != null) {
            final String label = StringUtils.trimToNull(aRow[aHeaders.getProjectNameIndex()]);

            if (label != null) {
                return new Collection(IDUtils.encode(aHost, aPath, id), label);
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
                default:
                    // Our default is to ignore things we don't care about
            }
        }

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
        }

        return true;
    }
}
