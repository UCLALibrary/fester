
package edu.ucla.library.iiif.fester.verticles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import info.freelibrary.util.FileUtils;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.RegexDirFilter;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * Tests run against the ManifestVerticle.
 */
@RunWith(VertxUnitRunner.class)
public class ManifestVerticleTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestVerticleTest.class, Constants.MESSAGES);

    private static final String MANIFEST_HOST = "https://iiif.library.ucla.edu";

    private static final String IMAGE_HOST = "https://iiif.library.ucla.edu/iiif/2";

    private static final String WORKS_CSV = "src/test/resources/csv/{}/batch1/{}1.csv";

    private static final String SINAI_WORKS_CSV = "src/test/resources/csv/{}_test_12/works.csv";

    private static final String CSV_FILE_PATH = "src/test/resources/csv/{}.csv";

    private static final String HATHAWAY = "hathaway";

    private static final String SINAI = "sinai";

    private static final String POSTCARDS = "capostcards";

    private static final String WORKS = "works";

    private Vertx myVertx;

    private String myRunID;

    /**
     * Initialization of the testing environment before the tests are run.
     *
     * @param aContext A testing context
     * @throws IOException If there is trouble starting the Vert.x instance
     */
    @Before
    public void setUp(final TestContext aContext) throws IOException {
        final DeploymentOptions options = new DeploymentOptions();
        final JsonObject config = new JsonObject();
        final Async asyncTask = aContext.async();

        options.setConfig(config.put(Config.IIIF_BASE_URL, IMAGE_HOST));

        myVertx = Vertx.vertx();
        myVertx.deployVerticle(ManifestVerticle.class.getName(), options, manifestorDeployment -> {
            if (manifestorDeployment.succeeded()) {
                myVertx.deployVerticle(FakeS3BucketVerticle.class.getName(), options, s3BucketDeployment -> {
                    if (s3BucketDeployment.succeeded()) {
                        final LocalMap<String, String> map = myVertx.sharedData().getLocalMap(Constants.VERTICLE_MAP);
                        final String deploymentKey = FakeS3BucketVerticle.class.getSimpleName();

                        System.out.println("here");
                        final Iterator iterator = map.keySet().iterator();
                        while (iterator.hasNext()) {
                            System.out.println("}} " + iterator.next());
                        }

                        if (map.containsKey(deploymentKey)) {
                            final String deploymentID = map.get(deploymentKey);

                            try {
                                System.out.println("+++++++++++ " + getS3TempDir(deploymentID));
                            } catch (final FileNotFoundException details) {
                                System.out.println(details.getMessage());
                            }
                        } else {
                            System.out.println("deployment key not found: " + deploymentKey);
                        }

                        asyncTask.complete();
                    } else {
                        aContext.fail(s3BucketDeployment.cause());
                    }
                });
            } else {
                aContext.fail(manifestorDeployment.cause());
            }
        });

        myRunID = UUID.randomUUID().toString();
    }

    /**
     * Cleanup the testing environment after the tests have been run.
     *
     * @param aContext A testing context
     */
    @After
    public void tearDown(final TestContext aContext) {
        myVertx.close(aContext.asyncAssertSuccess());
    }

    /**
     * Test against the Sinai works.
     *
     * @param aContext A testing context
     */
    @Test
    public final void testSinaiWorksManifest(final TestContext aContext) {
        final String path = StringUtils.format(SINAI_WORKS_CSV, SINAI);
        final JsonObject message = new JsonObject();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID).put(Constants.CSV_FILE_PATH, path);
        message.put(Constants.FESTER_HOST, MANIFEST_HOST);

        LOGGER.debug(MessageCodes.MFS_120, SINAI, ManifestVerticle.class.getName());

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, request -> {
            if (request.succeeded()) {
                asyncTask.complete();
            } else {
                aContext.fail(request.cause());
            }
        });
    }

    /**
     * Test against the Hathaway collection.
     *
     * @param aContext A testing context
     */
    @Test
    public final void testHathawayManifest(final TestContext aContext) {
        final String filePath = StringUtils.format(CSV_FILE_PATH, HATHAWAY);
        final JsonObject message = new JsonObject();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID).put(Constants.CSV_FILE_PATH, filePath);
        message.put(Constants.FESTER_HOST, MANIFEST_HOST);

        LOGGER.debug(MessageCodes.MFS_120, HATHAWAY, ManifestVerticle.class.getName());

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, request -> {
            if (request.succeeded()) {
                asyncTask.complete();
            } else {
                aContext.fail(request.cause());
            }
        });
    }

    /**
     * Test against the CA Postcards collection.
     *
     * @param aContext A testing context
     */
    @Test
    public final void testPostcardsManifest(final TestContext aContext) {
        final String filePath = StringUtils.format(CSV_FILE_PATH, POSTCARDS);
        final JsonObject message = new JsonObject();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID).put(Constants.CSV_FILE_PATH, filePath);
        message.put(Constants.FESTER_HOST, MANIFEST_HOST);

        LOGGER.debug(MessageCodes.MFS_120, POSTCARDS, ManifestVerticle.class.getName());

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, request -> {
            if (request.succeeded()) {
                asyncTask.complete();
            } else {
                aContext.fail(request.cause());
            }
        });
    }

    /**
     * Test against the Hathaway (sample Sinai) collection.
     *
     * @param aContext A testing context
     */
    @Test
    public final void testHathawaySinaiManifests(final TestContext aContext) {
        final String hathawayWorks = HATHAWAY + "/batch1/works";
        final String filePath = StringUtils.format(CSV_FILE_PATH, hathawayWorks);
        final JsonObject message = new JsonObject();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID).put(Constants.CSV_FILE_PATH, filePath);
        message.put(Constants.FESTER_HOST, MANIFEST_HOST);

        LOGGER.debug(MessageCodes.MFS_120, hathawayWorks, ManifestVerticle.class.getName());

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, request -> {
            if (request.succeeded()) {
                asyncTask.complete();
            } else {
                aContext.fail(request.cause());
            }
        });
    }

    /**
     * Test against a CSV that just has pages.
     *
     * @param aContext A testing context
     */
    @Test
    public final void testPagesManifest(final TestContext aContext) {
        final String filePath = StringUtils.format(WORKS_CSV, HATHAWAY, HATHAWAY);
        final JsonObject message = new JsonObject();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID).put(Constants.CSV_FILE_PATH, filePath);
        message.put(Constants.FESTER_HOST, MANIFEST_HOST);

        LOGGER.debug(MessageCodes.MFS_120, WORKS, ManifestVerticle.class.getName());

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, request -> {
            if (request.succeeded()) {
                asyncTask.complete();
            } else {
                aContext.fail(request.cause());
            }
        });
    }

    /**
     * Gets the location of the temporary fake S3 file system created by the FakeS3BucketVerticle.
     *
     * @param s3DeploymentID A deployment ID for the FakeS3BucketVerticle
     * @return The location of the temporary fake S3 file system
     * @throws FileNotFoundException If the directory could not be found
     */
    private String getS3TempDir(final String s3DeploymentID) throws FileNotFoundException {
        final FilenameFilter dirFilter = new RegexDirFilter(s3DeploymentID + "_.*");
        final File[] dirs = FileUtils.listFiles(new File(System.getProperty("java.io.tmpdir")), dirFilter);

        if (LOGGER.isWarnEnabled() && dirs.length > 1) {
            LOGGER.warn(MessageCodes.MFS_075, s3DeploymentID);
        }

        System.out.println(">>>>>>>>>>>>>>>> " + dirs[0].getAbsolutePath());

        return dirs[0].getAbsolutePath();
    }
}
