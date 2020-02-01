
package edu.ucla.library.iiif.fester.verticles;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import info.freelibrary.iiif.presentation.properties.ViewingDirection;
import info.freelibrary.iiif.presentation.properties.ViewingHint;
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

    private String myJsonFiles;

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

                        if (map.containsKey(deploymentKey)) {
                            try {
                                myJsonFiles = getS3TempDir(map.get(deploymentKey));
                                asyncTask.complete();
                            } catch (final FileNotFoundException details) {
                                aContext.fail(details);
                            }
                        } else {
                            aContext.fail(LOGGER.getMessage(MessageCodes.MFS_077, deploymentKey));
                        }
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
        final String jsonFile = myJsonFiles + Constants.SLASH
                + URLEncoder.encode("ark:/21198/z16t1r0h.json", StandardCharsets.UTF_8);
        final String path = StringUtils.format(SINAI_WORKS_CSV, SINAI);
        final JsonObject message = new JsonObject();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID).put(Constants.CSV_FILE_PATH, path);
        message.put(Constants.FESTER_HOST, MANIFEST_HOST);

        LOGGER.debug(MessageCodes.MFS_120, SINAI, ManifestVerticle.class.getName());

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, request -> {
            if (request.succeeded()) {
                final JsonObject manifest = new JsonObject(myVertx.fileSystem().readFileBlocking(jsonFile));


                assertEquals(ViewingHint.Option.PAGED.toString(), manifest.getString("viewingHint"));
                assertEquals(ViewingDirection.RIGHT_TO_LEFT.toString(), manifest.getString("viewingDirection"));

                if (!asyncTask.isCompleted()) {
                    asyncTask.complete();
                }
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
     * @param aS3DeploymentID A deployment ID for the FakeS3BucketVerticle
     * @return The location of the temporary fake S3 file system
     * @throws FileNotFoundException If the directory could not be found
     */
    private String getS3TempDir(final String aS3DeploymentID) throws FileNotFoundException {
        final FilenameFilter dirFilter = new RegexDirFilter(aS3DeploymentID + "_.*");
        final File[] dirs = FileUtils.listFiles(new File(System.getProperty("java.io.tmpdir")), dirFilter);
        final File s3TmpDir = dirs[0];
        final String s3TmpDirPath = s3TmpDir.getAbsolutePath();

        if (LOGGER.isWarnEnabled() && dirs.length > 1) {
            LOGGER.warn(MessageCodes.MFS_075, aS3DeploymentID);
        }

        if (!s3TmpDir.exists()) {
            throw new FileNotFoundException(s3TmpDirPath);
        }

        return s3TmpDirPath;
    }
}
