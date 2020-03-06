
package edu.ucla.library.iiif.fester.utils;

import java.util.UUID;

import io.vertx.core.Vertx;

/**
 * Utilities related to working with test IDs.
 */
public final class TestUtils {

    private static final String JSON_EXT = ".json";

    private TestUtils() {
    }

    /**
     * Gets a unique ID for testing.
     *
     * @return A unique ID
     */
    public static String getUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Gets an S3 key for the supplied work ID.
     *
     * @param aID A work ID
     * @return An S3 key
     */
    public static String getWorkS3Key(final String aID) {
        return "works/" + aID + JSON_EXT;
    }

    /**
     * Gets an S3 key for the supplied collection ID.
     *
     * @param aID A collection ID
     * @return An S3 key
     */
    public static String getCollS3Key(final String aID) {
        return "collections/" + aID + JSON_EXT;
    }

    /**
     * Gets test data from the test resources directory.
     *
     * @param aVertx A Vert.x instance
     * @param aPath A test resource path
     * @return The test data in string form
     */
    public static String getTestData(final Vertx aVertx, final String aPath) {
        return aVertx.fileSystem().readFileBlocking("src/test/resources/" + aPath).toString().trim();
    }
}
