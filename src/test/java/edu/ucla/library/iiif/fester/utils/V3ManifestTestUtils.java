
package edu.ucla.library.iiif.fester.utils;

import java.util.List;
import java.util.Optional;

import info.freelibrary.iiif.presentation.v3.Manifest;
import info.freelibrary.iiif.presentation.v3.properties.Label;
import info.freelibrary.iiif.presentation.v3.properties.Metadata;
import info.freelibrary.iiif.presentation.v3.utils.JSON;

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
        final Manifest manifest = JSON.readValue(aJsonManifest, Manifest.class);
        final List<Metadata> metadataList = manifest.getMetadata();

        for (Metadata metadata : metadataList) {
            Optional<String> labelOpt = metadata.getLabel().getFirstValue();

            if (labelOpt.isPresent() && labelOpt.get().equals(aMetadataLabel)) {
                return metadata.getValue().getFirstValue();
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> getLabel(final String aJsonManifest) {
        final Manifest manifest = JSON.readValue(aJsonManifest, Manifest.class);
        final Optional<Label> optLabel = manifest.getLabel();

        // All our manifests currently have only one label
        if (optLabel.isPresent()) {
            return optLabel.get().getFirstValue();
        } else {
            return Optional.empty();
        }
    }

}
