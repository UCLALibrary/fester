
package edu.ucla.library.iiif.fester;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

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
     * @param aPath A path to a CSV file
     * @throws IOException If there is trouble reading or writing data
     * @throws CsvException
     * @throws CsvParsingException
     */
    public CsvParser parse(final Path aPath) throws IOException, CsvException, CsvParsingException {
        reset();

        try (CSVReader csvReader = new CSVReader(Files.newBufferedReader(aPath))) {
            // The first row should be the CSV headers row
            for (final String[] row : csvReader.readAll()) {
                checkForEOLs(row);

                if (myCsvHeaders == null) {
                    myCsvHeaders = new CsvHeaders(row); // CsvParsingException if a 'required' header is missing
                } else {
                    final int objectTypeIndex = myCsvHeaders.getObjectTypeIndex();

                    // Handle each metadata object type and disallow unknown types
                    if (ObjectType.COLLECTION.equals(row[objectTypeIndex])) {
                        myCollection = getCollection(row);
                    } else if (ObjectType.WORK.equals(row[objectTypeIndex])) {
                        extractWorkMetadata(row);
                    } else if (ObjectType.PAGE.equals(row[objectTypeIndex])) {
                        extractPageMetadata(row);
                    } else if (ObjectType.MISSING.equals(StringUtils.trimTo(row[objectTypeIndex], Constants.EMPTY))) {
                        // skip
                    } else {
                        throw new CsvParsingException(MessageCodes.MFS_094, row[objectTypeIndex]);
                    }
                }
            }
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
    private List<String[]> extractWorkMetadata(final String[] aRow) throws CsvParsingException {
        final Optional<String> parentID = getMetadata(aRow[myCsvHeaders.getParentArkIndex()]);
        final Optional<String> workID = getMetadata(aRow[myCsvHeaders.getItemArkIndex()]);
        final Optional<String> label = getMetadata(aRow[myCsvHeaders.getTitleIndex()]);

        // Store the work data for full manifest creation
        myWorksList.add(aRow);

        // Create a brief work manifest for inclusion in the collection manifest
        if (workID.isPresent() && label.isPresent()) {
            final URI uri = IDUtils.getResourceURI(Constants.URL_PLACEHOLDER, IDUtils.getWorkS3Key(workID.get()));
            final Collection.Manifest manifest = new Collection.Manifest(uri.toString(), label.get());

            LOGGER.debug(MessageCodes.MFS_119, workID, parentID);

            if (parentID.isPresent()) {
                final String id = parentID.get();

                if (myWorksMap.containsKey(id)) {
                    myWorksMap.get(id).add(manifest);
                } else {
                    final List<Collection.Manifest> manifests = new ArrayList<>();

                    manifests.add(manifest);
                    myWorksMap.put(id, manifests);
                }
            } else {
                throw new CsvParsingException(MessageCodes.MFS_107);
            }
        } else {
            throw new CsvParsingException(MessageCodes.MFS_108);
        }

        return myWorksList;
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
        final Optional<String> parentID = getMetadata(aRow[myCsvHeaders.getParentArkIndex()]);

        if (parentID.isPresent()) {
            final String id = parentID.get();
            final List<String[]> page;

            if (myPagesMap.containsKey(id)) {
                myPagesMap.get(id).add(aRow);
            } else {
                page = new ArrayList<>();
                page.add(aRow);
                myPagesMap.put(id, page);
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
     * Check for hard returns in metadata values.
     *
     * @param aRow A row from the metadata CSV
     * @throws CsvParsingException If the metadata contains a hard return
     */
    private void checkForEOLs(final String[] aRow) throws CsvParsingException {
        for (int index = 0; index < aRow.length; index++) {
            if (EOL_PATTERN.matcher(aRow[index]).find()) {
                throw new CsvParsingException(MessageCodes.MFS_093, aRow[index]);
            }
        }
    }
}
