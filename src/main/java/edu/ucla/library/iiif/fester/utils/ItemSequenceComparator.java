/**
 *
 */

package edu.ucla.library.iiif.fester.utils;

import java.util.Comparator;

/**
 * An Item Sequence comparator that sorts the list of CSV rows.
 */
public class ItemSequenceComparator implements Comparator<String[]> {

    private final int myIndex;

    /**
     * Creates an Item Sequence comparator from the supplied array index position.
     *
     * @param aIndex The index position of a field to compare
     */
    public ItemSequenceComparator(final int aIndex) {
        myIndex = aIndex;
    }

    @Override
    public int compare(final String[] aFirstRow, final String[] aSecondRow) {
        return Integer.valueOf(aFirstRow[myIndex]).compareTo(Integer.valueOf(aSecondRow[myIndex]));
    }

}
