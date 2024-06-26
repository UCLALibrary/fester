
package edu.ucla.library.iiif.fester.verticles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import info.freelibrary.util.FileUtils;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.RegexDirFilter;
import info.freelibrary.util.StringUtils;

import info.freelibrary.iiif.presentation.v2.Manifest;
import info.freelibrary.iiif.presentation.v2.Sequence;
import info.freelibrary.iiif.presentation.v2.io.Manifestor;
import info.freelibrary.iiif.presentation.v2.properties.ViewingDirection;
import info.freelibrary.iiif.presentation.v2.properties.ViewingHint;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.ImageInfoLookup;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import edu.ucla.library.iiif.fester.utils.TestUtils;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * Tests run against the ManifestVerticle.
 */
@RunWith(VertxUnitRunner.class)
public class ManifestVerticleTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestVerticleTest.class, Constants.MESSAGES);

    private static final String[] TEST_VERTICLES = new String[] { ManifestVerticle.class.getName(),
        V2ManifestVerticle.class.getName(), FakeS3BucketVerticle.class.getName() };

    private static final String WORKS_CSV = "src/test/resources/csv/{}/batch1/{}1.csv";

    private static final String SINAI_WORKS_CSV = "src/test/resources/csv/sinai_test_12/works.csv";

    private static final String CSV_FILE_PATH = "src/test/resources/csv/{}.csv";

    private static final String HATHAWAY = "hathaway";

    private static final String SINAI = "sinai";

    private static final String POSTCARDS = "capostcards";

    private static final String WORKS = "works";

    @Rule
    public Timeout myTestTimeout = Timeout.seconds(600);

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

        myVertx = Vertx.vertx();
        ConfigRetriever.create(myVertx).getConfig(getConfig -> {
            if (getConfig.succeeded()) {
                @SuppressWarnings("rawtypes")
                final List<Future> futures = new ArrayList<>();

                options.setConfig(config.put(Config.IIIF_BASE_URL, getConfig.result().getString(Config.IIIF_BASE_URL))
                        .put(Constants.IIIF_API_VERSION, Constants.IIIF_API_V2));

                for (final String verticleName : Arrays.asList(TEST_VERTICLES)) {
                    final Promise<String> promise = Promise.promise();

                    myVertx.deployVerticle(verticleName, options, deployment -> {
                        if (deployment.succeeded()) {
                            promise.complete(deployment.result());
                        } else {
                            promise.fail(deployment.cause());
                        }
                    });

                    futures.add(promise.future());
                }

                CompositeFuture.all(futures).onComplete(startup -> {
                    if (startup.succeeded()) {
                        final LocalMap<String, String> map = myVertx.sharedData().getLocalMap(Constants.VERTICLE_MAP);
                        final String deploymentKey = FakeS3BucketVerticle.class.getSimpleName();

                        if (map.containsKey(deploymentKey)) {
                            try {
                                myJsonFiles = getS3TempDir(map.get(deploymentKey));
                                TestUtils.complete(asyncTask);
                            } catch (final FileNotFoundException details) {
                                aContext.fail(details);
                            }
                        } else {
                            aContext.fail(LOGGER.getMessage(MessageCodes.MFS_077, deploymentKey));
                        }
                    } else {
                        aContext.fail(startup.cause());
                    }
                });

                myRunID = UUID.randomUUID().toString();
            } else {
                aContext.fail(getConfig.cause());
            }
        });
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
        final String jsonFile = myJsonFiles + getTestFilePath("ark:/21198/z16t1r0h");
        final JsonObject message = new JsonObject();
        final DeliveryOptions options = new DeliveryOptions();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID).put(Constants.CSV_FILE_PATH, SINAI_WORKS_CSV);
        options.addHeader(Constants.ACTION, Op.POST_CSV);

        LOGGER.debug(MessageCodes.MFS_120, SINAI, ManifestVerticle.class.getName());

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, options, request -> {
            if (request.succeeded()) {
                final JsonObject manifest = new JsonObject(myVertx.fileSystem().readFileBlocking(jsonFile));
                final String rightToLeft = ViewingDirection.RIGHT_TO_LEFT.toString();

                aContext.assertEquals(ViewingHint.Option.PAGED.toString(), manifest.getString("viewingHint"));
                aContext.assertEquals(rightToLeft, manifest.getString("viewingDirection"));

                TestUtils.complete(asyncTask);
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
        final DeliveryOptions options = new DeliveryOptions();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID).put(Constants.CSV_FILE_PATH, filePath);
        options.addHeader(Constants.ACTION, Op.POST_CSV);
        options.setSendTimeout(60000);

        LOGGER.debug(MessageCodes.MFS_120, HATHAWAY, ManifestVerticle.class.getName());

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, options, request -> {
            if (request.succeeded()) {
                TestUtils.complete(asyncTask);
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
        final String foundFile = myJsonFiles + getTestFilePath("ark:/21198/zz000s3rfj");
        final String filePath = StringUtils.format(CSV_FILE_PATH, POSTCARDS);
        final JsonObject message = new JsonObject();
        final DeliveryOptions options = new DeliveryOptions();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID).put(Constants.CSV_FILE_PATH, filePath);
        options.addHeader(Constants.ACTION, Op.POST_CSV);

        LOGGER.debug(MessageCodes.MFS_120, POSTCARDS, ManifestVerticle.class.getName());

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, options, request -> {
            if (request.succeeded()) {
                final Manifest foundManifest = new Manifestor().readManifest(new File(foundFile));
                final List<Sequence> sequences = foundManifest.getSequences();

                // Check that the sequence was added to this manifest
                aContext.assertEquals(1, sequences.size());

                // Check that the canvas was added to this sequence
                aContext.assertEquals(1, sequences.get(0).getCanvases().size());

                TestUtils.complete(asyncTask);
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
        final DeliveryOptions options = new DeliveryOptions();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID).put(Constants.CSV_FILE_PATH, filePath);
        options.addHeader(Constants.ACTION, Op.POST_CSV);

        LOGGER.debug(MessageCodes.MFS_120, hathawayWorks, ManifestVerticle.class.getName());

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, options, request -> {
            if (request.succeeded()) {
                TestUtils.complete(asyncTask);
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
        final DeliveryOptions options = new DeliveryOptions();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID).put(Constants.CSV_FILE_PATH, filePath);
        options.addHeader(Constants.ACTION, Op.POST_CSV);

        LOGGER.debug(MessageCodes.MFS_120, WORKS, ManifestVerticle.class.getName());

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, options, request -> {
            if (request.succeeded()) {
                TestUtils.complete(asyncTask);
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
    public final void testPageOrder(final TestContext aContext) {
        final String foundFile = myJsonFiles + getTestFilePath("ark:/21198/z12f8rtw");
        final String expectedFile = "src/test/resources/json/v2/pages-ordered.json";
        final String filePath = "src/test/resources/csv/sinai_del1/ara249.csv";
        final JsonObject message = new JsonObject();
        final DeliveryOptions options = new DeliveryOptions();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID).put(Constants.CSV_FILE_PATH, filePath);
        message.put(Constants.IIIF_HOST, ImageInfoLookup.FAKE_IIIF_SERVER);
        options.addHeader(Constants.ACTION, Op.POST_CSV);

        LOGGER.debug(MessageCodes.MFS_120, WORKS, ManifestVerticle.class.getName());

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, options, request -> {
            if (request.succeeded()) {
                try {
                    final FileSystem fileSystem = myVertx.fileSystem();

                    final JsonObject expected = new JsonObject(fileSystem.readFileBlocking(expectedFile));
                    final JsonObject found = new JsonObject(fileSystem.readFileBlocking(foundFile));

                    // Confirm that the order of our pages is what we expect it to be
                    aContext.assertEquals(expected, found);
                } catch (final Exception details) {
                    aContext.fail(details.getCause());
                }

                TestUtils.complete(asyncTask);
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

    /**
     * A convenience method for getting the test file's path.
     *
     * @param aID An ID
     * @return The path of the found test file
     */
    private String getTestFilePath(final String aID) {
        return Constants.SLASH + URLEncoder.encode(IDUtils.getWorkS3Key(aID), StandardCharsets.UTF_8);
    }
}
