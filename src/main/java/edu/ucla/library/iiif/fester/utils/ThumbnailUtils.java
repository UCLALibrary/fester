
package edu.ucla.library.iiif.fester.utils;

import edu.ucla.library.iiif.fester.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility methods for thumbnail selection.
*/
public final class ThumbnailUtils {

    /**
     * Header-row name for thumbnail column.
     */
    private static final String HEADER_THUMB = "Thumbnail";

    /**
     * Private constructor for ThumbnailUtils class.
     */
    private ThumbnailUtils() {
    }

    /**
      * Adds a "thumbnail" column to a CSV if needed..
      *
      * @param aCsvList A CSV parsed into a list of arrays
      * @return The CSV with added "thumbnail" column
      */
    public static List<String[]> addThumbnailColumn(final List<String[]> aCsvList) {
        Objects.requireNonNull(aCsvList);
        final List<String[]> csvList;

        if (hasThumbnailColumn(aCsvList.get(0))) {
            return aCsvList;
        } else {
            final int newArraySize = aCsvList.get(0).length + 1;
            csvList = new ArrayList<>(aCsvList.size());
            aCsvList.stream().forEach(entry -> csvList.add(Arrays.copyOf(entry, newArraySize)));
            csvList.get(0)[newArraySize - 1] = HEADER_THUMB;
            return csvList;
        }
    }

    /**
     * Adds a base IIF URL for the thumnail image.
     *
     * @param aColumnIndex Index in CSV row where thumbnail URL will be added
     * @param aRowIndex Index in CSV of row being modified
     * @param aURL The thumbnail URL
     * @param aCsvList A CSV parsed into a list of arrays
     */
    public static void addThumbnailURL(final int aColumnIndex, final int aRowIndex,
                                       final String aURL, final List<String[]> aCsvList) {
        Objects.requireNonNull(aURL);
        Objects.requireNonNull(aCsvList);
        if (aCsvList.get(aRowIndex)[aColumnIndex] == null ||
            aCsvList.get(aRowIndex)[aColumnIndex].trim().equals(Constants.EMPTY)) {
            aCsvList.get(aRowIndex)[aColumnIndex] = aURL;
        }
    }

    /**
      * Return the index of the thumbnail column in a CSV header row.
      *
      * @param aRow Header row from a CSV
      * @return Index of the thumbnail header
      */
    public static int findThumbHeaderIndex(final String... aRow) {
        return Arrays.asList(aRow).indexOf(HEADER_THUMB);
    }

    /**
      * Chooses the index for a randomly-selected thumbnail.
      *
      * @param aMax Maximum value in selection range
      * @return Random number between 2 (index position 3) and aMax
      */
    public static int pickThumbnailIndex(final int aMax) {
        final int min = 2;
        return ThreadLocalRandom.current().nextInt(min, aMax + 1);
    }

    /**
      * Determines whether a CSV has a "Thumbnail" column.
      *
      * @param aHeaderRow The header row from a CSV
      * @return Yes/no for "thumbnail" column existence
      */
    private static boolean hasThumbnailColumn(final String... aHeaderRow) {
        return Arrays.stream(aHeaderRow).anyMatch(HEADER_THUMB::equals);
    }

}
