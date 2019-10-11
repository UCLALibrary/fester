
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
     * an asterisk/star, comes in handy
     */
    public static final String STAR = "*";

    /**
     * The file extension for JSON files
     */
    public static final String JSON_EXT = ".json";

    /**
     * ID Prefix to use for all PUT tests
     */
    public static final String PUT_TEST_ID_PREFIX = "PUT_";

    /**
     * Private constructor for Constants class.
     */
    private Constants() {
    }

}
