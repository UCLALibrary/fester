
package edu.ucla.library.iiif.fester;

/**
 * A class for package constants.
 */
public final class Constants {

    /**
     * ResourceBundle file name for I18n messages.
     */
    public static final String MESSAGES = "fester_messages";

    /**
     * The IP for an unspecified host.
     */
    public static final String UNSPECIFIED_HOST = "0.0.0.0";

    /**
     * The message header key associated with the Fester operation (HTTP request) that caused the message send.
     */
    public static final String ACTION = "action";

    /**
     * The message body key associated with the Fester operation input data (HTTP request body) that caused the message
     * send.
     */
    public static final String DATA = "data";

    /**
     * The name of the manifest ID parameter.
     */
    public static final String MANIFEST_ID = "manifestId";

    /**
     * The name of the collection name parameter.
     */
    public static final String COLLECTION_NAME = "collectionName";

    /**
     * The content-type header key.
     */
    public static final String CONTENT_TYPE = "content-type";

    /**
     * The media type for JSON (the format of IIIF manifests).
     */
    public static final String JSON_MEDIA_TYPE = "application/json";

    /**
     * The media type for HTML.
     */
    public static final String HTML_MEDIA_TYPE = "text/html";

    /**
     * The media type for CSV.
     */
    public static final String CSV_MEDIA_TYPE = "text/csv";

    /**
     * COR header for allowing access to our manifests to the world
     */
    public static final String CORS_HEADER = "Access-Control-Allow-Origin";

    /**
     * An asterisk/star for use as a wildcard.
     */
    public static final String STAR = "*";

    /**
     * A slash for constructing URL paths.
     */
    public static final String SLASH = "/";

    /**
     * A dot.
     */
    public static final String DOT = ".";

    /**
     * Just a empty string, useful
     */
    public static final String EMPTY = "";

    /**
     * The file extension for JSON files
     */
    public static final String JSON_EXT = "json";

    /**
     * The file path of the CSV collection file being processed.
     */
    public static final String CSV_FILE_PATH = "csv-file-path";

    /**
     * The name of the CSV collection file being processed.
     */
    public static final String CSV_FILE_NAME = "csv-file-name";

    /**
     * The name of the IIIF host parameter.
     */
    public static final String IIIF_HOST = "iiif-host";

    /**
     * The HTTP POST route for CSV uploads.
     */
    public static final String POST_CSV_ROUTE = "/collections";

    /**
     * The host at which we're serving content.
     */
    public static final String FESTER_HOST = "fester-host";

    /**
     * The suffix for work manifest URI paths.
     */
    public static final String MANIFEST_URI_PATH_SUFFIX = "/manifest";

    /**
     * The prefix for work manifest S3 keys.
     */
    public static final String WORK_S3_KEY_PREFIX = "works/";

    /**
     * The prefix for collection manifest S3 keys.
     */
    public static final String COLLECTION_S3_KEY_PREFIX = "collections/";

    /**
     * The prefix for collection manifest URI paths.
     */
    public static final String COLLECTION_URI_PATH_PREFIX = SLASH + COLLECTION_S3_KEY_PREFIX;

    /**
     * The record of completed S3 uploads.
     */
    public static final String RESULTS_MAP = "s3-uploads";

    /**
     * A name for wait counters.
     */
    public static final String WAIT_COUNT = "wait-count";

    /**
     * A name for the S3 request counter.
     */
    public static final String S3_REQUEST_COUNT = "s3-request-count";

    /**
     * Manifest content, stored as a JSON object
     */
    public static final String MANIFEST_CONTENT = "manifest-content";

    /**
     * The POST parameter for uploading a CSV file.
     */
    public static final String CSV_FILE = "csv-file";

    /**
     * The name of the mapping of deployed verticles.
     */
    public static final String VERTICLE_MAP = "fester-verticles";

    /**
     * The ID property in a manifest (collection or work).
     */
    public static final String ID = "@id";

    /**
     * Private constructor for Constants class.
     */
    private Constants() {
    }

}
