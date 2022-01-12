
package edu.ucla.library.iiif.fester.utils;

import java.util.UUID;

import info.freelibrary.util.StringUtils;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;

/**
 * Utilities related to working with test IDs.
 */
public final class TestUtils {

    /**
     * A regular expression pattern for IDs.
     */
    private static final String ID_PATTERN = "\\\"{}\\\":\\\"[a-zA-Z0-9\\-\\.\\:\\%\\/\\,]*\\\"";

    /**
     * A pattern for a sanitized JSON ID key and value.
     */
    private static final String EMPTY_ID = "\"{}\":\"\"";

    /**
     * An array of ID keys that might be found in a JSON manifest.
     */
    private static final String[] ID_KEYS = new String[] { "id", "target" };

    /**
     * Creates a new test utilities class.
     */
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

    /**
     * Determines whether the supplied manifests are equal in all respects other than their IDs.
     *
     * @param aExpectedManifest The expected manifest
     * @param aFoundManifest The test's resulting manifest
     * @return Whether the manifests are effectively equal
     */
    public static boolean manifestsAreEffectivelyEqual(final JsonObject aExpectedManifest,
            final JsonObject aFoundManifest) {
        return sanitize(aExpectedManifest, ID_KEYS).equals(sanitize(aFoundManifest, ID_KEYS));
    }

    /**
     * Cleans the IDs in the supplied manifest.
     *
     * @param aManifest A manifest whose IDs should be sanitized
     * @param aKeyArray An array of ID keys from the JSON document
     * @return A manifest with its IDs sanitized to a fixed value
     */
    private static JsonObject sanitize(final JsonObject aManifest, final String[] aKeyArray) {
        String json = aManifest.encode();

        for (final String name : aKeyArray) {
            json = json.replaceAll(StringUtils.format(ID_PATTERN, name), StringUtils.format(EMPTY_ID, name));
        }

        return new JsonObject(json);
    }

    /**
     * Completes an asynchronous task.
     *
     * @param aAsyncTask An asynchronous task
     */
    public static void complete(final Async aAsyncTask) {
        if (!aAsyncTask.isCompleted()) {
            aAsyncTask.complete();
        }
    }
}
