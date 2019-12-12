
package edu.ucla.library.iiif.fester.utils;

import static edu.ucla.library.iiif.fester.Constants.MANIFEST;
import static edu.ucla.library.iiif.fester.Constants.SLASH;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;

import edu.ucla.library.iiif.fester.Constants;

/**
 * A utilities class for working with IDs.
 */
public final class IDUtils {

    private static final int MAX_ID_COUNT = 1000;

    private static final int DEFAULT_ID_LENGTH = 6;

    private IDUtils() {
    }

    /**
     * Gets a list of unique IDs.
     *
     * @return A list of unique IDs
     */
    public static List<String> getIDs() {
        return getIDs(MAX_ID_COUNT, DEFAULT_ID_LENGTH);
    }

    /**
     * Gets a list of unique IDs.
     *
     * @return A list of unique IDs
     */
    public static List<String> getIDs(final int aCount, final int aIDLength) {
        final Set<String> idSet = new HashSet<>(aCount);

        // Could do this without random by pre-generating a list and shuffling
        for (int index = 0; index < aCount; index++) {
            if (!idSet.add(RandomStringUtils.random(aIDLength, true, true))) {
                index -= 1;
            }
        }

        return Arrays.asList(idSet.toArray(new String[] {}));
    }

    /**
     * Gets the last part of an ID that uses slashes to indicate parts (e.g. ARKs).
     *
     * @param aID An ID
     * @return A string that can be used as a part of a larger ID pattern
     */
    public static String getLastPart(final String aID) {
        final String id = URLDecoder.decode(aID, StandardCharsets.UTF_8);
        final String[] parts = id.split(Constants.SLASH);

        return parts[parts.length - 1];
    }

    /**
     * Encode a collection manifest ID.
     *
     * @param aHost The host at which Fester is running
     * @param aCollectionsPath The path at which the manifest is served
     * @param aID The manifest ID
     * @return An encoded manifest ID
     */
    public static String encode(final String aHost, final String aCollectionsPath, final String aID) {
        final StringBuilder sb = new StringBuilder();
        final String encodedID = URLEncoder.encode(aID, StandardCharsets.UTF_8);

        return sb.append(aHost).append(aCollectionsPath).append(SLASH).append(encodedID).toString();
    }

    /**
     * Encode a work manifest ID.
     *
     * @param aHost The host at which Fester is running
     * @param aID The manifest ID
     * @return An encoded manifest ID
     */
    public static String encode(final String aHost, final String aID) {
        final StringBuilder sb = new StringBuilder();
        final String encodedID = URLEncoder.encode(aID, StandardCharsets.UTF_8);

        return sb.append(aHost).append(SLASH).append(encodedID).append(SLASH).append(MANIFEST).toString();
    }

    /**
     * Decodes an encoded collection ID.
     *
     * @param aID The encoded collection ID
     * @param aCollectionsPath The path at which collections can be found
     * @return A decoded ID
     */
    public static String decode(final URI aID, final String aCollectionsPath) {
        final int startIndex = aCollectionsPath.length() + 1;

        // Note that getPath() URL decodes the path too
        return aID.getPath().substring(startIndex);
    }

    /**
     * Decodes an encoded work ID.
     *
     * @param aID The encoded work ID
     * @return A decoded ID
     */
    public static String decode(final URI aID) {
        final String path = aID.getPath();
        final int endIndex = path.lastIndexOf('/');

        return path.substring(1, endIndex);
    }
}
