
package edu.ucla.library.iiif.fester.fit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.testcontainers.containers.GenericContainer;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.utils.ContainerConfig;
import edu.ucla.library.iiif.fester.utils.ContainerUtils;

/**
 * An abstract integration test class that starts up the Docker container for all tests that extend this class to use.
 */
public class BaseFesterIT extends AbstractFesterFIT {

    /* The name of the application we're testing and the type of tests; we use this class name minus "Abstract" */
    private static final String SERVICE_NAME = BaseFesterIT.class.getSimpleName().substring(8);

    /* This is the port the container expects, but a random one will be the one exposed */
    private static final int SERVICE_PORT = Integer.parseInt(System.getProperty(Config.HTTP_PORT));

    private static final ContainerConfig FESTER_CONFIG = new ContainerConfig(SERVICE_NAME, SERVICE_PORT);

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

        final String s3AccessKey = System.getProperty(Config.S3_ACCESS_KEY);
        final String s3SecretKey = System.getProperty(Config.S3_SECRET_KEY);
        final String s3Region = System.getProperty(Config.S3_REGION);
        final AWSCredentials s3Credentials = new BasicAWSCredentials(s3AccessKey, s3SecretKey);
        final AWSCredentialsProvider s3CredentialsProvider = new AWSStaticCredentialsProvider(s3Credentials);
        final AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard();
        final EndpointConfiguration s3Endpoint = new EndpointConfiguration(Constants.S3_ENDPOINT, s3Region);

        s3ClientBuilder.withEndpointConfiguration(s3Endpoint).withCredentials(s3CredentialsProvider);
        s3ClientBuilder.withClientConfiguration(new ClientConfiguration());

        myS3Client = s3ClientBuilder.build();
    }

}
