
package edu.ucla.library.iiif.fester.utils;

import static com.google.common.base.Preconditions.checkNotNull;
import static edu.ucla.library.iiif.fester.utils.ContainerConfig.S3_ALIAS;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.TestConstants;

/**
 * Some Docker container utilities.
 */
public final class ContainerUtils {

    /* The endpoint host used from within the Fester container */
    public static final String HOST = "http://" + S3_ALIAS + ":{}";

    /* Logger for the container utilities */
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerUtils.class, MessageCodes.BUNDLE);

    private ContainerUtils() {
        // This is intentionally left empty
    }

    /**
     * Gets the Fester container.
     *
     * @param aConfig A container configuration
     * @return The Fester container
     */
    public static GenericContainer<?> getFesterContainer(final ContainerConfig aConfig) {
        final String containerTag = toTag(System.getProperty(TestConstants.CONTAINER_IMAGE));
        final GenericContainer<?> container = new GenericContainer<>(containerTag);
        final String placeholder = checkNotNull(System.getProperty(Config.PLACEHOLDER_IMAGE, Constants.EMPTY));
        final String festerizeVersion = checkNotNull(System.getProperty(Config.FESTERIZE_VERSION, Constants.EMPTY));
        final String accessKey = checkNotNull(System.getProperty(Config.S3_ACCESS_KEY, aConfig.getS3AccessKey()));
        final String secretKey = checkNotNull(System.getProperty(Config.S3_SECRET_KEY, aConfig.getS3SecretKey()));
        final String endpoint = System.getProperty(Config.S3_ENDPOINT, StringUtils.format(HOST, 4566)); // aConfig.getS3Port()
        final String region = checkNotNull(System.getProperty(Config.S3_REGION, aConfig.getS3Region()));
        final String bucket = checkNotNull(System.getProperty(Config.S3_BUCKET));
        final String featureFlagsURL = System.getProperty(Config.FEATURE_FLAGS);
        final String featureFlags = featureFlagsURL == null ? Config.FEATURE_FLAGS : toEnv(Config.FEATURE_FLAGS);
        final String containerPort = checkNotNull(Integer.toString(aConfig.getContainerPort()));
        final Map<String, String> envMap = Map.of(toEnv(Config.S3_ACCESS_KEY), accessKey, toEnv(Config.S3_SECRET_KEY),
                secretKey, toEnv(Config.S3_REGION), region, toEnv(Config.S3_ENDPOINT), endpoint,
                toEnv(Config.HTTP_PORT), containerPort, toEnv(Config.S3_BUCKET), bucket, featureFlags,
                featureFlagsURL != null ? featureFlagsURL : Constants.EMPTY, toEnv(Config.PLACEHOLDER_IMAGE),
                placeholder, toEnv(Config.FESTERIZE_VERSION), festerizeVersion);
        final String jdwpHostPort = System.getProperty(Config.JDWP_HOST_PORT);

        // Check to see if we want to output our Fester container logs when we run; the default is "no"
        if (Boolean.parseBoolean(System.getProperty(Config.LOGS_ON, Boolean.FALSE.toString()))) {
            container.withLogConsumer(frame -> {
                if (frame.getType() == OutputFrame.OutputType.STDOUT ||
                        frame.getType() == OutputFrame.OutputType.STDERR) {
                    LOGGER.info("[FESTER] " + frame.getUtf8String().trim());
                }
            });
        }

        if (!Constants.EMPTY.equals(jdwpHostPort) && jdwpHostPort != null) {
            // Allow our container to attach to a JDWP server running on the host machine
            Testcontainers.exposeHostPorts(Integer.parseInt(jdwpHostPort));
        }

        container.withExposedPorts(aConfig.getContainerPort()).withEnv(envMap).withNetwork(Network.SHARED);
        container.waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

        container.start();
        return container;
    }

    /**
     * Gets a local S3-compatible container.
     *
     * @return A local S3-compatible container
     */
    public static LocalStackContainer getS3Container() {
        final DockerImageName localstackImage = DockerImageName.parse("localstack/localstack");
        final LocalStackContainer s3Container = new LocalStackContainer(localstackImage);

        s3Container.withServices(Service.S3).withNetwork(Network.SHARED).withNetworkAliases(S3_ALIAS).start();

        return s3Container;
    }

    /**
     * Replaces a SNAPSHOT version with 'latest' for the Docker image tag.
     *
     * @param aVersion A artifact version
     * @return A Docker image tag
     */
    public static String toTag(final String aVersion) {
        final String version = aVersion != null ? aVersion : "fester:0.0.0-SNAPSHOT";

        if (version.contains("-SNAPSHOT")) {
            final StringBuilder builder = new StringBuilder(version);
            final int index = builder.lastIndexOf(":");

            if (index != -1) {
                builder.replace(index + 1, builder.length(), "latest");
                return builder.toString();
            } else {
                return version;
            }
        } else {
            return version;
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
