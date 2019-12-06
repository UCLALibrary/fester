
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
     * The name of the manifest ID parameter.
     */
    public static final String MANIFEST_ID = "manifestId";

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
     * Just a empty string, useful
     */
    public static final String EMPTY = "";

    /**
     * The file extension for JSON files
     */
    public static final String JSON_EXT = ".json";

    /**
     * The file path of the CSV collection file being processed.
     */
    public static final String CSV_FILE_PATH = "csv-file-path";

    /**
     * The name of the CSV collection file being processed.
     */
    public static final String CSV_FILE_NAME = "csv-file-name";

    /**
     * The path at which we can find the collection manifests.
     */
    public static final String COLLECTIONS_PATH = "/collections";

    /**
     * The host at which we're serving content.
     */
    public static final String FESTER_HOST = "fester-host";

    /**
     * The path at which we can find the collection manifests.
     */
    public static final String COLLECTIONS_PATH = "collections-path";

    /**
     * The path that distinguishes a work manifest at which work manifests.
     */
    public static final String MANIFEST = "manifest";

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
     * Private constructor for Constants class.
     */
    private Constants() {
    }

}
