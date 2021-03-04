
package edu.ucla.library.iiif.fester.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import info.freelibrary.util.StringUtils;

/**
 * Tests of thumbnail utilities.
 */
public class ThumbnailUtilsTest {
    private static final String TEST_CSV = "src/test/resources/csv/hathaway/batch1/works.csv";

    private static final String EXPECTED_HEADER = "src/test/resources/csv/empty_thumbs.csv";

    private static final String EXPECTED_CSV = "src/test/resources/csv/thumbs.csv";

    private static final String THUMBNAIL = "http://iiif.library.ucla.edu/collections/ark%3A%2F21198%2Fzz001ng4t6";

    /**
     * Tests adding thumbnail column the CSV data.
     *
     * @throws IOException If there is trouble reading or writing the CSV files
     * @throws CsvException If there is trouble parsing the CSV data
     */
    @Test
    public final void testAddThumbHeader() throws IOException, CsvException {
        final List<String[]> csvData = read(TEST_CSV);
        final String expected = writeToString(read(EXPECTED_HEADER));
        final String found = writeToString(ThumbnailUtils.addThumbnailColumn(csvData));

        assertEquals(expected, found);
    }

    /**
     * Tests adding thumbnail URL the CSV data.
     *
     * @throws IOException If there is trouble reading or writing the CSV files
     * @throws CsvException If there is trouble parsing the CSV data
     */
    @Test
    public final void testAddThumbnail() throws IOException, CsvException {
        final List<String[]> csvData = read(EXPECTED_HEADER);
        final int thumbColumn = ThumbnailUtils.findThumbHeaderIndex(csvData.get(0));
        for (int row = 1; row < csvData.size(); row++) {
            ThumbnailUtils.addThumbnailURL(thumbColumn, row, THUMBNAIL, csvData);
        }
        final String expected = writeToString(read(EXPECTED_CSV));
        final String found = writeToString(csvData);

        assertEquals(expected, found);
    }

    /**
     * Tests selecting thumbnail index.
     *
     */
    @Test
    public final void testPickThumbnailIndex() {
        final int min = 2;
        final int max = 20;
        int old = 0;
        for (int i = 0; i < 10; i++ ) {
            final int selected = ThumbnailUtils.pickThumbnailIndex(max);
            assertTrue(selected >= min && selected <= max);
            assertTrue(selected != old);
            old = selected;
        }
    }

    /**
     * Read a CSV file into a CSV data list.
     *
     * @param aCsvFilePath A CSV file to read
     * @return A CSV data list
     * @throws IOException If there is trouble reading or writing the CSV file
     * @throws CsvException If there is trouble parsing the CSV data
     */
    public static List<String[]> read(final String aCsvFilePath) throws IOException, CsvException {
        final Reader reader = Files.newBufferedReader(Paths.get(aCsvFilePath));

        try (CSVReader csvReader = new CSVReader(reader)) {
            return csvReader.readAll();
        }
    }

    /**
     * Write a CSV data list to a file and then read that file into a string.
     *
     * @param aCsvData A CSV data list
     * @return The string that's read from the output file
     * @throws IOException If there was trouble reading or writing the CSV data
     * @throws CsvException If there was trouble parsing the CSV data
     */
    public static String writeToString(final List<String[]> aCsvData) throws IOException, CsvException {
        final String filePath = File.createTempFile("fester", null).getAbsolutePath();
        final Writer writer = Files.newBufferedWriter(Paths.get(filePath));

        // We normalize the results by writing every file passed in with the same configs
        try (CSVWriter csvWriter = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END)) {
            csvWriter.writeAll(aCsvData);
            csvWriter.flushQuietly();

            return StringUtils.read(new File(filePath));
        }
    }
}
