
package edu.ucla.library.iiif.manifeststore;

/**
 * Some constant values for the project.
 */
public final class Config {

    // The HTTP port configuration parameter
    public static final String HTTP_PORT = "http.port";

    // The OpenAPI specification configuration parameter
    public static final String OPENAPI_SPEC_PATH = "openapi.spec.path";

    public static final String S3_ACCESS_KEY = "manifeststore.s3.access_key";

    public static final String S3_SECRET_KEY = "manifeststore.s3.secret_key";

    public static final String S3_REGION = "manifeststore.s3.region";

    public static final String S3_BUCKET = "manifeststore.s3.bucket";

    /**
     * Private constructor for the Constants class.
     */
    private Config() {
    }

}
