
package edu.ucla.library.iiif.fester;

/**
 * A set of HTTP related constants.
 */
public final class HTTP {

    /** Success response */
    public static final int OK = 200;

    /** Success with no content */
    public static final int SUCCESS_NO_CONTENT = 204;

    /** Created response */
    public static final int CREATED = 201;

    /** Bad request */
    public static final int BAD_REQUEST = 400;

    /** Permission denied */
    public static final int FORBIDDEN = 403;

    /** Not found response */
    public static final int NOT_FOUND = 404;

    /** Method not allowed */
    public static final int METHOD_NOT_ALLOWED = 405;

    /** An empty or other unsupported media type */
    public static final int UNSUPPORTED_MEDIA_TYPE = 415;

    /** Generic internal server error */
    public static final int INTERNAL_SERVER_ERROR = 500;

    /**
     * A private constructor for the constants class.
     */
    private HTTP() {
    }
}
