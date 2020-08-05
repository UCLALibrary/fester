
package edu.ucla.library.iiif.fester.utils;

import java.util.UUID;

import io.vertx.core.Vertx;

/**
 * Utilities related to working with test IDs.
 */
public final class TestUtils {

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
