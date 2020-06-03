package edu.ucla.library.iiif.fester.utils;

import java.util.Comparator;
import java.util.Iterator;

import info.freelibrary.iiif.presentation.Collection;
import info.freelibrary.iiif.presentation.properties.Value;

import se.sawano.java.text.AlphanumericComparator;

/**
 * Comparator for sorting manifests alpha-numerically based on their label.
 */
public class ManifestLabelComparator implements Comparator<Collection.Manifest> {

    private final AlphanumericComparator myComparator = new AlphanumericComparator();

    /**
     * Creates an Manifest Label comparator.
     */
    public ManifestLabelComparator() {
    }

    @Override
    public int compare(final Collection.Manifest aFirstManifest, final Collection.Manifest aSecondManifest) {
        // Each label is a list of values
        final Iterator<Value> firstLabelValues = aFirstManifest.getLabel().getValues().iterator();
        final Iterator<Value> secondLabelValues = aSecondManifest.getLabel().getValues().iterator();

        // Compare values until we reach the end of at least one of the labels
        while (firstLabelValues.hasNext() && secondLabelValues.hasNext()) {
            final String firstLabelValue = firstLabelValues.next().getValue();
            final String secondLabelValue = secondLabelValues.next().getValue();
            final int valueComparison = myComparator.compare(firstLabelValue, secondLabelValue);

            if (valueComparison != 0) {
                return valueComparison;
            }
        }

        if (!firstLabelValues.hasNext() && secondLabelValues.hasNext()) {
            // firstLabelValues is shorter than, but otherwise equal to, secondLabelValues
            return -1;
        } else if (firstLabelValues.hasNext() && !secondLabelValues.hasNext()) {
            // vice-versa
            return 1;
        } else {
            return 0;
        }
    }
}
