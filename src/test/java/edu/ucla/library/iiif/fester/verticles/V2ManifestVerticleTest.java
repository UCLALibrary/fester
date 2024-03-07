
package edu.ucla.library.iiif.fester.verticles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.TestUtils;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
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
public class V2ManifestVerticleTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(V2ManifestVerticleTest.class, Constants.MESSAGES);

    private static final String[] TEST_VERTICLES = new String[] { V2ManifestVerticle.class.getName(),
        ManifestVerticle.class.getName(), FakeS3BucketVerticle.class.getName() };

    private static final String THUMBNAIL_TEST_FILE = "z19s79m2.json";

    private static final String ORIGINAL_TEST_FILE = "z1fj82dp.json";

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

                config.mergeIn(getConfig.result());
                options.setConfig(config.put(Constants.IIIF_API_VERSION, Constants.IIIF_API_V2));

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
     * Tests whether the test CSV with a thumbnail is accurately transformed into a manifest.
     *
     * @param aContext A test context
     */
    @Test
    public final void testStaticResourceThumbnail(final TestContext aContext) {
        final DeliveryOptions options = new DeliveryOptions().addHeader(Constants.ACTION, Op.POST_CSV);
        final JsonObject message = new JsonObject();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID)
                .put(Constants.CSV_FILE_PATH, "src/test/resources/csv/static-image-dims.csv")
                .put(Constants.IIIF_API_VERSION, Constants.IIIF_API_V2);

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, options, request -> {
            if (request.succeeded()) {
                try {
                    final List<Path> results = Files.list(Path.of(myJsonFiles))
                            .filter(path -> path.toString().endsWith(THUMBNAIL_TEST_FILE)).collect(Collectors.toList());

                    if (results.isEmpty()) {
                        aContext.fail(new FileNotFoundException(THUMBNAIL_TEST_FILE));
                    } else {
                        new JsonObject(StringUtils.read(results.get(0).toFile())).equals(new JsonObject(
                                StringUtils.read(new File("src/test/resources/json/v2/thumbnail.json"))));
                    }
                } catch (final IOException details) {
                    aContext.fail(details);
                }

                TestUtils.complete(asyncTask);
            } else {
                aContext.fail(request.cause());
            }
        });
    }

    /**
     * Tests whether the test CSV without a thumbnail is accurately transformed into a manifest with a thumbnail.
     *
     * @param aContext A test context
     */
    @Test
    public final void testStaticResourceOriginal(final TestContext aContext) {
        final DeliveryOptions options = new DeliveryOptions().addHeader(Constants.ACTION, Op.POST_CSV);
        final JsonObject message = new JsonObject();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID)
                .put(Constants.CSV_FILE_PATH, "src/test/resources/csv/static-image-dims.csv")
                .put(Constants.IIIF_API_VERSION, Constants.IIIF_API_V2);

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, options, request -> {
            if (request.succeeded()) {
                try {
                    final List<Path> results = Files.list(Path.of(myJsonFiles))
                            .filter(path -> path.toString().endsWith(ORIGINAL_TEST_FILE)).collect(Collectors.toList());

                    if (results.isEmpty()) {
                        aContext.fail(new FileNotFoundException(ORIGINAL_TEST_FILE));
                    } else {
                        new JsonObject(StringUtils.read(results.get(0).toFile())).equals(
                                new JsonObject(StringUtils.read(new File("src/test/resources/json/v2/original.json"))));
                    }
                } catch (final IOException details) {
                    aContext.fail(details);
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

}
