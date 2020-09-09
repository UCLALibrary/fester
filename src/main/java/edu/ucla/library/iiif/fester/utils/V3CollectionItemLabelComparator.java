
package edu.ucla.library.iiif.fester.utils;

import java.util.Comparator;

import info.freelibrary.iiif.presentation.v3.Collection;

import se.sawano.java.text.AlphanumericComparator;

/**
 * Comparator for sorting collection items alpha-numerically based on their label. Note that this comparison is based on
 * the assumption that all items embedded in a collection have exactly one label.
 */
public class V3CollectionItemLabelComparator implements Comparator<Collection.Item> {

    private final AlphanumericComparator myComparator = new AlphanumericComparator();

    @Override
    public int compare(final Collection.Item aFirstCollectionItem, final Collection.Item aSecondCollectionItem) {
        final Collection.Item.Type firstType = aFirstCollectionItem.getType();
        final Collection.Item.Type secondType = aSecondCollectionItem.getType();

        final String firstLabel;
        final String secondLabel;

        if (firstType == secondType) {
            firstLabel = aFirstCollectionItem.getLabel().getString();
            secondLabel = aSecondCollectionItem.getLabel().getString();

            return myComparator.compare(firstLabel, secondLabel);
        } else {
            // Sort Collections before Manifests
            if (firstType == Collection.Item.Type.Collection && secondType == Collection.Item.Type.Manifest) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
