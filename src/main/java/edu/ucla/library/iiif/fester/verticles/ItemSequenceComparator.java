/**
 *
 */

package edu.ucla.library.iiif.fester.verticles;

import java.util.Comparator;

/**
 * An Item Sequence comparator that sorts the list of CSV rows.
 */
public class ItemSequenceComparator implements Comparator<String[]> {

    private final int myIndex;

    /**
     * Creates an Item Sequence comparator from the supplied array index position.
     */
    public ItemSequenceComparator(final int aIndex) {
        myIndex = aIndex;
    }

    @Override
    public int compare(final String[] aFirstRow, final String[] aSecondRow) {
        return aFirstRow[myIndex].compareTo(aSecondRow[myIndex]);
    }

}
