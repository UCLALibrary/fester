
package edu.ucla.library.iiif.fester;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.opencsv.CSVIterator;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import info.freelibrary.iiif.presentation.Collection;
import info.freelibrary.iiif.presentation.properties.Attribution;
import info.freelibrary.iiif.presentation.properties.Metadata;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.utils.IDUtils;

/**
 * A parser for ingested CSV data.
 */
public class CsvParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvParser.class, Constants.MESSAGES);

    private static final Pattern EOL_PATTERN = Pattern.compile(".*\\R");

    private final Map<String, List<Collection.Manifest>> myWorksMap = new HashMap<>();

    private final Map<String, List<String[]>> myPagesMap = new LinkedHashMap<>();

    private final List<String[]> myWorksList = new ArrayList<>();

    private Collection myCollection;

    private CsvHeaders myCsvHeaders;

    /**
     * Creates a new CsvParser.
     */
    public CsvParser() {
    }

    /**
     * Parses the CSV file at the supplied path. This is not thread-safe.
     *
     * Optional CSV columns:
     *
     *   IIIF Access URL, Item Sequence (if the CSV contains no page rows), viewingHint, viewingDirection,
     *   Name.repository, Rights.statementLocal, Rights.servicesContact,
     *
     * @param aPath A path to a CSV file
     * @throws IOException If there is trouble reading or writing data
     * @throws CsvException
     * @throws CsvParsingException
     */
    public CsvParser parse(final Path aPath) throws IOException, CsvException, CsvParsingException {
        reset();

        try (CSVReader csvReader = new CSVReader(Files.newBufferedReader(aPath))) {
            int rowsRead = 0;
            int works = 0;
            int pages = 0;

            final CSVIterator csvIterator = new CSVIterator(csvReader);
            final List<String[]> rows = new LinkedList<>();

            while (csvIterator.hasNext()) {
                final String[] nextRow = csvIterator.next();

                // Skip blank rows
                if (!(nextRow.length == 1 && "".equals(nextRow[0].trim()))) {
                    rows.add(nextRow);
                }
            }

            // Remove the header row from the list of rows
            myCsvHeaders = new CsvHeaders(checkForEOLs(rows.remove(0)));

            // Required CSV columns
            if (!myCsvHeaders.hasItemArkIndex()) {
                throw new CsvParsingException(MessageCodes.MFS_113);
            } else if (!myCsvHeaders.hasParentArkIndex()) {
                throw new CsvParsingException(MessageCodes.MFS_114);
            } else if (!myCsvHeaders.hasTitleIndex()) {
                throw new CsvParsingException(MessageCodes.MFS_111);
            } else if (!myCsvHeaders.hasFileNameIndex()) {
                throw new CsvParsingException(MessageCodes.MFS_112);
            } else if (!myCsvHeaders.hasItemSequenceIndex()) {
                throw new CsvParsingException(MessageCodes.MFS_123);
            }

            for (final String[] row : rows) {
                final ObjectType objectType;

                checkForEOLs(row);
                objectType = getObjectType(row);

                switch (objectType) {
                    case COLLECTION: {
                        myCollection = getCollection(row);
                        break;
                    }
                    case WORK: {
                        extractWorkMetadata(row);
                        works += 1;
                        break;
                    }
                    case PAGE: {
                        extractPageMetadata(row);
                        pages += 1;
                        break;
                    }
                    default: {
                        // MISSING, so skip
                        break;
                    }
                }
                rowsRead += 1;
            }

            LOGGER.debug(MessageCodes.MFS_095, rowsRead, works, pages, myPagesMap.size());
        } catch (final RuntimeException details) {
            throw new CsvParsingException(details);
        }

        return this;
    }

    /**
     * Gets the collection parsed from the CSV data. Not all CSV data will have a collection.
     *
     * @return An optional collection
     */
    public Optional<Collection> getCollection() {
        return Optional.ofNullable(myCollection);
    }

    /**
     * Gets the headers from the CSV data.
     *
     * @return CSV headers
     */
    public CsvHeaders getCsvHeaders() {
        return myCsvHeaders;
    }

    /**
     * Gets the CSV file's metadata.
     *
     * @return The CSV metadata
     */
    public CsvMetadata getCsvMetadata() {
        return new CsvMetadata(myWorksMap, myWorksList, myPagesMap);
    }

    /**
     * Reset the CSV parser.
     */
    private CsvParser reset() {
        myWorksList.clear();
        myPagesMap.clear();
        myWorksMap.clear();

        myCsvHeaders = null;
        myCollection = null;

        return this;
    }

    /**
     * Add a Work manifest.
     *
     * @param aRow A CSV row representing a Work
     * @param aHeaders The CSV headers
     * @param aWorksMap A collection of Work manifests
     * @throws CsvParsingException If there is trouble getting the necessary info from the CSV
     */
    private void extractWorkMetadata(final String[] aRow) throws CsvParsingException {
        final Optional<String> parentIdOpt = getMetadata(aRow[myCsvHeaders.getParentArkIndex()]);
        final Optional<String> workIdOpt = getMetadata(aRow[myCsvHeaders.getItemArkIndex()]);
        final Optional<String> labelOpt = getMetadata(aRow[myCsvHeaders.getTitleIndex()]);

        // Store the work data for full manifest creation
        myWorksList.add(aRow);

        // Create a brief work manifest for inclusion in the collection manifest
        if (workIdOpt.isPresent() && labelOpt.isPresent()) {
            final String workID = workIdOpt.get();
            final URI uri = IDUtils.getResourceURI(Constants.URL_PLACEHOLDER, IDUtils.getWorkS3Key(workID));
            final Collection.Manifest manifest = new Collection.Manifest(uri.toString(), labelOpt.get());

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(MessageCodes.MFS_119, workID, parentIdOpt.orElse(Constants.EMPTY));
            }

            if (parentIdOpt.isPresent()) {
                final String parentID = parentIdOpt.get();

                if (myWorksMap.containsKey(parentID)) {
                    myWorksMap.get(parentID).add(manifest);
                } else {
                    final List<Collection.Manifest> manifests = new ArrayList<>();

                    manifests.add(manifest);
                    myWorksMap.put(parentID, manifests);
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
    private void extractPageMetadata(final String[] aRow) throws CsvParsingException {
        final Optional<String> workIdOpt = getMetadata(aRow[myCsvHeaders.getParentArkIndex()]);

        if (workIdOpt.isPresent()) {
            final String workID = workIdOpt.get();
            final List<String[]> page;

            if (myPagesMap.containsKey(workID)) {
                myPagesMap.get(workID).add(aRow);
            } else {
                page = new ArrayList<>();
                page.add(aRow);
                myPagesMap.put(workID, page);
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
    private Collection getCollection(final String[] aRow) throws CsvParsingException {
        final Optional<String> id = getMetadata(aRow[myCsvHeaders.getItemArkIndex()]);
        final Collection collection;

        if (id.isPresent()) {
            final URI uri = IDUtils.getResourceURI(Constants.URL_PLACEHOLDER, IDUtils.getCollectionS3Key(id.get()));
            final Optional<String> label = getMetadata(aRow[myCsvHeaders.getTitleIndex()]);
            final Metadata metadata = new Metadata();

            if (label.isEmpty()) {
                throw new CsvParsingException(MessageCodes.MFS_104);
            }

            collection = new Collection(uri.toString(), label.get());

            // Add optional properties
            if (myCsvHeaders.hasRepositoryNameIndex()) {
                final Optional<String> repoName = getMetadata(aRow[myCsvHeaders.getRepositoryNameIndex()]);

                if (repoName.isPresent()) {
                    metadata.add(Constants.REPOSITORY_NAME_METADATA_LABEL, repoName.get());
                }
            }

            if (myCsvHeaders.hasLocalRightsStatementIndex()) {
                final Optional<String> rights = getMetadata(aRow[myCsvHeaders.getLocalRightsStatementIndex()]);

                if (rights.isPresent()) {
                    collection.setAttribution(new Attribution(rights.get()));
                }
            }

            if (myCsvHeaders.hasRightsContactIndex()) {
                final Optional<String> contract = getMetadata(aRow[myCsvHeaders.getRightsContactIndex()]);

                if (contract.isPresent()) {
                    metadata.add(Constants.RIGHTS_CONTACT_METADATA_LABEL, contract.get());
                }
            }

            if (metadata.getEntries().size() > 0) {
                collection.setMetadata(metadata);
            }

            return collection;
        } else {
            throw new CsvParsingException(MessageCodes.MFS_106);
        }
    }

    private Optional<String> getMetadata(final String aRowColumnValue) {
        return Optional.ofNullable(StringUtils.trimToNull(aRowColumnValue));
    }

    /**
     * Checks for hard returns in metadata values, and returns the row if none are found.
     *
     * @param aRow A row from the metadata CSV
     * @return The row
     * @throws CsvParsingException If the metadata contains a hard return
     */
    private String[] checkForEOLs(final String[] aRow) throws CsvParsingException {
        for (int index = 0; index < aRow.length; index++) {
            if (EOL_PATTERN.matcher(aRow[index]).find()) {
                throw new CsvParsingException(MessageCodes.MFS_093, aRow[index]);
            }
        }
        return aRow;
    }

    /**
     * Gets the type of the object represented by a CSV row.
     *
     * @param aRow A row from the metadata CSV
     * @return The object type
     * @throws CsvParsingException If object type isn't included in the CSV headers, or the object type index is out of
     * bounds of the CSV row, or the metadata contains an unknown object type
     */
    private ObjectType getObjectType(final String[] aRow) throws CsvParsingException {
        if (myCsvHeaders.hasObjectTypeIndex()) {
            final int objectTypeIndex = myCsvHeaders.getObjectTypeIndex();

            if (aRow.length > objectTypeIndex) {
                final String objectType = aRow[objectTypeIndex];

                if (ObjectType.COLLECTION.equals(objectType)) {
                    return ObjectType.COLLECTION;
                } else if (ObjectType.WORK.equals(objectType)) {
                    return ObjectType.WORK;
                } else if (ObjectType.PAGE.equals(objectType)) {
                    return ObjectType.PAGE;
                } else if (ObjectType.MISSING.equals(StringUtils.trimTo(objectType, Constants.EMPTY))) {
                    return ObjectType.MISSING;
                } else {
                    // Disallow unknown types
                    throw new CsvParsingException(MessageCodes.MFS_094, objectType);
                }
            } else {
                throw new CsvParsingException(MessageCodes.MFS_098, objectTypeIndex, Arrays.toString(aRow));
            }
        } else {
            throw new CsvParsingException(MessageCodes.MFS_115);
        }
    }
}
