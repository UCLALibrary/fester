
package edu.ucla.library.iiif.fester.utils;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;

/**
 * A configuration so we don't have to pass a gazillion parameters.
 */
public class ContainerConfig {

    /* A network alias for the LocalStack S3 service */
    public static final String S3_ALIAS = "s3.localstack";

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerConfig.class, Constants.MESSAGES);

    /* A name for our test container */
    private final String myContainerName;

    /* An expected port for our container */
    private final int myContainerPort;

    /* An S3 access key for our back-end */
    private String myAccessKey;

    /* An S3 access key for our back-end */
    private String mySecretKey;

    /* An S3 region for our back-end */
    private String myRegion;

    /* A port for our S3 back-end */
    private int myExposedS3Port;

    /* An endpoint configuration for an S3 client */
    private EndpointConfiguration myEndpointConfig;

    /**
     * Creates a new configuration.
     *
     * @param aContainerName A container name
     * @param aContainerPort A container port
     */
    public ContainerConfig(final String aContainerName, final int aContainerPort) {
        myContainerName = aContainerName;
        myContainerPort = aContainerPort;
        myExposedS3Port = 80;
    }

    /**
     * Creates a new configuration.
     *
     * @param aContainerName A container name
     * @param aContainerPort A container port
     * @param aS3Container A container that provides a test S3 environment
     */
    public ContainerConfig(final String aContainerName, final int aContainerPort,
            final LocalStackContainer aS3Container) {
        final AWSCredentialsProvider credentialsProvider = aS3Container.getDefaultCredentialsProvider();
        final AWSCredentials credentials = credentialsProvider.getCredentials();

        myEndpointConfig = aS3Container.getEndpointConfiguration(Service.S3);
        myContainerName = aContainerName;
        myContainerPort = aContainerPort;
        myAccessKey = credentials.getAWSAccessKeyId();
        mySecretKey = credentials.getAWSSecretKey();
        myRegion = myEndpointConfig.getSigningRegion();
        myExposedS3Port = aS3Container.getExposedPorts().get(0);

        // This is the endpoint that the S3 client uses when setting up test resources
        LOGGER.debug(MessageCodes.MFS_079, myEndpointConfig.getServiceEndpoint());
    }

    /**
     * Gets the container name.
     *
     * @return A container name
     */
    public String getContainerName() {
        return myContainerName;
    }

    /**
     * Gets the container port.
     *
     * @return A container port
     */
    public int getContainerPort() {
        return myContainerPort;
    }

    /**
     * Sets the S3 region for our service.
     *
     * @param aRegion An S3 region in string form
     * @return The configuration
     */
    public ContainerConfig setS3Region(final String aRegion) {
        myRegion = aRegion;
        return this;
    }

    /**
     * Gets the S3 region.
     *
     * @return An S3 region in string form
     */
    public String getS3Region() {
        return myRegion;
    }

    /**
     * Sets the S3 port.
     *
     * @param aPort A port at which S3 can be reached
     * @return The container configuration
     */
    public ContainerConfig setS3Port(final int aPort) {
        myExposedS3Port = aPort;
        return this;
    }

    /**
     * Gets the S3 port.
     *
     * @return The port at which S3 can be reached
     */
    public int getS3Port() {
        return myExposedS3Port;
    }

    /**
     * Sets the S3 access key for our service.
     *
     * @param aAccessKey An S3 access key
     * @return The configuration
     */
    public ContainerConfig setS3AccessKey(final String aAccessKey) {
        myAccessKey = aAccessKey;
        return this;
    }

    /**
     * Gets the S3 access key.
     *
     * @return An S3 access key in string form
     */
    public String getS3AccessKey() {
        return myAccessKey;
    }

    /**
     * Sets the S3 secret key for our service.
     *
     * @param aSecretKey An S3 secret key in string form
     * @return The configuration
     */
    public ContainerConfig setS3SecretKey(final String aSecretKey) {
        mySecretKey = aSecretKey;
        return this;
    }

    /**
     * Gets the S3 secret key.
     *
     * @return An S3 secret key in string form
     */
    public String getS3SecretKey() {
        return mySecretKey;
    }

    /**
     * Returns whether the container configuration includes an S3 configuration.
     *
     * @return True if there is an S3 configuration; else, false
     */
    public boolean hasS3Config() {
        return myAccessKey != null && mySecretKey != null && myRegion != null;
    }

    /**
     * Gets the endpoint configuration for an S3 client.
     *
     * @return An endpoint configuration for an S3 client
     */
    public EndpointConfiguration getEndpointConfiguration() {
        if (hasS3Config()) {
            return myEndpointConfig;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Gets an AWS credentials provider.
     *
     * @return An AWS credentials provider
     * @throws UnsupportedOperationException If no S3 container is configured
     */
    public AWSCredentialsProvider getAwsCredsProvider() {
        if (hasS3Config()) {
            return new AWSStaticCredentialsProvider(new BasicAWSCredentials(myAccessKey, mySecretKey));
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
