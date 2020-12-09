
package edu.ucla.library.iiif.fester.utils;

import java.util.Optional;

import info.freelibrary.iiif.presentation.v2.Manifest;

/**
 * Test utilities related to v2 manifests.
 */
public class V2ManifestTestUtils implements ManifestTestUtils {

    /**
     * Creates a new test utilities class.
     */
    public V2ManifestTestUtils() {
    }

    @Override
    public String getApiVersion() {
        return "v2";
    }

    @Override
    public Optional<String> getMetadata(final String aJsonManifest, final String aMetadataLabel) {
        final Manifest manifest = Manifest.fromString(aJsonManifest);
        return manifest.getMetadata().getValue(aMetadataLabel);
    }

}
