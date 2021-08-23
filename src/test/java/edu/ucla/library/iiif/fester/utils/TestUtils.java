
package edu.ucla.library.iiif.fester.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.freelibrary.util.StringUtils;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;


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

    /**
     * Determines if the given manifests are effectively equal.
     *
     * Note that this method does not consider integers and their corresponding floating point representations to be
     * equal (e.g., 60 != 60.0), so any JSON test fixtures with integral duration values should be modified so that they
     * all have a ".0" appended to the end of the value. For example, <code>"duration": 60</code> should be changed to
     * <code>"duration": 60.0</code>.
     *
     * @param aManifestArray An array of IIIF manifests
     * @return true if they are all equal after "de-randomizing" IIIF resource IDs, false otherwise
     */
    public static boolean manifestsAreEffectivelyEqual(final JsonObject... aManifestArray) {
        final String annoPageIdPattern = "anno-page-[0-9a-z]{4}";
        final String annoIdPattern = "anno-[0-9a-z]{4}";
        final String canvasIdPattern = "canvas-[0-9a-z]{4}";

        // anno-page must come before anno since "page" also matches [0-9a-z]{4}
        final Pattern idPattern = Pattern.compile(StringUtils.format("(?<annopage>{})|(?<anno>{})|(?<canvas>{})",
                annoPageIdPattern, annoIdPattern, canvasIdPattern));

        final long distinctManifestCount = Arrays.stream(aManifestArray).map(manifest -> {

            final Matcher idMatcher = idPattern.matcher(manifest.toString());
            final StringBuilder sb = new StringBuilder();

            final Map<String, String> annoPageIdReplacementMap = new HashMap<>();
            final Map<String, String> annoIdReplacementMap = new HashMap<>();
            final Map<String, String> canvasIdReplacementMap = new HashMap<>();

            int annoPageIdMatchCount = 0;
            int annoIdMatchCount = 0;
            int canvasIdMatchCount = 0;

            // De-randomize all the IIIF resource IDs so the manifests can be compared
            while (idMatcher.find()) {
                final String annoPageIdMatch = idMatcher.group("annopage");
                final String annoIdMatch = idMatcher.group("anno");
                final String canvasIdMatch = idMatcher.group("canvas");

                final String replacementId;

                if (annoPageIdMatch != null) {
                    if (annoPageIdReplacementMap.containsKey(annoPageIdMatch)) {
                        // Use the same de-randomized ID for instances of the same ID
                        replacementId = annoPageIdReplacementMap.get(annoPageIdMatch);
                    } else {
                        // Create a new de-randomized ID
                        replacementId = StringUtils.format("anno-page-{}", ++annoPageIdMatchCount);
                        annoPageIdReplacementMap.put(annoPageIdMatch, replacementId);
                    }
                } else if (annoIdMatch != null) {
                    if (annoIdReplacementMap.containsKey(annoIdMatch)) {
                        replacementId = annoIdReplacementMap.get(annoIdMatch);
                    } else {
                        replacementId = StringUtils.format("anno-{}", ++annoIdMatchCount);
                        annoIdReplacementMap.put(annoIdMatch, replacementId);
                    }
                } else { // canvas
                    if (canvasIdReplacementMap.containsKey(canvasIdMatch)) {
                        replacementId = canvasIdReplacementMap.get(canvasIdMatch);
                    } else {
                        replacementId = StringUtils.format("canvas-{}", ++canvasIdMatchCount);
                        canvasIdReplacementMap.put(canvasIdMatch, replacementId);
                    }
                }
                idMatcher.appendReplacement(sb, replacementId);
            }
            idMatcher.appendTail(sb);

            return new JsonObject(sb.toString());

        }).distinct().count();

        return distinctManifestCount == 1;
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
