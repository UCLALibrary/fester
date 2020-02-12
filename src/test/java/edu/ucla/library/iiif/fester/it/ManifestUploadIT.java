
package edu.ucla.library.iiif.fester.it;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.TestConstants;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * This test confirms that our entire manifest upload process works as expected. We check that we can POST a manifest,
 * GET it back successfully, and then DELETE it.
 */
@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ManifestUploadIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestUploadIT.class, Constants.MESSAGES);

    private static final int PORT = Integer.parseInt(System.getProperty(Config.HTTP_PORT));

    private static final String MANIFEST_FILE_NAME = "testManifest.json";

    private static final File MANIFEST_FILE = new File("src/test/resources", MANIFEST_FILE_NAME);

    private static final String MANIFEST_PATH = "/{}/manifest";

    private static final String STATUS = "/status/fester";

    private static final String HELLO = "Hello";

    private static final String MANIFEST_ID = UUID.randomUUID().toString();

    private static Vertx myVertx = Vertx.vertx();

    final String myManifestFilePath = MANIFEST_FILE.getAbsolutePath();

    final Buffer myManifest = myVertx.fileSystem().readFileBlocking(myManifestFilePath);

    /**
     * Confirms the service is up (should run first, thanks to the FixMethodOrder config above).
     *
     * @param aContext A testing environment
     */
    @SuppressWarnings("deprecation")
    @Test
    public final void test1_CheckThatServiceIsUp(final TestContext aContext) {
        final Async asyncTask = aContext.async();

        // Give our Jar-based application a second or two to start up
        myVertx.setTimer(2000, timer -> {
            // First, let's sanity-check our service status endpoint before we do anything real
            myVertx.createHttpClient().getNow(PORT, Constants.UNSPECIFIED_HOST, STATUS, response -> {
                final int statusCode = response.statusCode();

                // Validate the response
                if (statusCode == HTTP.OK) {
                    response.bodyHandler(body -> {
                        aContext.assertEquals(body.getString(0, body.length()), HELLO);
                        LOGGER.info(MessageCodes.MFS_030);
                    });

                    if (!asyncTask.isCompleted()) {
                        asyncTask.complete();
                    }
                } else {
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_031));
                }
            });
        });
    }

    /**
     * Confirms it's possible to PUT a manifest.
     *
     * @param aContext A testing environment
     */
    @SuppressWarnings("deprecation")
    @Test
    public final void test2_CheckThatPutManifestWorks(final TestContext aContext) {
        final Async asyncTask = aContext.async();
        final String myPutManifestID = TestConstants.PUT_TEST_ID_PREFIX + MANIFEST_ID;
        final String requestPath = IDUtils.getResourceURIPath(IDUtils.getWorkS3Key(myPutManifestID));
        final Buffer myManifest = myVertx.fileSystem().readFileBlocking(myManifestFilePath);
        final RequestOptions requestOpts = new RequestOptions();

        requestOpts.setPort(PORT).setHost(Constants.UNSPECIFIED_HOST).setURI(requestPath);
        requestOpts.addHeader(Constants.CONTENT_TYPE, Constants.JSON_MEDIA_TYPE);

        LOGGER.info(MessageCodes.MFS_016, requestPath);

        myVertx.createHttpClient().put(requestOpts, response -> {
            final int statusCode = response.statusCode();

            if (statusCode == HTTP.OK) {
                asyncTask.complete();
            } else {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_018, myPutManifestID, response.statusMessage()));
            }
        }).end(myManifest);
    }

    /**
     * Confirms it's possible to GET a manifest.
     *
     * @param aContext A testing environment
     * @throws IOException If there is trouble reading the manifest
     */
    @SuppressWarnings("deprecation")
    @Test
    public final void test3_CheckThatGetManifestWorks(final TestContext aContext) throws IOException {
        final Async asyncTask = aContext.async();
        final String myPutManifestID = TestConstants.PUT_TEST_ID_PREFIX + MANIFEST_ID;
        final String requestPath = IDUtils.getResourceURIPath(IDUtils.getWorkS3Key(myPutManifestID));
        final String expectedManifest = StringUtils.read(MANIFEST_FILE);

        LOGGER.info(MessageCodes.MFS_027, requestPath);

        myVertx.createHttpClient().getNow(PORT, Constants.UNSPECIFIED_HOST, requestPath, response -> {
            final int statusCode = response.statusCode();

            if (statusCode == HTTP.OK) {
                response.bodyHandler(body -> {
                    final String foundManifest = body.toString(StandardCharsets.UTF_8);

                    // Check that what we retrieve is the same as what we stored
                    if (new JsonObject(expectedManifest).equals(new JsonObject(foundManifest))) {
                        asyncTask.complete();
                    } else {
                        aContext.fail(LOGGER.getMessage(MessageCodes.MFS_033, foundManifest, expectedManifest));
                    }
                });
            } else {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_009, myPutManifestID));
            }
        });
    }

    /**
     * Confirms it's possible to DELETE a manifest.
     *
     * @param aContext A testing environment
     */
    @SuppressWarnings("deprecation")
    @Test
    public final void test4_CheckThatDeleteManifestWorks(final TestContext aContext) {
        final Async asyncTask = aContext.async();
        final String myPutManifestID = TestConstants.PUT_TEST_ID_PREFIX + MANIFEST_ID;
        final String requestPath = StringUtils.format(MANIFEST_PATH, myPutManifestID);

        LOGGER.info(MessageCodes.MFS_028, requestPath);

        myVertx.createHttpClient().delete(PORT, Constants.UNSPECIFIED_HOST, requestPath, response -> {
            final int statusCode = response.statusCode();

            if (statusCode == HTTP.SUCCESS_NO_CONTENT) {
                asyncTask.complete();
            } else {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_035, myPutManifestID));
            }
        }).end();
    }

    /**
     * Confirms our test manifest is no longer stored.
     *
     * @param aContext A testing environment
     */
    @SuppressWarnings("deprecation")
    @Test
    public final void test5_CheckThatOurManifestIsNotStored(final TestContext aContext) {
        final Async asyncTask = aContext.async();
        final String myPutManifestID = TestConstants.PUT_TEST_ID_PREFIX + MANIFEST_ID;
        final String requestPath = StringUtils.format(MANIFEST_PATH, myPutManifestID);

        LOGGER.info(MessageCodes.MFS_029, requestPath);

        myVertx.createHttpClient().getNow(PORT, Constants.UNSPECIFIED_HOST, requestPath, response -> {
            final int statusCode = response.statusCode();

            if (statusCode == HTTP.OK) {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_036, myPutManifestID));
            } else {
                asyncTask.complete();
            }
        });
    }
}
