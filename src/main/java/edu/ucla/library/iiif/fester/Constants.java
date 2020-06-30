
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
     * The default S3 endpoint.
     */
    public static final String S3_ENDPOINT = "https://s3.amazonaws.com";

    /**
     * The message header key associated with the Fester operation (HTTP request) that caused the message send.
     */
    public static final String ACTION = "action";

    /**
     * The message body key associated with the Fester operation input data (HTTP request body) that caused the
     * message send.
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
     * The plain text content-type.
     */
    public static final String PLAIN_TEXT_TYPE = "text/plain";

    /**
     * COR header for allowing access to our manifests to the world.
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
     * Just a empty string, useful.
     */
    public static final String EMPTY = "";

    /**
     * A regular expression representing end of line character(s).
     */
    public static final String EOL_REGEX = "\\r|\\n|\\r\\n";

    /**
     * The file extension for JSON files.
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
     * Used as a message header when the sender wants the manifest returned from S3 without resource URLs re-written.
     */
    public static final String NO_REWRITE_URLS = "no-rewrite-urls";

    /**
     * A unique random placeholder URL that prefixes all IIIF Presentation API resource URLs in all manifests at rest
     * in S3. It gets replaced with Constants.URL on each GET request.
     */
    public static final String URL_PLACEHOLDER = "http://b1dbe4a0-443c-479f-bf0a-25c352df0d8f.iiif.library.ucla.edu";

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
     * Manifest content, stored as a JSON object.
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
     * The label that should be displayed along with the repository name in the manifest metadata.
     */
    public static final String REPOSITORY_NAME_METADATA_LABEL = "Repository";

    /**
     * The label that should be displayed along with the rights contact in the manifest metadata.
     */
    public static final String RIGHTS_CONTACT_METADATA_LABEL = "Rights contact";

    /**
     * Private constructor for Constants class.
     */
    private Constants() {
    }

}
