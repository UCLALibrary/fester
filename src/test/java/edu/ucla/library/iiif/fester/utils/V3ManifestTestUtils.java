
package edu.ucla.library.iiif.fester.utils;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import info.freelibrary.iiif.presentation.v3.Manifest;
import info.freelibrary.iiif.presentation.v3.properties.Metadata;

/**
 * Test utilities related to v3 manifests.
 */
public class V3ManifestTestUtils implements ManifestTestUtils {

    /**
     * Creates a new test utilities class.
     */
    public V3ManifestTestUtils() {
    }

    @Override
    public String getApiVersion() {
        return "v3";
    }

    @Override
    public Optional<String> getMetadata(final String aJsonManifest, final String aMetadataLabel) {
        final Manifest manifest = Manifest.fromString(aJsonManifest);
        final List<Metadata> metadataList = manifest.getMetadata();
        final Iterator<Metadata> iterator = metadataList.iterator();

        while (iterator.hasNext()) {
            final Metadata metadata = iterator.next();
            final String label = metadata.getLabel().getString();

            if (label.equals(aMetadataLabel)) {
                return Optional.of(metadata.getValue().getString());
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> getLabel(final String aJsonManifest) {
        final Manifest manifest = Manifest.fromString(aJsonManifest);

        // All our manifests currently have only one label
        return Optional.of(manifest.getLabel().getString());
    }

}
