
package edu.ucla.library.iiif.manifeststore;

/**
 * A class for package constants.
 */
public final class Constants {

    /**
     * ResourceBundle file name for I18n messages.
     */
    public static final String MESSAGES = "manifeststore_messages";

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
     * Private constructor for Constants class.
     */
    private Constants() {
    }

}
