
package edu.ucla.library.iiif.fester.fit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.utils.ContainerConfig;
import edu.ucla.library.iiif.fester.utils.ContainerUtils;

/**
 * An abstract functional test class that starts up the Docker container for all tests that extend this class to use.
 */
public class BaseFesterFT extends AbstractFesterFIT {

    /* The name of the application we're testing and the type of tests; we use this class name minus "Abstract" */
    private static final String SERVICE_NAME = BaseFesterFT.class.getSimpleName().substring(8);

    /* This is the port the container expects, but a random one will be the one exposed */
    private static final int SERVICE_PORT = Integer.parseInt(System.getProperty(Config.HTTP_PORT));

    /* Start up an S3 LocalStack equivalent that we can test against */
    private static final LocalStackContainer S3 = ContainerUtils.getS3Container();

    /* Get a container configuration for Fester */
    private static final ContainerConfig FESTER_CONFIG = new ContainerConfig(SERVICE_NAME, SERVICE_PORT, S3);

    /* The application's singleton container against which we're going to run our tests */
    private static final GenericContainer<?> FESTER_CONTAINER = ContainerUtils.getFesterContainer(FESTER_CONFIG);

    /**
     * Sets up our testing environment.
     */
    @BeforeClass
    public static void setUpTestEnv() {
        FESTER_PORT = FESTER_CONTAINER.getFirstMappedPort();
    }

    @Override
    @Before
    public void setUpTest() {
        super.setUpTest();

        final AWSCredentialsProvider s3Credentials = FESTER_CONFIG.getAwsCredsProvider();
        final AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard();
        final EndpointConfiguration endpoint = new EndpointConfiguration(FESTER_CONFIG.getS3Endpoint(), "us-east-1");

        s3ClientBuilder.withEndpointConfiguration(endpoint).withCredentials(s3Credentials);
        s3ClientBuilder.withClientConfiguration(new ClientConfiguration().withProtocol(Protocol.HTTP));

        myS3Client = s3ClientBuilder.withPathStyleAccessEnabled(true).build();
    }

}
