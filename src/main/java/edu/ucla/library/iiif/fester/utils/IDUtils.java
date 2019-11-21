
package edu.ucla.library.iiif.fester.utils;

import static edu.ucla.library.iiif.fester.Constants.MANIFEST;
import static edu.ucla.library.iiif.fester.Constants.SLASH;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * A utilities class for working with IDs.
 */
public final class IDUtils {

    private IDUtils() {
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
