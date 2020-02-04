
package edu.ucla.library.iiif.fester.utils;

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
import info.freelibrary.util.FileUtils;

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
     * Gets a IIIF resource URI from its S3 key.
     *
     * @param aHost The host at which Fester is running
     * @param aS3Key The S3 key for the IIIF resource
     * @return A IIIF resource URI
     */
    public static URI getResourceURI(final String aHost, final String aS3Key) {
        return URI.create(new StringBuilder().append(aHost).append(getResourceURIPath(aS3Key)).toString());
    }

    /**
     * Gets the path part of a IIIF resource URI from its S3 key.
     *
     * @param aS3Key The S3 key for the IIIF resource
     * @return A IIIF resource URI path
     */
    public static String getResourceURIPath(final String aS3Key) {
        final String encodedID = URLEncoder.encode(getResourceID(aS3Key), StandardCharsets.UTF_8);
        final String path;

        if (aS3Key.contains(Constants.COLLECTION_S3_KEY_PREFIX)) {
            path = Constants.COLLECTION_URI_PATH_PREFIX + encodedID;
        } else {
            // TODO: check (aS3Key.contains(Constants.WORK_S3_KEY_PREFIX))
            path = Constants.SLASH + encodedID + Constants.MANIFEST_URI_PATH_SUFFIX;
        }
        return path;
    }

    /**
     * Gets a IIIF resource ID (ARK) from its S3 key.
     *
     * @param aS3Key The S3 key for the IIIF resource
     * @return An ID (ARK)
     */
    public static String getResourceID(final String aS3Key) {
        final String s3KeyPrefix;
        if (aS3Key.contains(Constants.COLLECTION_S3_KEY_PREFIX)) {
            s3KeyPrefix = Constants.COLLECTION_S3_KEY_PREFIX;
        } else {
            // TODO: check (aS3Key.contains(Constants.WORK_S3_KEY_PREFIX))
            s3KeyPrefix = Constants.WORK_S3_KEY_PREFIX;
        }
        return FileUtils.stripExt(aS3Key.substring(s3KeyPrefix.length()));
    }

    /**
     * Gets a IIIF resource ID (ARK) from its URI.
     *
     * @param aURI The URI of the IIIF resource
     * @return An ID (ARK)
     */
    public static String getResourceID(final URI aURI) {
        return getResourceID(getResourceS3Key(aURI));
    }

    /**
     * Gets a IIIF resource's S3 key from its URI.
     *
     * @param aURI The URI of the IIIF resource
     * @return An S3 key
     */
    public static String getResourceS3Key(final URI aURI) {
        final String path = aURI.getPath();
        final String encodedID;
        final String uriPathPrefix;
        final String s3KeyPrefix;
        final int endIndex;

        // Get the (encoded) resource ID from the URI path
        if (path.contains(Constants.COLLECTION_URI_PATH_PREFIX)) {
            uriPathPrefix = Constants.COLLECTION_URI_PATH_PREFIX;
            s3KeyPrefix = Constants.COLLECTION_S3_KEY_PREFIX;
            endIndex = path.length();
        } else {
            // TODO: check (path.contains(Constants.MANIFEST_URI_PATH_SUFFIX))
            uriPathPrefix = Constants.SLASH;
            s3KeyPrefix = Constants.WORK_S3_KEY_PREFIX;
            endIndex = path.length() - Constants.MANIFEST_URI_PATH_SUFFIX.length();
        }
        encodedID = path.substring(uriPathPrefix.length(), endIndex);

        // Add any prefix and a .json extension
        return s3KeyPrefix + URLDecoder.decode(encodedID, StandardCharsets.UTF_8) + Constants.DOT + Constants.JSON_EXT;
    }

    /**
     * Gets an S3 key for a work.
     *
     * @param aID The ID (ARK) of the work
     * @return An S3 key
     */
    public static String getWorkS3Key(final String aID) {
        return Constants.WORK_S3_KEY_PREFIX + aID + Constants.DOT + Constants.JSON_EXT;
    }

    /**
     * @deprecated Gets an S3 key for a work, with an extension to append if the identifier doesn't have it already.
     *
     * TODO: remove in 1.0.0
     *
     * @param aID The ID (ARK) of the work
     * @param aExt The extension to append if the identifier doesn't have it already
     * @return An S3 key
     */
    @Deprecated
    public static String getWorkS3Key(final String aID, final String aExt) {
        if (FileUtils.getExt(aID).equals(aExt)) {
            return Constants.WORK_S3_KEY_PREFIX + aID;
        } else {
            return Constants.WORK_S3_KEY_PREFIX + aID + Constants.DOT + aExt;
        }
    }

    /**
     * Gets an S3 key for a collection.
     *
     * @param aID The ID (ARK) of the collection
     * @return An S3 key
     */
    public static String getCollectionS3Key(final String aID) {
        return Constants.COLLECTION_S3_KEY_PREFIX + aID + Constants.DOT + Constants.JSON_EXT;
    }
}
