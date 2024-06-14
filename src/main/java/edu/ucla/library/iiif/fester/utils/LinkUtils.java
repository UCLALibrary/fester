
package edu.ucla.library.iiif.fester.utils;

import static edu.ucla.library.iiif.fester.Constants.EMPTY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.CSV;
import edu.ucla.library.iiif.fester.CsvHeaders;
import edu.ucla.library.iiif.fester.CsvParser;
import edu.ucla.library.iiif.fester.CsvParsingException;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.ObjectType;

/**
 * A utility class for link generation.
 */
public final class LinkUtils {

    /** A logger for the {@code LinkUtils} object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkUtils.class, MessageCodes.BUNDLE);

    /**
     * Creates a new {@code LinkUtils} instance.
     */
    private LinkUtils() {
        // This is intentionally left empty
    }

    /**
     * Adds manifest links to a supplied CSV.
     *
     * @param aHostURL The Fester host URL
     * @param aCsvList The CSV data structure
     * @return A modified CSV data structure
     */
    public static List<String[]> addManifests(final String aHostURL, final List<String[]> aCsvList) {
        Objects.requireNonNull(aHostURL);
        Objects.requireNonNull(aCsvList);

        final int manifestHeaderIndex = getColumnIndex(CSV.MANIFEST_URL, aCsvList);
        final int itemArkHeaderIndex = getColumnIndex(CSV.ITEM_ARK, aCsvList);
        final int columnLength = aCsvList.get(0).length;
        final List<String[]> csvList;

        if (manifestHeaderIndex == columnLength) {
            final Iterator<String[]> iterator = aCsvList.iterator();
            final int newArraySize = columnLength + 1;

            csvList = new ArrayList<>(aCsvList.size());

            // Populate our CSV list with larger String arrays so we can add the new column
            while (iterator.hasNext()) {
                csvList.add(Arrays.copyOf(iterator.next(), newArraySize));
            }
        } else {
            csvList = aCsvList;
        }

        for (int index = 0; index < csvList.size(); index++) {
            final String[] row = csvList.get(index);

            if (index == 0) {
                row[manifestHeaderIndex] = CSV.MANIFEST_URL;
            } else {
                try {
                    final ObjectType objectType = CsvParser.getObjectType(row, new CsvHeaders(aCsvList.get(0)));
                    final String itemARK = row[itemArkHeaderIndex];

                    // URLs vary depending on whether the row is a Collection or Work
                    if (ObjectType.COLLECTION.equals(objectType)) {
                        row[manifestHeaderIndex] =
                                IDUtils.getResourceURI(aHostURL, IDUtils.getCollectionS3Key(itemARK)).toString();
                    } else if (ObjectType.WORK.equals(objectType)) {
                        row[manifestHeaderIndex] =
                                IDUtils.getResourceURI(aHostURL, IDUtils.getWorkS3Key(itemARK)).toString();
                    } else {
                        row[manifestHeaderIndex] = EMPTY; // Use an empty placeholder for things without links
                    }
                } catch (final CsvParsingException details) {
                    // Should not be possible; we checked this on CSV submission
                    LOGGER.error(details.getMessage());
                    row[manifestHeaderIndex] = EMPTY;
                }
            }
        }

        return csvList;
    }

    private static int getColumnIndex(final String aHeader, final List<String[]> aCsvList) {
        final String[] headers = aCsvList.get(0);

        for (int index = 0; index < headers.length; index++) {
            if (aHeader.equalsIgnoreCase(headers[index])) {
                return index;
            }
        }

        // If we don't find it, we'll put it after the last array slot
        return headers.length;
    }
}
