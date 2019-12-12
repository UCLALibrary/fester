
package edu.ucla.library.iiif.fester.verticles;

import static edu.ucla.library.iiif.fester.Constants.MESSAGES;
import static org.junit.Assume.assumeTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class S3BucketVerticleTest extends AbstractFesterVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3BucketVerticleTest.class, MESSAGES);

    private static final String MANIFEST_PATH = "src/test/resources/testManifest.json";

    private static final String VERTICLE_NAME = S3BucketVerticle.class.getName();

    private static final String DEFAULT_ACCESS_KEY = "YOUR_ACCESS_KEY";

    private static String s3Bucket = "unconfigured";

    private static AWSCredentials myAWSCredentials;

    @Rule
    public RunTestOnContext myRunTestOnContextRule = new RunTestOnContext();

    private String myManifestKey;

    /** We can't, as of yet, execute these tests without a non-default S3 configuration */
    private boolean isExecutable;

    private AmazonS3 myAmazonS3;

    /**
     * Set up the testing environment.
     *
     * @param aContext A test context
     * @throws Exception If there is trouble starting Vert.x or configuring the tests
     */
    @SuppressWarnings("deprecation")
    @Before
    public void setUp(final TestContext aContext) throws Exception {
        final Vertx vertx = myRunTestOnContextRule.vertx();
        final ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
        final DeploymentOptions options = new DeploymentOptions();
        final Async asyncTask = aContext.async();

        configRetriever.getConfig(getConfig -> {
            if (getConfig.succeeded()) {
                final JsonObject config = getConfig.result();

                myManifestKey = UUID.randomUUID().toString() + Constants.JSON_EXT;

                // We need to determine if we'll be able to run the S3 integration tests so we can skip if needed
                if (config.containsKey(Config.S3_ACCESS_KEY) && !config.getString(Config.S3_ACCESS_KEY,
                        DEFAULT_ACCESS_KEY).equalsIgnoreCase(DEFAULT_ACCESS_KEY)) {
                    isExecutable = true;
                }

                vertx.deployVerticle(VERTICLE_NAME, options.setConfig(config), deployment -> {
                    if (deployment.failed()) {
                        final Throwable details = deployment.cause();
                        final String message = details.getMessage();

                        LOGGER.error(details, message);
                        aContext.fail(message);
                    }

                    if (myAmazonS3 == null) {
                        final String s3AccessKey = config.getString(Config.S3_ACCESS_KEY);
                        final String s3SecretKey = config.getString(Config.S3_SECRET_KEY);

                        // get myAWSCredentials ready
                        myAWSCredentials = new BasicAWSCredentials(s3AccessKey, s3SecretKey);

                        // instantiate the myAmazonS3 client
                        myAmazonS3 = new AmazonS3Client(myAWSCredentials);
                    }

                    s3Bucket = config.getString(Config.S3_BUCKET);
                    LOGGER.debug(MessageCodes.MFS_067, getClass().getName());
                    asyncTask.complete();
                });
            } else {
                aContext.fail(getConfig.cause());
                asyncTask.complete();
            }
        });
    }

    /**
     * Tear down the testing environment.
     *
     * @param aContext A test context
     * @throws Exception If there is trouble closing down the Vert.x instance
     */
    @After
    public void tearDown(final TestContext aContext) throws Exception {
        final Async async = aContext.async();

        myRunTestOnContextRule.vertx().close(result -> {
            if (!result.succeeded()) {
                final String message = LOGGER.getMessage(MessageCodes.MFS_066);

                LOGGER.error(message);
                aContext.fail(message);
            }

            // clean up our test files
            myAmazonS3.deleteObject(s3Bucket, myManifestKey);

            async.complete();
        });
    }

    /**
     * Tests being able to store to S3. This requires an actual S3 configuration. The test will be skipped if no such
     * configuration exists.
     *
     * @param aContext A test context
     */
    @Test
    public final void testS3Storage(final TestContext aContext) {
        try {
            // Skip this test if we don't have a valid S3 configuration
            assumeTrue(LOGGER.getMessage(MessageCodes.MFS_065), isExecutable);
        } catch (final AssumptionViolatedException details) {
            LOGGER.warn(details.getMessage());
            throw details;
        }

        final Vertx vertx = myRunTestOnContextRule.vertx();
        final JsonObject message = new JsonObject();
        final Async asyncTask = aContext.async();

        // load up our test Manifest.json file, and convert it to a JsonObject to hand to our S3 Verticle
        final JsonObject testManifestContent = vertx.fileSystem().readFileBlocking(MANIFEST_PATH).toJsonObject();

        // send the parameters the Fester S3 Verticle wants, those would be
        // param: manifestID:String - the identifier we will use when storing the Manifest in S3
        // param: manifest-content:jsonObject - holds the content of the Manifest object

        message.put(Constants.MANIFEST_ID, myManifestKey);

        message.put(Constants.MANIFEST_CONTENT, testManifestContent);

        vertx.eventBus().request(VERTICLE_NAME, message, send -> {
            if (send.failed()) {
                final Throwable details = send.cause();

                if (details != null) {
                    LOGGER.error(details, details.getMessage());
                }

                aContext.fail();
            }

            asyncTask.complete();
        });
    }

}