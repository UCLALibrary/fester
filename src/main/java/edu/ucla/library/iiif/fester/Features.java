
package edu.ucla.library.iiif.fester;

import java.util.Map;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

/**
 * A class containing the configurable features that are supported by the Fester application.
 */
public final class Features {

    /** A constant for the batch ingest feature. */
    public static final String BATCH_INGEST = "fester.batch.ingest";

    /** Features' logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Features.class, Constants.MESSAGES);

    /**
     * A map of feature names.
     */
    private static final Map<String, String> FEATURE_NAMES =
            Map.of(BATCH_INGEST, LOGGER.getMessage(MessageCodes.MFS_084));

    /**
     * Creates a new features instance.
     */
    private Features() {
        // This is intentionally left empty.
    }

    /**
     * Gets the display name of a feature.
     *
     * @param aFeatureKey A feature key
     * @return The display name associated with that key
     */
    public static String getDisplayName(final String aFeatureKey) {
        return FEATURE_NAMES.get(aFeatureKey);
    }
}
