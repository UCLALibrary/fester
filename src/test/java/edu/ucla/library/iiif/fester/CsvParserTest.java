
package edu.ucla.library.iiif.fester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import com.opencsv.exceptions.CsvException;

/**
 * Tests of the {@link CsvParser}.
 */
public class CsvParserTest {

    private static final File DIR = new File("src/test/resources/csv/parser-fixtures");

    private static final String GOOD_CSV = "good.csv";

    private CsvParser myCsvParser;

    /**
     * Sets up the testing environment.
     */
    @Before
    public final void setUp() {
        myCsvParser = new CsvParser();
    }

    /**
     * Tests catching an EOL in a CSV file.
     *
     * @throws CsvParsingException If there is an error while parsing the CSV data
     * @throws CsvException If there is a generic CSV error
     * @throws IOException If there is trouble reading the CSV data
     */
    @Test
    public final void testParse() throws CsvParsingException, CsvException, IOException {
        myCsvParser.parse(getTestPath(GOOD_CSV));
    }

    /**
     * Tests handling spaces in non-header rows in a CSV file.
     *
     * @throws CsvParsingException If there is an error while parsing the CSV data
     * @throws CsvException If there is a generic CSV error
     * @throws IOException If there is trouble reading the CSV data
     */
    @Test
    public final void testParseSpaces() throws CsvParsingException, CsvException, IOException {
        final CsvMetadata metadata = myCsvParser.parse(getTestPath("spaces.csv")).getCsvMetadata();
        final String first = metadata.getWorksList().get(7)[0];
        assertTrue(first.startsWith("C"));
    }

    /**
     * Tests catching an EOL in a CSV file.
     *
     * @throws CsvParsingException If there is an error while parsing the CSV data
     * @throws CsvException If there is a generic CSV error
     * @throws IOException If there is trouble reading the CSV data
     */
    @Test
    public final void testGetCollection() throws CsvParsingException, CsvException, IOException {
        assertNotNull(myCsvParser.parse(getTestPath(GOOD_CSV)).getCsvCollection().get());
    }

    /**
     * Tests getting the headers from the CSV file.
     *
     * @throws CsvParsingException If there is an error while parsing the CSV data
     * @throws CsvException If there is a generic CSV error
     * @throws IOException If there is trouble reading the CSV data
     */
    @Test
    public final void testGetCsvHeaders() throws CsvParsingException, CsvException, IOException {
        final CsvHeaders csvHeaders = myCsvParser.parse(getTestPath(GOOD_CSV)).getCsvHeaders();

        assertEquals(6, csvHeaders.getFileNameIndex());
        assertEquals(38, csvHeaders.getContentAccessUrlIndex());
        assertEquals(1, csvHeaders.getItemArkIndex());
        assertEquals(7, csvHeaders.getItemSequenceIndex());
        assertEquals(-1, csvHeaders.getLocalRightsStatementIndex());
        assertEquals(5, csvHeaders.getObjectTypeIndex());
        assertEquals(39, csvHeaders.getIiifObjectTypeIndex());
        assertEquals(2, csvHeaders.getParentArkIndex());
        assertEquals(20, csvHeaders.getRepositoryNameIndex());
        assertEquals(17, csvHeaders.getRightsContactIndex());
        assertEquals(23, csvHeaders.getTitleIndex());
        assertEquals(-1, csvHeaders.getViewingDirectionIndex());
        assertEquals(-1, csvHeaders.getViewingHintIndex());
    }

    /**
     * Tests getting headers that contain non-EOL whitespace from the CSV file.
     *
     * @throws CsvParsingException If there is an error while parsing the CSV data
     * @throws CsvException If there is a generic CSV error
     * @throws IOException If there is trouble reading the CSV data
     */
    @Test
    public final void testGetCsvHeadersWhitespace() throws CsvParsingException, IOException, CsvException {
        final CsvHeaders csvHeaders = myCsvParser.parse(getTestPath("headers_whitespace.csv")).getCsvHeaders();

        assertEquals(1, csvHeaders.getItemArkIndex());
        assertEquals(2, csvHeaders.getParentArkIndex());
        assertEquals(3, csvHeaders.getObjectTypeIndex());
        assertEquals(24, csvHeaders.getIiifObjectTypeIndex());
        assertEquals(4, csvHeaders.getFileNameIndex());
        assertEquals(5, csvHeaders.getItemSequenceIndex());
        assertEquals(12, csvHeaders.getTitleIndex());
    }

    /**
     * Tests getting the CSV metadata from the parsing process.
     *
     * @throws CsvParsingException If there is an error while parsing the CSV data
     * @throws CsvException If there is a generic CSV error
     * @throws IOException If there is trouble reading the CSV data
     */
    @Test
    public final void testGetCsvMetadata() throws CsvParsingException, CsvException, IOException {
        myCsvParser.parse(getTestPath(GOOD_CSV));

        assertEquals(20, myCsvParser.getCsvMetadata().getWorksList().size());
        assertEquals(1, myCsvParser.getCsvMetadata().getWorksMap().size());
        assertEquals(0, myCsvParser.getCsvMetadata().getPagesMap().size());
    }

    /**
     * Tests catching an EOL in a CSV file.
     *
     * @throws CsvParsingException If there is an error while parsing the CSV data
     * @throws CsvException If there is a generic CSV error
     * @throws IOException If there is trouble reading the CSV data
     */
    @Test(expected = CsvParsingException.class)
    public final void testEolCsvs() throws CsvParsingException, CsvException, IOException {
        myCsvParser.parse(getTestPath("eol.csv"));
    }

    /**
     * Tests catching invalid object types in a CSV file.
     *
     * @throws CsvParsingException If there is an error while parsing the CSV data
     * @throws CsvException If there is a generic CSV error
     * @throws IOException If there is trouble reading the CSV data
     */
    @Test(expected = CsvParsingException.class)
    public final void testObjectTypeValuesWorks() throws CsvParsingException, CsvException, IOException {
        myCsvParser.parse(getTestPath("bad_obj_type.csv"));
    }

    /**
     * Tests the parser's required CSV headers check on a works CSV without the Item Sequence column.
     *
     * @throws CsvParsingException If there is an error while parsing the CSV data
     * @throws CsvException If there is a generic CSV error
     * @throws IOException If there is trouble reading the CSV data
     */
    @Test
    public final void testWorkCsvNoItemSequence() throws CsvParsingException, CsvException, IOException {
        myCsvParser.parse(getTestPath("hathaway_batch2_works_no_item_seq.csv"));

        assertEquals(4, myCsvParser.getCsvMetadata().getWorksList().size());
        assertEquals(1, myCsvParser.getCsvMetadata().getWorksMap().size());
        assertEquals(0, myCsvParser.getCsvMetadata().getPagesMap().size());
    }

    /**
     * Tests the parser's required CSV headers check on a pages CSV without the Item Sequence column.
     *
     * @throws CsvParsingException If there is an error while parsing the CSV data
     * @throws CsvException If there is a generic CSV error
     * @throws IOException If there is trouble reading the CSV data
     */
    @Test(expected = CsvParsingException.class)
    public final void testPageCsvNoItemSequence() throws CsvParsingException, CsvException, IOException {
        myCsvParser.parse(getTestPath("hathaway5_pages_no_item_seq.csv"));
    }

    /**
     * Tests the parser's required CSV headers check on a "combined" CSV without the Item Sequence column.
     *
     * @throws CsvParsingException If there is an error while parsing the CSV data
     * @throws CsvException If there is a generic CSV error
     * @throws IOException If there is trouble reading the CSV data
     */
    @Test(expected = CsvParsingException.class)
    public final void testCollectionWorkPageCsvNoItemSequence() throws CsvParsingException, CsvException,
            IOException {
        myCsvParser.parse(getTestPath("hathaway_combined_no_item_seq.csv"));
    }

    /**
     * Gets the test fixture's path.
     *
     * @param aFixtureName The test fixture's name
     * @return The path to the test fixture
     */
    private Path getTestPath(final String aFixtureName) {
        return Paths.get(new File(DIR, aFixtureName).getAbsolutePath());
    }
}
