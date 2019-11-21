
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
     * Private constructor for Constants class.
     */
    private Constants() {
    }

}
