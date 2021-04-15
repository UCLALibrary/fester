
package edu.ucla.library.iiif.fester;

/**
 * Some constant values for the project.
 */
public final class Config {

    /* Configuration parameter for the URL Fester is available at */
    public static final String FESTER_URL = "fester.url";

    /* The HTTP port configuration parameter */
    public static final String HTTP_PORT = "fester.http.port";

    /* The OpenAPI specification configuration parameter */
    public static final String OPENAPI_SPEC_PATH = "openapi.spec.path";

    /* The AWS S3 access key */
    public static final String S3_ACCESS_KEY = "fester.s3.access_key";

    /* The AWS S3 secret key */
    public static final String S3_SECRET_KEY = "fester.s3.secret_key";

    /* The AWS S3 region used for signing communications */
    public static final String S3_REGION = "fester.s3.region";

    /* The AWS S3 bucket into which manifests and collection docs are put */
    public static final String S3_BUCKET = "fester.s3.bucket";

    /* The base IIIF server URL that is used to get width and height for canvases */
    public static final String IIIF_BASE_URL = "iiif.base.url";

    /* The placeholder image URL, used for images that are missing */
    public static final String PLACEHOLDER_IMAGE = "fester.placeholder.url";

    /* The S3 endpoint that's used when storing and retrieving manifests */
    public static final String S3_ENDPOINT = "fester.s3.endpoint";

    /* Config property for turning logs on while running in test mode */
    public static final String LOGS_ON = "fester.logs.output";

    /* A feature flags configuration for the Docker container */
    public static final String FEATURE_FLAGS = "feature.flags";

    /* The port a debugger may listen on to debug a containerized Fester instance */
    public static final String JDWP_HOST_PORT = "jdwp.host.port";

    /* The version of Festerize that is compatible with this version of Fester */
    public static final String FESTERIZE_VERSION = "festerize.version";

    /* A string to distinguish A/V access URLs from generic IIF access URLs */
    public static final String AV_URL_STRING = "fester.av.string";

    /**
     * Private constructor for the Constants class.
     */
    private Config() {
    }

}
