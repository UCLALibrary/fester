
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

import info.freelibrary.util.FileUtils;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MalformedPathException;
import edu.ucla.library.iiif.fester.MessageCodes;

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
     * @throws MalformedPathException If the supplied S3 key doesn't start with {@link Constants.WORK_S3_KEY_PREFIX}
     *         or {@link Constants.COLLECTION_S3_KEY_PREFIX}
     */
    public static String getResourceURIPath(final String aS3Key) throws MalformedPathException {
        final String encodedID = URLEncoder.encode(getResourceID(aS3Key), StandardCharsets.UTF_8);
        final String path;

        checkPrefixValidity(aS3Key);

        if (aS3Key.startsWith(Constants.COLLECTION_S3_KEY_PREFIX)) {
            path = Constants.COLLECTION_URI_PATH_PREFIX + encodedID;
        } else { // it starts with Constants.WORK_S3_KEY_PREFIX
            path = Constants.SLASH + encodedID + Constants.MANIFEST_URI_PATH_SUFFIX;
        }

        return path;
    }

    /**
     * Gets a IIIF resource ID (ARK) from its S3 key.
     *
     * @param aS3Key The S3 key for the IIIF resource
     * @return An ID (ARK)
     * @throws MalformedPathException If the supplied S3 key doesn't start with {@link Constants.WORK_S3_KEY_PREFIX}
     *         or {@link Constants.COLLECTION_S3_KEY_PREFIX}
     */
    public static String getResourceID(final String aS3Key) throws MalformedPathException {
        final String s3KeyPrefix;

        checkPrefixValidity(aS3Key);

        if (aS3Key.startsWith(Constants.COLLECTION_S3_KEY_PREFIX)) {
            s3KeyPrefix = Constants.COLLECTION_S3_KEY_PREFIX;
        } else { // it starts with Constants.WORK_S3_KEY_PREFIX
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
     * @throws MalformedPathException If the supplied URI doesn't contain {@link Constants.MANIFEST_URI_PATH_SUFFIX}
     *         or {@link Constants.COLLECTION_URI_PATH_PREFIX}
     */
    public static String getResourceS3Key(final URI aURI) throws MalformedPathException {
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
        } else if (path.contains(Constants.MANIFEST_URI_PATH_SUFFIX)) {
            uriPathPrefix = Constants.SLASH;
            s3KeyPrefix = Constants.WORK_S3_KEY_PREFIX;
            endIndex = path.length() - Constants.MANIFEST_URI_PATH_SUFFIX.length();
        } else {
            throw new MalformedPathException(MessageCodes.MFS_143, aURI, Constants.MANIFEST_URI_PATH_SUFFIX,
                    Constants.COLLECTION_URI_PATH_PREFIX);
        }

        encodedID = path.substring(uriPathPrefix.length(), endIndex);

        // Concatenate prefix, ID, and a ".json" extension
        return s3KeyPrefix + URLDecoder.decode(encodedID, StandardCharsets.UTF_8) + Constants.DOT +
                Constants.JSON_EXT;
    }

    /**
     * Gets an S3 key for a work.
     *
     * @param aID The ID (ARK) of the work
     * @return An S3 key
     */
    public static String getWorkS3Key(final String aID) {
        final String jsonExtension = Constants.DOT + Constants.JSON_EXT;

        if (aID.startsWith(Constants.WORK_S3_KEY_PREFIX)) {
            throw new MalformedPathException(MessageCodes.MFS_144, aID);
        }

        if (aID.endsWith(jsonExtension)) {
            throw new MalformedPathException(MessageCodes.MFS_145, aID);
        }

        return Constants.WORK_S3_KEY_PREFIX + aID + Constants.DOT + Constants.JSON_EXT;
    }

    /**
     * Gets an S3 key for a collection.
     *
     * @param aID The ID (ARK) of the collection
     * @return An S3 key
     */
    public static String getCollectionS3Key(final String aID) {
        final String jsonExtension = Constants.DOT + Constants.JSON_EXT;

        if (aID.startsWith(Constants.COLLECTION_S3_KEY_PREFIX)) {
            throw new MalformedPathException(MessageCodes.MFS_144, aID);
        }

        if (aID.endsWith(jsonExtension)) {
            throw new MalformedPathException(MessageCodes.MFS_145, aID);
        }

        return Constants.COLLECTION_S3_KEY_PREFIX + aID + jsonExtension;
    }

    /**
     * Checks for a valid S3 key path prefix.
     *
     * @param aS3Key An S3 key
     */
    private static void checkPrefixValidity(final String aS3Key) {
        if (!aS3Key.startsWith(Constants.COLLECTION_S3_KEY_PREFIX) && !aS3Key.startsWith(
                Constants.WORK_S3_KEY_PREFIX)) {
            throw new MalformedPathException(MessageCodes.MFS_142, aS3Key, Constants.WORK_S3_KEY_PREFIX,
                    Constants.COLLECTION_S3_KEY_PREFIX);
        }
    }
}
