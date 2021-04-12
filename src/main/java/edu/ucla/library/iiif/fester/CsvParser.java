
package edu.ucla.library.iiif.fester;

import static edu.ucla.library.iiif.fester.Constants.EMPTY;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import com.opencsv.CSVIterator;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

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

    //private static final String AV_URL_STRING = "pairtree";

    private final Map<String, List<String[]>> myWorksMap = new HashMap<>();

    private final Map<String, List<String[]>> myPagesMap = new LinkedHashMap<>();

    private final List<String[]> myWorksList = new ArrayList<>();

    private String[] myCollectionData;

    private String myAVUrlString;

    private CsvHeaders myCsvHeaders;

    /**
     * Creates a new CsvParser.
     */
    public CsvParser() {

    }

    /**
     * Parses the CSV file at the supplied path. This is not thread-safe. Optional CSV columns: IIIF Access URL, Item
     * Sequence (if the CSV contains no page rows), viewingHint, viewingDirection, Name.repository,
     * Rights.statementLocal, Rights.servicesContact,
     *
     * @param aPath A path to a CSV file
     * @param aIiifVersion The target IIIF Presentation API version
     * @return This CSV parser
     * @throws IOException If there is trouble reading or writing data
     * @throws CsvException If there is trouble reading the CSV data
     * @throws CsvParsingException If there is trouble parsing the CSV data
     */
    public CsvParser parse(final Path aPath, final String aIiifVersion)
            throws IOException, CsvException, CsvParsingException {
        reset();

        try (CSVReader csvReader = new CSVReader(Files.newBufferedReader(aPath))) {
            int rowsRead = 0;
            int works = 0;
            int pages = 0;

            final CSVIterator csvIterator = new CSVIterator(csvReader);
            final List<String[]> rows = new LinkedList<>();
            final Set<ObjectType> csvObjectTypes;

            while (csvIterator.hasNext()) {
                final String[] nextRow = csvIterator.next();

                // Skip blank rows
                if (!(nextRow.length == 1 && EMPTY.equals(nextRow[0].trim())) &&
                        !EMPTY.equals(String.join(EMPTY, nextRow).trim())) {
                    rows.add(nextRow);
                }
            }

            // Remove the header row from the list of rows
            myCsvHeaders = new CsvHeaders(checkForEOLs(rows.remove(0)));

            // Get the set of object types that we'll be mapping CSV rows to
            csvObjectTypes = getObjectTypes(rows, myCsvHeaders);

            // Required CSV columns
            if (!myCsvHeaders.hasItemArkIndex()) {
                throw new CsvParsingException(MessageCodes.MFS_113);
            } else if (!myCsvHeaders.hasParentArkIndex()) {
                throw new CsvParsingException(MessageCodes.MFS_114);
            } else if (!myCsvHeaders.hasTitleIndex()) {
                throw new CsvParsingException(MessageCodes.MFS_111);
            } else if (!myCsvHeaders.hasFileNameIndex()) {
                throw new CsvParsingException(MessageCodes.MFS_112);
            } else if (!myCsvHeaders.hasItemSequenceIndex() && csvObjectTypes.contains(ObjectType.PAGE)) {
                throw new CsvParsingException(MessageCodes.MFS_123);
            }

            for (final String[] row : rows) {
                checkForEOLs(row);
                trimValues(row);
                if (aIiifVersion != null) {
                    checkApiCompatibility(row, aPath, aIiifVersion);
                }

                switch (getObjectType(row, myCsvHeaders)) {
                    case COLLECTION: {
                        extractCollectionMetadata(row);
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
     * Parses the CSV file at the supplied path. This is not thread-safe. Optional CSV columns: IIIF Access URL, Item
     * Sequence (if the CSV contains no page rows), viewingHint, viewingDirection, Name.repository,
     * Rights.statementLocal, Rights.servicesContact,
     *
     * @param aPath A path to a CSV file
     * @return This CSV parser
     * @throws IOException If there is trouble reading or writing data
     * @throws CsvException If there is trouble reading the CSV data
     * @throws CsvParsingException If there is trouble parsing the CSV data
     */
    public CsvParser parse(final Path aPath) throws IOException, CsvException, CsvParsingException {
        return parse(aPath, null);
    }

    /**
     * Gets the collection parsed from the CSV data. Not all CSV data will have a collection.
     *
     * @return An optional collection
     */
    public Optional<String[]> getCsvCollection() {
        return Optional.ofNullable(myCollectionData);
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
        myCollectionData = null;
        myCsvHeaders = null;

        myWorksList.clear();
        myPagesMap.clear();
        myWorksMap.clear();

        return this;
    }

    /**
     * Extracts the collection metadata, checking that required properties are there.
     *
     * @param aRow The row of collection metadata
     * @throws CsvParsingException If there is trouble parsing the data
     */
    private void extractCollectionMetadata(final String... aRow) throws CsvParsingException {
        if (getMetadata(aRow, myCsvHeaders.getItemArkIndex()).isPresent()) {
            if (getMetadata(aRow, myCsvHeaders.getTitleIndex()).isEmpty()) {
                throw new CsvParsingException(MessageCodes.MFS_104);
            }

            myCollectionData = aRow;
        } else {
            throw new CsvParsingException(MessageCodes.MFS_106);
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
    private void extractWorkMetadata(final String... aRow) throws CsvParsingException {
        final Optional<String> parentIdOpt = getMetadata(aRow, myCsvHeaders.getParentArkIndex());
        final Optional<String> workIdOpt = getMetadata(aRow, myCsvHeaders.getItemArkIndex());
        final Optional<String> labelOpt = getMetadata(aRow, myCsvHeaders.getTitleIndex());

        // Store the work data for full manifest creation
        myWorksList.add(aRow);

        // Create a brief work manifest for inclusion in the collection manifest
        if (workIdOpt.isPresent() && labelOpt.isPresent()) {
            final String workID = workIdOpt.get();
            final URI uri = IDUtils.getResourceURI(Constants.URL_PLACEHOLDER, IDUtils.getWorkS3Key(workID));
            final String[] workData = new String[] { uri.toString(), labelOpt.get() };

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(MessageCodes.MFS_119, workID, parentIdOpt.orElse(Constants.EMPTY));
            }

            if (parentIdOpt.isPresent()) {
                final String parentID = parentIdOpt.get();

                if (myWorksMap.containsKey(parentID)) {
                    myWorksMap.get(parentID).add(workData);
                } else {
                    final List<String[]> worksData = new ArrayList<>();

                    worksData.add(workData);
                    myWorksMap.put(parentID, worksData);
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
    private void extractPageMetadata(final String... aRow) throws CsvParsingException {
        final Optional<String> workIdOpt = getMetadata(aRow, myCsvHeaders.getParentArkIndex());

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
     * Checks for hard returns in metadata values, and returns the row if none are found.
     *
     * @param aRow A row from the metadata CSV
     * @return The row
     * @throws CsvParsingException If the metadata contains a hard return
     */
    private String[] checkForEOLs(final String... aRow) throws CsvParsingException {
        for (int index = 0; index < aRow.length; index++) {
            if (EOL_PATTERN.matcher(aRow[index]).find()) {
                throw new CsvParsingException(MessageCodes.MFS_093, aRow[index]);
            }
        }
        return aRow;
    }

    /**
     * Removes leading/trailing whitespace from row entries
     *
     * @param aRow A row from the metadata CSV
     * @return The row
     */
    private String[] trimValues(final String... aRow) {
        for (int index = 0; index < aRow.length; index++) {
            if (aRow[index] != null) {
                aRow[index] = aRow[index].trim();
            }
        }
        return aRow;
    }

    /**
     * Checks for A/V metadata in rows that represent v2 canvases, and returns the row if none is found.
     *
     * @param aRow A row from the metadata CSV
     * @param aPath A path to a CSV file
     * @return The row
     * @throws CsvParsingException If the row represents a v2 canvas and contains any A/V metadata
     */
    @SuppressWarnings({"unchecked", "BooleanExpressionComplexity"})
    private String[] checkApiCompatibility(final String[] aRow, final Path aPath, final String aIiifVersion)
            throws CsvParsingException {
        final String rowId = getMetadata(aRow, myCsvHeaders.getItemArkIndex()).get();

        final Optional<Integer> mediaWidth =
                (Optional<Integer>) getMetadata(aRow, myCsvHeaders.getMediaWidthIndex(), Integer.class, aPath);
        final Optional<Integer> mediaHeight =
                (Optional<Integer>) getMetadata(aRow, myCsvHeaders.getMediaHeightIndex(), Integer.class, aPath);
        final Optional<Float> mediaDuration =
                (Optional<Float>) getMetadata(aRow, myCsvHeaders.getMediaDurationIndex(), Float.class, aPath);
        final Optional<String> mediaFormat = getMetadata(aRow, myCsvHeaders.getMediaFormatIndex());
        final Optional<String> audioVideoAccessUrl = getMetadata(aRow, myCsvHeaders.getContentAccessUrlIndex());

        if (Constants.IIIF_API_V2.equals(aIiifVersion)) {
            if (mediaWidth.isPresent() || mediaHeight.isPresent() || mediaDuration.isPresent() ||
                    mediaFormat.isPresent() || (audioVideoAccessUrl.isPresent() &&
                    audioVideoAccessUrl.toString().contains(myAVUrlString))) {
                throw new CsvParsingException(MessageCodes.MFS_168, rowId, aPath);
            }
        } else { // Constants.IIIF_API_V3
            if (mediaFormat.isPresent()) {
                final String format = mediaFormat.get();
                final String primaryType;

                try {
                    primaryType = new MimeType(format).getPrimaryType();
                } catch (final MimeTypeParseException details) {
                    throw new CsvParsingException(MessageCodes.MFS_169, format, rowId, aPath);
                }

                switch (primaryType) {
                    case "video": {
                        if (mediaWidth.isEmpty() || mediaHeight.isEmpty() || mediaDuration.isEmpty() ||
                                audioVideoAccessUrl.isEmpty()) {
                            throw new CsvParsingException(MessageCodes.MFS_170, rowId, format, aPath);
                        }
                        if (audioVideoAccessUrl.isPresent() &&
                            !audioVideoAccessUrl.toString().contains(myAVUrlString)) {
                            throw new CsvParsingException(MessageCodes.MFS_176, rowId, format, aPath);
                        }
                        break;
                    }
                    case "audio": {
                        if (mediaDuration.isEmpty() || audioVideoAccessUrl.isEmpty()) {
                            throw new CsvParsingException(MessageCodes.MFS_174, rowId, format, aPath);
                        }
                        if (!mediaWidth.isEmpty() || !mediaHeight.isEmpty()) {
                            throw new CsvParsingException(MessageCodes.MFS_175, rowId, format, aPath);
                        }
                        if (audioVideoAccessUrl.isPresent() &&
                            !audioVideoAccessUrl.toString().contains(myAVUrlString)) {
                            throw new CsvParsingException(MessageCodes.MFS_176, rowId, format, aPath);
                        }
                        break;
                    }
                    default: {
                        throw new CsvParsingException(MessageCodes.MFS_171, primaryType, rowId, aPath);
                    }
                }
            } else if (mediaWidth.isPresent() || mediaHeight.isPresent() || mediaDuration.isPresent() ||
                    (audioVideoAccessUrl.isPresent() && audioVideoAccessUrl.toString().contains(myAVUrlString))) {
                throw new CsvParsingException(MessageCodes.MFS_172, rowId, aPath);
            }
        }
        return aRow;
    }

    /**
     * Gets the type of the object represented by a CSV row.
     *
     * @param aRow A row from the metadata CSV
     * @param aCsvHeaders CSV header information
     * @return The object type
     * @throws CsvParsingException If object type isn't included in the CSV headers, or the object type index is out
     *         of bounds of the CSV row, or the metadata contains an unknown object type
     */
    public static ObjectType getObjectType(final String[] aRow, final CsvHeaders aCsvHeaders)
            throws CsvParsingException {
        if (aCsvHeaders.hasObjectTypeIndex()) {
            final int objectTypeIndex = aCsvHeaders.getObjectTypeIndex();

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

    /**
     * Gets the types of the objects represented by the CSV rows.
     *
     * @param aCsvRows A list of rows from the metadata CSV
     * @param aCsvHeaders CSV header information
     * @return The set of object types
     * @throws CsvParsingException If object type isn't included in the CSV headers, or the object type index is out
     *         of bounds of the CSV row, or the metadata contains an unknown object type
     */
    public static Set<ObjectType> getObjectTypes(final List<String[]> aCsvRows, final CsvHeaders aCsvHeaders)
            throws CsvParsingException {
        final Set<ObjectType> objectTypes = EnumSet.noneOf(ObjectType.class);

        for (final String[] row : aCsvRows) {
            objectTypes.add(getObjectType(row, aCsvHeaders));
        }

        return objectTypes;
    }

    /**
     * Gets the metadata from the supplied row and index position.
     *
     * @param aRow A row of metadata
     * @param aIndex An index position of the metadata to retrieve
     * @return An optional metadata value
     */
    public static Optional<String> getMetadata(final String[] aRow, final int aIndex) {
        try {
            return Optional.ofNullable(StringUtils.trimToNull(aRow[aIndex]));
        } catch (final IndexOutOfBoundsException details) {
            return Optional.empty();
        }
    }

    /**
     * Gets the metadata from the supplied row and index position.
     *
     * @param aRow A row of metadata
     * @param aIndex An index position of the metadata to retrieve
     * @param aType The type of data that is expected in the field
     * @param aPath The path of the current CSV
     * @return An optional metadata value
     * @throws CsvParsingException
     */
    public static Optional<?> getMetadata(final String[] aRow, final int aIndex, final Class<?> aType,
            final Path aPath) throws CsvParsingException {
        try {
            final String rawValue = aRow[aIndex];

            if (rawValue.equals(EMPTY)) {
                return Optional.empty();
            } else {
                try {
                    if (aType == Integer.class) {
                        return Optional.of(Integer.parseInt(StringUtils.trimTo(rawValue, EMPTY)));
                    } else if (aType == Float.class) {
                        return Optional.of(Float.parseFloat(StringUtils.trimTo(rawValue, EMPTY)));
                    } else if (aType == String.class) {
                        return getMetadata(aRow, aIndex);
                    } else {
                        throw new CsvParsingException(MessageCodes.MFS_173, rawValue, aPath, aType);
                    }
                } catch (final NumberFormatException details) {
                    throw new CsvParsingException(MessageCodes.MFS_173, rawValue, aPath, aType);
                }
            }
        } catch (final IndexOutOfBoundsException details) {
            return Optional.empty();
        }
    }

    /**
     * Sets the A/V URL identifier string.
     *
     * @param aAVUrlString The A/V URL identifier string
     * @return This CSV parser
     */
    public CsvParser setAVUrlString(final String aAVUrlString) {
        myAVUrlString = aAVUrlString;
        return this;
    }
}
