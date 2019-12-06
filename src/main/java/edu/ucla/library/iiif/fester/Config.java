
package edu.ucla.library.iiif.fester;

/**
 * Some constant values for the project.
 */
public final class Config {

    // The HTTP port configuration parameter
    public static final String HTTP_PORT = "fester.http.port";

    // The OpenAPI specification configuration parameter
    public static final String OPENAPI_SPEC_PATH = "openapi.spec.path";

    public static final String S3_ACCESS_KEY = "fester.s3.access_key";

    public static final String S3_SECRET_KEY = "fester.s3.secret_key";

    public static final String S3_REGION = "fester.s3.region";

    public static final String S3_BUCKET = "fester.s3.bucket";

    /**
     * Private constructor for the Constants class.
     */
    private Config() {
    }

}
