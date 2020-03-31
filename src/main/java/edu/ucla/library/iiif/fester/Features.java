
package edu.ucla.library.iiif.fester;

import java.util.Map;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

public final class Features {

    public static final String BATCH_INGEST = "fester.batch.ingest";

    private static final Logger LOGGER = LoggerFactory.getLogger(Features.class, Constants.MESSAGES);

    private static final Map<String, String> FEATURE_NAMES = Map.of(BATCH_INGEST, LOGGER.getMessage(
            MessageCodes.MFS_084));

    private Features() {
    }

    public static String getDisplayName(final String aFeatureKey) {
        return FEATURE_NAMES.get(aFeatureKey);
    }
}
