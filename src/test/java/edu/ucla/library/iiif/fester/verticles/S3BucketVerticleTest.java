
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
import com.amazonaws.services.s3.model.AmazonS3Exception;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class S3BucketVerticleTest extends AbstractFesterVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3BucketVerticleTest.class, MESSAGES);

    private static final String MANIFEST_PATH = "src/test/resources/testManifest.json";

    private static final String DEFAULT_ACCESS_KEY = "YOUR_ACCESS_KEY";

    private static final String MANIFEST_URI = "http://localhost:9999/{}/manifest";

    private static final String COLLECTION_URI = "http://localhost:9999{}";

    private static final String ID = "@id";

    private static String myS3Bucket = "unconfigured";

    private static AWSCredentials myAWSCredentials;

    @Rule
    public RunTestOnContext myRunTestOnContextRule = new RunTestOnContext();

    private String myManifestKey;

    private String myCollectionKey;

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

                // Create the IDs that will be needed for running the tests
                myManifestKey = UUID.randomUUID().toString();
                myCollectionKey = Constants.COLLECTIONS_PATH + Constants.SLASH + myManifestKey;

                // We need to determine if we'll be able to run the S3 integration tests so we can skip if needed
                if (config.containsKey(Config.S3_ACCESS_KEY) && !config.getString(Config.S3_ACCESS_KEY,
                        DEFAULT_ACCESS_KEY).equalsIgnoreCase(DEFAULT_ACCESS_KEY)) {
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
            } else {
                final String manifestKey = myManifestKey + Constants.JSON_EXT;
                final String collectionKey = myCollectionKey + Constants.JSON_EXT;

                // Clean up our manifest test file
                if (myAmazonS3.doesObjectExist(myS3Bucket, manifestKey)) {
                    myAmazonS3.deleteObject(myS3Bucket, manifestKey);
                }

                // Clean up our collection test file
                if (myAmazonS3.doesObjectExist(myS3Bucket, collectionKey)) {
                    myAmazonS3.deleteObject(myS3Bucket, collectionKey);
                }

                async.complete();
            }
        });
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

        // Create a fake manifest ID/URI with our test manifest key
        manifest.put(ID, StringUtils.format(MANIFEST_URI, myManifestKey));

        vertx.eventBus().request(S3BucketVerticle.class.getName(), manifest, send -> {
            if (send.succeeded()) {
                final String s3Object;

                // When we check the object though we use the key that's used when the object is PUT
                try {
                    s3Object = myAmazonS3.getObjectAsString(myS3Bucket, myManifestKey + Constants.JSON_EXT);
                    aContext.assertEquals(manifest, new JsonObject(s3Object));
                } catch (final AmazonS3Exception details) {
                    aContext.fail(details);
                }

                // If the assertion passed, we need to complete our task
                if (!asyncTask.isCompleted()) {
                    asyncTask.complete();
                }
            } else {
                aContext.fail(send.cause());
            }
        });
    }

    /**
     * Tests being able to store a collection manifest to S3. This requires an actual S3 configuration. The test will
     * be skipped if no such configuration exists.
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

        // Create a fake manifest ID/URI with our collection key
        manifest.put(ID, StringUtils.format(COLLECTION_URI, myCollectionKey));

        LOGGER.debug(MessageCodes.MFS_130, manifest.getString(ID));

        vertx.eventBus().request(S3BucketVerticle.class.getName(), manifest, send -> {
            if (send.succeeded()) {
                final String s3Object;

                // When we check the object though we use the key that's used when the object is PUT
                try {
                    final String collectionKey = myCollectionKey + Constants.JSON_EXT;

                    LOGGER.debug(MessageCodes.MFS_129, collectionKey);

                    s3Object = myAmazonS3.getObjectAsString(myS3Bucket, collectionKey);
                    aContext.assertEquals(manifest, new JsonObject(s3Object));
                } catch (final AmazonS3Exception details) {
                    aContext.fail(details);
                }

                // If the assertion passed, we need to complete our task
                if (!asyncTask.isCompleted()) {
                    asyncTask.complete();
                }
            } else {
                aContext.fail(send.cause());
            }
        });
    }
}
