
package edu.ucla.library.iiif.fester.utils;

import java.util.Comparator;
import java.util.Optional;

import info.freelibrary.iiif.presentation.v3.Collection;
import info.freelibrary.iiif.presentation.v3.ResourceTypes;
import info.freelibrary.iiif.presentation.v3.properties.Label;

import se.sawano.java.text.AlphanumericComparator;

/**
 * Comparator for sorting collection items alpha-numerically based on their label. Note that this comparison is based on
 * the assumption that all items embedded in a collection have exactly one label.
 */
public class V3CollectionItemLabelComparator implements Comparator<Collection.Item> {

    private final AlphanumericComparator myComparator = new AlphanumericComparator();

    @Override
    public int compare(final Collection.Item aFirstCollectionItem, final Collection.Item aSecondCollectionItem) {

        final String firstType = aFirstCollectionItem.getType();
        final String secondType = aSecondCollectionItem.getType();

        if (firstType.equals(secondType)) {
            final Optional<Label> firstLabel = aFirstCollectionItem.getLabel();
            final Optional<Label> secondLabel = aSecondCollectionItem.getLabel();

            return Comparator.nullsLast(myComparator).compare(firstLabel.flatMap(Label::getFirstValue).orElse(null),
                    secondLabel.flatMap(Label::getFirstValue).orElse(null));
        } else {
            // Sort Collections before Manifests
            if (firstType.equals(ResourceTypes.COLLECTION) && secondType.equals(ResourceTypes.MANIFEST)) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
