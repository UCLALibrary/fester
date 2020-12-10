
package edu.ucla.library.iiif.fester.utils;

import edu.ucla.library.iiif.fester.Constants;

import java.util.List;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class ThumbnailUtils {
    private static final String HEADER_THUMB = "thumbnail";

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
            return csvList;
        }
    }

   /**
     * Adds a base IIF URL for the thumnail image.
     *
     * @param aRow A row from a CSV
     * @param aURL The thumbnail URL
     * @return The updated row
   */
    public static void addThumbnailURL(final String[] aRow, final String aURL) {
        final int thumbnailIndex = Arrays.asList(aRow).indexOf(HEADER_THUMB);
        if (aRow[thumbnailIndex] == null || aRow[thumbnailIndex].trim().equals(Constants.EMPTY) ) {
            aRow[thumbnailIndex] = aURL;
        }
    }

   /**
     * Chooses the index for a randomly-selected thumbnail.
     *
     * @param aMax Maximum value in selection range
     * @return Random number between 3 and aMax
   */
    public static int pickThumbnailIndex(final int aMax) {
        final int min = 3;
        return (int) (Math.random() * (aMax - min + 1) + min);
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
