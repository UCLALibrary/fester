
package edu.ucla.library.iiif.fester.utils;

import static edu.ucla.library.iiif.fester.utils.ContainerConfig.S3_ALIAS;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.TestConstants;

/**
 * Some Docker container utilities.
 */
public final class ContainerUtils {

    /* The S3 LocalStack and Fester containers use the same network */
    public static final Network NETWORK = Network.newNetwork();

    /* The endpoint host used from within the Fester container */
    public static final String HOST = "http://" + S3_ALIAS + ":{}";

    private ContainerUtils() {
    }

    /**
     * Gets the Fester container.
     *
     * @param aConfig A container configuration
     * @return The Fester container
     */
    public static GenericContainer getFesterContainer(final ContainerConfig aConfig) {
        final String containerTag = toTag(System.getProperty(TestConstants.CONTAINER_IMAGE));
        final GenericContainer<?> container = new GenericContainer(containerTag);
        final String accessKey = System.getProperty(Config.S3_ACCESS_KEY, aConfig.getS3AccessKey());
        final String secretKey = System.getProperty(Config.S3_SECRET_KEY, aConfig.getS3SecretKey());
        final String endpoint = System.getProperty(Config.S3_ENDPOINT, StringUtils.format(HOST, aConfig.getS3Port()));
        final String region = System.getProperty(Config.S3_REGION, aConfig.getS3Region());
        final String bucket = System.getProperty(Config.S3_BUCKET);
        final String featureFlagsURL = System.getProperty(Config.FEATURE_FLAGS);
        final String featureFlags = featureFlagsURL == null ? Config.FEATURE_FLAGS : toEnv(Config.FEATURE_FLAGS);
        final Map<String, String> envMap = Map.of(toEnv(Config.S3_ACCESS_KEY), accessKey, toEnv(Config.S3_SECRET_KEY),
                secretKey, toEnv(Config.S3_REGION), region, toEnv(Config.S3_ENDPOINT), endpoint, toEnv(
                        Config.HTTP_PORT), Integer.toString(aConfig.getContainerPort()), toEnv(Config.S3_BUCKET),
                bucket, featureFlags, featureFlagsURL);

        // Check to see if we want to output our Fester container logs when we run; the default is "no"
        if (Boolean.parseBoolean(System.getProperty(Config.LOGS_ON, Boolean.FALSE.toString()))) {
            container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(aConfig.getContainerName())));
        }

        container.withExposedPorts(aConfig.getContainerPort()).withEnv(envMap).withNetwork(NETWORK).start();

        return container;
    }

    /**
     * Gets a local S3-compatible container.
     *
     * @return A local S3-compatible container
     */
    public static LocalStackContainer getS3Container() {
        final LocalStackContainer s3Container = new LocalStackContainer();

        s3Container.withServices(Service.S3).withNetwork(NETWORK).withNetworkAliases(S3_ALIAS).start();

        return s3Container;
    }

    /**
     * Replaces a SNAPSHOT version with 'latest' for the Docker image tag.
     *
     * @param aVersion A artifact version
     * @return A Docker image tag
     */
    public static String toTag(final String aVersion) {
        Objects.requireNonNullElse(aVersion, "(null)");

        if (aVersion.contains("-SNAPSHOT")) {
            final StringBuilder builder = new StringBuilder(aVersion);
            final int index = builder.lastIndexOf(":");

            if (index != -1) {
                builder.replace(index + 1, builder.length(), "latest");
                return builder.toString();
            } else {
                return aVersion;
            }
        } else {
            return aVersion;
        }
    }

    /**
     * Converts a system property name to an environmental variable.
     *
     * @param aPropertyName A system property name
     * @return The environmental property
     */
    private static String toEnv(final String aPropertyName) {
        return aPropertyName.replace('.', '_').toUpperCase(Locale.US);
    }
}
