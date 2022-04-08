
package edu.ucla.library.iiif.fester;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Processed metadata from a supplied CSV file.
 */
public class CsvMetadata {

    /** A map of CSV works. */
    private final Map<String, List<String[]>> myWorksMap;

    /** A map of CSV pages. */
    private final Map<String, List<String[]>> myPagesMap;

    /** A list of works from the CSV. */
    private final List<String[]> myWorksList;

    /**
     * Creates a CSV metadata object.
     *
     * @param aWorksMap A map of metadata for works stored in a collection document
     * @param aWorksList A list of works metadata
     * @param aPagesMap A map of pages metadata
     */
    public CsvMetadata(final Map<String, List<String[]>> aWorksMap, final List<String[]> aWorksList,
            final Map<String, List<String[]>> aPagesMap) {
        myWorksMap = aWorksMap;
        myWorksList = aWorksList;
        myPagesMap = aPagesMap;
    }

    /**
     * Gets the metadata for the works stored in a collection document.
     *
     * @return The metadata for the works stored in a collection document
     */
    public Map<String, List<String[]>> getWorksMap() {
        return myWorksMap;
    }

    /**
     * Gets the metadata for the works manifests.
     *
     * @return The metadata for the works manifests
     */
    public List<String[]> getWorksList() {
        return myWorksList;
    }

    /**
     * Returns whether our metadata has works.
     *
     * @return True if our metadata has works; else, false
     */
    public boolean hasWorks() {
        return !myWorksList.isEmpty();
    }

    /**
     * Gets the collection ID of the first work (if any) in the original CSV.
     *
     * @param aIndex The position of the parent ARK in the row
     * @return An optional collection ID
     */
    public Optional<String> getFirstCollectionID(final int aIndex) {
        if (hasWorks()) {
            return Optional.ofNullable(myWorksList.get(0)[aIndex]);
        }

        return Optional.empty();
    }

    /**
     * Gets the metadata for the manifest pages.
     *
     * @return The metadata for the manifest pages
     */
    public Map<String, List<String[]>> getPagesMap() {
        return myPagesMap;
    }

    /**
     * Tests whether the CSV metadata has page data.
     *
     * @return True if pages are found; else, false
     */
    public boolean hasPages() {
        return !myPagesMap.isEmpty();
    }

    /**
     * Returns an page iterator that has the work ID as the entry key.
     *
     * @return A page iterator
     */
    public Iterator<Entry<String, List<String[]>>> getPageIterator() {
        return myPagesMap.entrySet().iterator();
    }
}
