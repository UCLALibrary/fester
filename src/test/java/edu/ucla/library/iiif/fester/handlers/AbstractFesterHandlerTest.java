
package edu.ucla.library.iiif.fester.handlers;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.verticles.FakeS3BucketVerticle;
import edu.ucla.library.iiif.fester.verticles.MainVerticle;
import edu.ucla.library.iiif.fester.verticles.S3BucketVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
abstract class AbstractFesterHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFesterHandlerTest.class,
            Constants.MESSAGES);

    private static final String MANIFEST_FILE_NAME = "testManifest.json";

    private static final String IIIF_URL = "http://0.0.0.0";

    protected static final File MANIFEST_FILE = new File("src/test/resources", MANIFEST_FILE_NAME);

    protected Vertx myVertx;

    protected AmazonS3 myS3Client;

    protected String myS3Bucket;

    protected String myManifestID;

    protected String myJsonlessManifestID;

    /**
     * Test set up.
     *
     * @param aContext A testing context
     */
    @Before
    @SuppressWarnings({ "deprecation" })
    public void setUp(final TestContext aContext) throws IOException {
        final DeploymentOptions options = new DeploymentOptions();
        final ServerSocket socket = new ServerSocket(0);
        final int port = socket.getLocalPort();
        final Future<AsyncResult> future = Future.future();
        final Async asyncResult = aContext.async();

        LOGGER.debug(MessageCodes.MFS_002, port);

        aContext.put(Config.HTTP_PORT, port);
        options.setConfig(new JsonObject().put(Config.HTTP_PORT, port).put(Config.IIIF_BASE_URL, IIIF_URL));
        socket.close();

        myJsonlessManifestID = UUID.randomUUID().toString();
        myManifestID = myJsonlessManifestID + ".json";

        // We only need to initialize our testing tools once; if done, skip
        if (myVertx == null) {
            initialize(future);
        } else {
            future.complete();
        }

        // If our testing tools have been initialized, start up our Fester
        future.setHandler(initialization -> {
            if (initialization.succeeded()) {
                deployFester(aContext, asyncResult, options);
            } else if (initialization.cause() != null) {
                aContext.fail(initialization.cause());
            } else {
                aContext.fail();
            }
        });
    }

    /**
     * Test tear down.
     *
     * @param aContext A testing context
     */
    @After
    public void tearDown(final TestContext aContext) {
        try {
            // If object doesn't exist, this still completes successfully
            myS3Client.deleteObject(myS3Bucket, myManifestID);
        } catch (final SdkClientException details) {
            aContext.fail(details);
        }

        try {
            // If object doesn't exist, this still completes successfully
            myS3Client.deleteObject(myS3Bucket, myJsonlessManifestID);
        } catch (final SdkClientException details) {
            aContext.fail(details);
        }

        myVertx.close(aContext.asyncAssertSuccess());
    }

    /**
     * Deploy Fester to test against.
     *
     * @param aContext A test context
     * @param aAsyncTask An asynchronous task that completes the setup
     * @param aOpts Deployment options used to configure Fester
     */
    private void deployFester(final TestContext aContext, final Async aAsyncTask, final DeploymentOptions aOpts) {
        myVertx.deployVerticle(MainVerticle.class.getName(), aOpts, deployment -> {
            if (deployment.succeeded()) {
                final LocalMap<String, String> map = myVertx.sharedData().getLocalMap(Constants.VERTICLE_MAP);
                final String s3BucketDeploymentId = map.get(S3BucketVerticle.class.getSimpleName());

                // Our older handlers talk to S3 directly so we need to put some test files there
                try {
                    // Store a manifest whose ID that has a '.json' extension
                    LOGGER.debug(MessageCodes.MFS_006, myManifestID, myS3Bucket);
                    myS3Client.putObject(myS3Bucket, myManifestID, MANIFEST_FILE);

                    // Store a manifest whose ID that doesn't have a '.json' extension
                    LOGGER.debug(MessageCodes.MFS_006, myJsonlessManifestID, myS3Bucket);
                    myS3Client.putObject(myS3Bucket, myJsonlessManifestID, MANIFEST_FILE);

                    // We don't need to use the real S3BucketVerticle for our non-S3BucketVerticle tests though
                    myVertx.undeploy(s3BucketDeploymentId, undeployment -> {
                        if (undeployment.succeeded()) {
                            myVertx.deployVerticle(FakeS3BucketVerticle.class.getName(), fakeS3VerticleDeployment -> {
                                if (fakeS3VerticleDeployment.succeeded()) {
                                    aAsyncTask.complete();
                                } else {
                                    aContext.fail(fakeS3VerticleDeployment.cause());
                                }
                            });
                        } else {
                            aContext.fail(undeployment.cause());
                        }
                    });
                } catch (final SdkClientException details) {
                    aContext.fail(details);
                }
            } else {
                aContext.fail(deployment.cause());
            }
        });
    }

    /**
     * Initialize our testing tools.
     *
     * @param aFuture A future to capture when the initialization is completed
     * @throws IOException If there is trouble reading from the configuration file
     */
    @SuppressWarnings({ "deprecation" })
    private void initialize(final Future aFuture) throws IOException {
        final ConfigRetriever configRetriever;

        myVertx = Vertx.vertx();
        configRetriever = ConfigRetriever.create(myVertx);

        // We pull our application's configuration in for the S3 client configuration
        configRetriever.getConfig(configuration -> {
            if (configuration.failed()) {
                aFuture.fail(configuration.cause());
            } else {
                final JsonObject config = configuration.result();

                final String s3AccessKey = config.getString(Config.S3_ACCESS_KEY);
                final String s3SecretKey = config.getString(Config.S3_SECRET_KEY);
                final String s3Region = config.getString(Config.S3_REGION);

                // Output access and secret key only if logging level is set to the lowest level
                LOGGER.trace(MessageCodes.MFS_007, s3AccessKey, s3SecretKey);

                // Configure AWS credentials
                final AWSCredentials awsCreds = new BasicAWSCredentials(s3AccessKey, s3SecretKey);
                final AWSCredentialsProvider credsProvider = new AWSStaticCredentialsProvider(awsCreds);
                final AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard();

                // Create S3 client from supplied credentials and region
                s3ClientBuilder.withCredentials(credsProvider).withRegion(s3Region);

                myS3Client = s3ClientBuilder.build();
                myS3Bucket = config.getString(Config.S3_BUCKET);

                LOGGER.debug(MessageCodes.MFS_005, s3Region);

                aFuture.complete();
            }
        });
    }
}
