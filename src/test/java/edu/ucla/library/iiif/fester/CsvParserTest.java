
package edu.ucla.library.iiif.fester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
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
        myCsvParser.parse(Paths.get(new File(DIR, GOOD_CSV).getAbsolutePath()));
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
        myCsvParser.parse(Paths.get(new File(DIR, GOOD_CSV).getAbsolutePath()));
        assertNotNull(myCsvParser.getCollection().get());
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
        final CsvHeaders csvHeaders;

        myCsvParser.parse(Paths.get(new File(DIR, GOOD_CSV).getAbsolutePath()));
        csvHeaders = myCsvParser.getCsvHeaders();

        assertEquals(6, csvHeaders.getFileNameIndex());
        assertEquals(38, csvHeaders.getImageAccessUrlIndex());
        assertEquals(1, csvHeaders.getItemArkIndex());
        assertEquals(7, csvHeaders.getItemSequenceIndex());
        assertEquals(-1, csvHeaders.getLocalRightsStatementIndex());
        assertEquals(5, csvHeaders.getObjectTypeIndex());
        assertEquals(2, csvHeaders.getParentArkIndex());
        assertEquals(20, csvHeaders.getRepositoryNameIndex());
        assertEquals(17, csvHeaders.getRightsContactIndex());
        assertEquals(23, csvHeaders.getTitleIndex());
        assertEquals(-1, csvHeaders.getViewingDirectionIndex());
        assertEquals(-1, csvHeaders.getViewingHintIndex());
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
        myCsvParser.parse(Paths.get(new File(DIR, GOOD_CSV).getAbsolutePath()));

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
        myCsvParser.parse(Paths.get(new File(DIR, "eol.csv").getAbsolutePath()));
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
        myCsvParser.parse(Paths.get(new File(DIR, "bad_obj_type.csv").getAbsolutePath()));
    }
}
