
package edu.ucla.library.iiif.fester.utils;

import java.util.Comparator;

import info.freelibrary.iiif.presentation.v2.Collection;

import se.sawano.java.text.AlphanumericComparator;

/**
 * Comparator for sorting manifests alpha-numerically based on their label. Note that this comparison is based on the
 * assumption that all work manifests embedded in a collection manifest have exactly one label.
 * <p>
 * FIXME: Use the v3 Collection.Manifest
 */
public class V3ManifestLabelComparator implements Comparator<Collection.Manifest> {

    private final AlphanumericComparator myComparator = new AlphanumericComparator();

    @Override
    public int compare(final Collection.Manifest aFirstManifest, final Collection.Manifest aSecondManifest) {
        final String firstLabel = aFirstManifest.getLabel().getValues().get(0).getValue();
        final String secondLabel = aSecondManifest.getLabel().getValues().get(0).getValue();

        return myComparator.compare(firstLabel, secondLabel);
    }

}
