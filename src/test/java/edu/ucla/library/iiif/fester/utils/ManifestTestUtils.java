
package edu.ucla.library.iiif.fester.utils;

import java.util.Optional;

/**
 * An interface for utilities used to testing IIIF manifests.
 */
public interface ManifestTestUtils {

    /**
     * Gets the API version of the manifest test utility implementation.
     *
     * @return A IIIF API version
     */
    String getApiVersion();

    /**
     * Gets the requested metadata.
     *
     * @param aJsonManifest A JSON manifest
     * @param aMetadataLabel A metadata label
     * @return The value of the requested metadata entry
     */
    Optional<String> getMetadata(String aJsonManifest, String aMetadataLabel);

}
