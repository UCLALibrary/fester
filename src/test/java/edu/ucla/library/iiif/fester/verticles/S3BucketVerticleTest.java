
package edu.ucla.library.iiif.fester.verticles;

import static edu.ucla.library.iiif.fester.Constants.MESSAGES;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.regex.Pattern;

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
import com.amazonaws.services.s3.model.AmazonS3Exception;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.IDUtils;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * Tests the verticle that handles S3 bucket interactions.
 */
@RunWith(VertxUnitRunner.class)
public class S3BucketVerticleTest extends AbstractFesterVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3BucketVerticleTest.class, MESSAGES);

    private static final String TEST_COLLECTION_FILE = "src/test/resources/json/v2/ark%3A%2F21198%2Fzz0009gsq9.json";

    private static final String MANIFEST_PATH = "src/test/resources/json/v2/testManifest.json";

    private static final String DEFAULT_ACCESS_KEY = "YOUR_ACCESS_KEY";

    private static String myS3Bucket = "unconfigured";

    private static AWSCredentials myAWSCredentials;

    @Rule
    public RunTestOnContext myRunTestOnContextRule = new RunTestOnContext();

    private String myManifestID;

    private String myManifestS3Key;

    private URI myManifestUri;

    private String myCollectionID;

    private String myCollectionS3Key;

    private URI myCollectionUri;

    /** We can't, as of yet, execute these tests without a non-default S3 configuration */
    private boolean isExecutable;

    private AmazonS3 myAmazonS3;

    private String myUrl;

    private final String myUrlPlaceholderPattern = Pattern.quote(Constants.URL_PLACEHOLDER);

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

                myUrl = config.getString(Config.FESTER_URL);

                // Create the IDs that will be needed for running the tests
                myManifestID = UUID.randomUUID().toString();
                myManifestS3Key = IDUtils.getWorkS3Key(myManifestID);
                myManifestUri = IDUtils.getResourceURI(myUrl, myManifestS3Key);

                myCollectionID = UUID.randomUUID().toString();
                myCollectionS3Key = IDUtils.getCollectionS3Key(myCollectionID);
                myCollectionUri = IDUtils.getResourceURI(myUrl, myCollectionS3Key);

                // We need to determine if we'll be able to run the S3 integration tests so we can skip if needed
                if (config.containsKey(Config.S3_ACCESS_KEY) && !config
                        .getString(Config.S3_ACCESS_KEY, DEFAULT_ACCESS_KEY).equalsIgnoreCase(DEFAULT_ACCESS_KEY)) {
                    isExecutable = true;
                }

                vertx.deployVerticle(S3BucketVerticle.class.getName(), options.setConfig(config), deployment -> {
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

                    myS3Bucket = config.getString(Config.S3_BUCKET);

                    LOGGER.debug(MessageCodes.MFS_067, getClass().getName());
                    complete(asyncTask);
                });
            } else {
                aContext.fail(getConfig.cause());
                complete(asyncTask);
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
            } else {
                // Clean up our manifest test file
                if (myAmazonS3.doesObjectExist(myS3Bucket, myManifestS3Key)) {
                    myAmazonS3.deleteObject(myS3Bucket, myManifestS3Key);
                }

                // Clean up our collection test file
                if (myAmazonS3.doesObjectExist(myS3Bucket, myCollectionS3Key)) {
                    myAmazonS3.deleteObject(myS3Bucket, myCollectionS3Key);
                }

                complete(async);
            }
        });
    }

    /**
     * Tests getting an S3 collection.
     *
     * @param aContext A testing environment
     * @throws IOException If there is trouble reading a manifest
     */
    @Test
    public final void testGetS3Collection(final TestContext aContext) throws IOException {
        try {
            // Skip this test if we don't have a valid S3 configuration
            assumeTrue(LOGGER.getMessage(MessageCodes.MFS_065), isExecutable);
        } catch (final AssumptionViolatedException details) {
            LOGGER.warn(details.getMessage());
            throw details;
        }

        final File collectionFile = new File(TEST_COLLECTION_FILE);
        final JsonObject expected =
                new JsonObject(StringUtils.read(collectionFile).replaceAll(myUrlPlaceholderPattern, myUrl));
        final JsonObject message = new JsonObject();
        final DeliveryOptions options = new DeliveryOptions();
        final Async asyncTask = aContext.async();

        message.put(Constants.COLLECTION_NAME, myCollectionID);
        options.addHeader(Constants.ACTION, Op.GET_COLLECTION);

        // Initialize our inherited class' vertx instance
        vertx = myRunTestOnContextRule.vertx();

        // Put our test object in the bucket so we can get it in our test
        myAmazonS3.putObject(myS3Bucket, myCollectionS3Key, collectionFile);

        if (myAmazonS3.doesObjectExist(myS3Bucket, myCollectionS3Key)) {
            sendMessage(S3BucketVerticle.class.getName(), message, options, send -> {
                if (send.succeeded()) {
                    aContext.assertEquals(expected, send.result().body());

                    complete(asyncTask);
                } else {
                    aContext.fail(send.cause());
                }
            });
        } else {
            aContext.fail();
        }
    }

    /**
     * Tests being able to store a work manifest to S3. This requires an actual S3 configuration. The test will be
     * skipped if no such configuration exists.
     *
     * @param aContext A test context
     */
    @Test
    public final void testS3ManifestStorage(final TestContext aContext) {
        try {
            // Skip this test if we don't have a valid S3 configuration
            assumeTrue(LOGGER.getMessage(MessageCodes.MFS_065), isExecutable);
        } catch (final AssumptionViolatedException details) {
            LOGGER.warn(details.getMessage());
            throw details;
        }

        final Vertx vertx = myRunTestOnContextRule.vertx();
        final Async asyncTask = aContext.async();
        final Buffer manifestContent = vertx.fileSystem().readFileBlocking(MANIFEST_PATH);
        final JsonObject manifest = manifestContent.toJsonObject();
        final JsonObject message = new JsonObject();
        final DeliveryOptions options = new DeliveryOptions();

        // Create a fake manifest ID/URI with our test manifest key
        manifest.put(Constants.ID_V2, myManifestUri.toString());

        message.put(Constants.MANIFEST_ID, myManifestID).put(Constants.DATA, manifest);
        options.addHeader(Constants.ACTION, Op.PUT_MANIFEST);

        vertx.eventBus().request(S3BucketVerticle.class.getName(), message, options, send -> {
            if (send.succeeded()) {
                final String s3Object;

                // When we check the object though we use the key that's used when the object is PUT
                try {
                    s3Object = myAmazonS3.getObjectAsString(myS3Bucket, myManifestS3Key);
                    aContext.assertEquals(manifest, new JsonObject(s3Object));
                } catch (final AmazonS3Exception details) {
                    aContext.fail(details);
                }

                complete(asyncTask);
            } else {
                aContext.fail(send.cause());
            }
        });
    }

    /**
     * Tests being able to store a collection manifest to S3. This requires an actual S3 configuration. The test will be
     * skipped if no such configuration exists.
     *
     * @param aContext A test context
     */
    @Test
    public final void testS3CollectionStorage(final TestContext aContext) {
        try {
            // Skip this test if we don't have a valid S3 configuration
            assumeTrue(LOGGER.getMessage(MessageCodes.MFS_065), isExecutable);
        } catch (final AssumptionViolatedException details) {
            LOGGER.warn(details.getMessage());
            throw details;
        }

        final Vertx vertx = myRunTestOnContextRule.vertx();
        final Async asyncTask = aContext.async();
        final Buffer manifestContent = vertx.fileSystem().readFileBlocking(MANIFEST_PATH);
        final JsonObject manifest = manifestContent.toJsonObject();
        final JsonObject message = new JsonObject();
        final DeliveryOptions options = new DeliveryOptions();

        // Create a fake manifest ID/URI with our collection key
        manifest.put(Constants.ID_V2, myCollectionUri.toString());

        LOGGER.debug(MessageCodes.MFS_130, manifest.getString(Constants.ID_V2));

        message.put(Constants.COLLECTION_NAME, myCollectionID).put(Constants.DATA, manifest);
        options.addHeader(Constants.ACTION, Op.PUT_COLLECTION);

        vertx.eventBus().request(S3BucketVerticle.class.getName(), message, options, send -> {
            if (send.succeeded()) {
                final String s3Object;

                // When we check the object though we use the key that's used when the object is PUT
                try {
                    LOGGER.debug(MessageCodes.MFS_129, myCollectionS3Key);

                    s3Object = myAmazonS3.getObjectAsString(myS3Bucket, myCollectionS3Key);
                    aContext.assertEquals(manifest, new JsonObject(s3Object));
                } catch (final AmazonS3Exception details) {
                    aContext.fail(details);
                }

                complete(asyncTask);
            } else {
                aContext.fail(send.cause());
            }
        });
    }

    /**
     * Completes an asynchronous task.
     *
     * @param aAsyncTask An asynchronous task
     */
    protected void complete(final Async aAsyncTask) {
        if (!aAsyncTask.isCompleted()) {
            aAsyncTask.complete();
        }
    }
}
