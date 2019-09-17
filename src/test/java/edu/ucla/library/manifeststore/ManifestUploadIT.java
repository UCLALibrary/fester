
package edu.ucla.library.manifeststore;

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

import edu.ucla.library.iiif.manifeststore.Constants;
import edu.ucla.library.iiif.manifeststore.HTTP;
import edu.ucla.library.iiif.manifeststore.MessageCodes;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * This test confirms that our entire manifest upload process works as expected. From the ticket that requested this
 * integration test (IIIF-350): Test should ensure: round trip: POST a doc and GET it back successfully. Then delete
 * it.
 */
@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ManifestUploadIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestUploadIT.class, Constants.MESSAGES);

    private static final int PORT = Integer.parseInt(System.getProperty("http.port"));

    private static final String MANIFEST_FILE_NAME = "testManifest.json";

    private static final File MANIFEST_FILE = new File("src/test/resources", MANIFEST_FILE_NAME);

    private static final String MANIFEST_PATH = "/{}/manifest";

    private static final String PING = "/ping";

    private static final String HELLO = "Hello";

    private static Vertx vertx = Vertx.vertx();

    private static final String MANIFEST_ID = UUID.randomUUID().toString();

    private static final String MANIFEST_ID_WITH_EXT = MANIFEST_ID + ".json";

    final String myManifestFilePath = MANIFEST_FILE.getAbsolutePath();

    final Buffer myManifest = vertx.fileSystem().readFileBlocking(myManifestFilePath);

    /**
     * confirm that the service is up (should run first, thanks to the FixMethodOrder config above)
     */
    @SuppressWarnings("deprecation")
    @Test
    public final void TestAcheckThatServiceIsUp(final TestContext aContext) {
        final Async asyncTask = aContext.async();
        // first, let's sanity-check our service ping endpoint before we do anything real
        vertx.createHttpClient().getNow(PORT, Constants.UNSPECIFIED_HOST, PING, response -> {
            // validate the response

            final int statusCode = response.statusCode();
            if (statusCode == HTTP.OK) {
                response.bodyHandler(body -> {
                    aContext.assertEquals(body.getString(0, body.length()), HELLO);
                    LOGGER.info(MessageCodes.MFS_030);
                });
                asyncTask.complete();
            } else {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_031));
            }

        }); // end HttpClient.getNow
    } // end method

    /**
     * confirm that it's possible to PUT a manifest
     */
    @SuppressWarnings("deprecation")
    @Test
    public final void TestBcheckThatPUTmanifestWorks(final TestContext aContext) {
        final Async asyncTask = aContext.async();

        /************ PUT TEST ************************************************/
        final String myDotJsonPutManifestID = Constants.PUT_TEST_ID_PREFIX + MANIFEST_ID_WITH_EXT;
        final String testIDPath = StringUtils.format(MANIFEST_PATH, myDotJsonPutManifestID);
        LOGGER.info(MessageCodes.MFS_016, testIDPath); // Test PUTing a test manifest to: {}
        final Buffer myManifest = vertx.fileSystem().readFileBlocking(myManifestFilePath);
        // set up our requestOpts for this PUT
        final RequestOptions requestOpts = new RequestOptions();
        requestOpts.setPort(PORT).setHost(Constants.UNSPECIFIED_HOST).setURI(testIDPath);
        requestOpts.addHeader(Constants.CONTENT_TYPE, Constants.JSON_MEDIA_TYPE);

        // PUT this Manifest
        vertx.createHttpClient().put(requestOpts, response -> {
            final int statusCode = response.statusCode();
            if (statusCode == HTTP.OK) {
                asyncTask.complete();
            } else {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_032));
            }
        }).end(myManifest); // end HttpClient.put
    } // end method

    /**
     * confirm that it's possible to GET a manifest
     *
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    @Test
    public final void TestCcheckThatGETmanifestWorks(final TestContext aContext) throws IOException {
        final Async asyncTask = aContext.async();
        /************ GET TEST ************************************************/
        final String myDotJsonPutManifestID = Constants.PUT_TEST_ID_PREFIX + MANIFEST_ID_WITH_EXT;
        final String testIDPath = StringUtils.format(MANIFEST_PATH, myDotJsonPutManifestID);
        final String expectedManifest = StringUtils.read(MANIFEST_FILE);

        LOGGER.info(MessageCodes.MFS_027, testIDPath); // Test GETing a test manifest from: {}
        vertx.createHttpClient().getNow(PORT, Constants.UNSPECIFIED_HOST, testIDPath, response -> {
            final int statusCode = response.statusCode();
            if (statusCode == HTTP.OK) {
                // yep, we can GET a manifest, one last thing to check
                response.bodyHandler(body -> {
                    final String foundManifest = body.toString(StandardCharsets.UTF_8);
                    // Check that what we retrieve is the same as what we stored
                    if (new JsonObject(expectedManifest).equals(new JsonObject(foundManifest))) {
                        asyncTask.complete();
                    } else {
                        aContext.fail(LOGGER.getMessage(MessageCodes.MFS_033));
                    }
                }); // end bodyHandler
            } else {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_034));
            } // end if

        }); // end HttpClient.get
    } // end method

    /**
     * confirm that it's possible to DELETE a manifest
     */
    @SuppressWarnings("deprecation")
    @Test
    public final void TestDcheckThatDELETEmanifestWorks(final TestContext aContext) {
        final Async asyncTask = aContext.async();
        /************ DELETE TEST *********************************************/
        final String myDotJsonPutManifestID = Constants.PUT_TEST_ID_PREFIX + MANIFEST_ID_WITH_EXT;
        final String testIDPath = StringUtils.format(MANIFEST_PATH, myDotJsonPutManifestID);
        LOGGER.info(MessageCodes.MFS_028, testIDPath); // Test DELETing a test manifest at: {}

        vertx.createHttpClient().delete(PORT, Constants.UNSPECIFIED_HOST, testIDPath, response -> {
            final int statusCode = response.statusCode();
            if (statusCode == HTTP.SUCCESS_NO_CONTENT) {
                asyncTask.complete();
            } else {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_035));
            } // end if/else
        }).end(); // end HttpClient.delete
    } // end method

    /**
     * confirm that our test manifest is no longer stored
     */
    @SuppressWarnings("deprecation")
    @Test
    public final void TestEcheckThatOurManifestIsNotStored(final TestContext aContext) {
        final Async asyncTask = aContext.async();
        /************ ANOTHER GET TEST, HOPE WE DON'T FIND IT AFTER A DELETE **/
        final String myDotJsonPutManifestID = Constants.PUT_TEST_ID_PREFIX + MANIFEST_ID_WITH_EXT;
        final String testIDPath = StringUtils.format(MANIFEST_PATH, myDotJsonPutManifestID);
        LOGGER.info(MessageCodes.MFS_029, testIDPath); // Confirming test manifest has been deleted from: {}
        vertx.createHttpClient().getNow(PORT, Constants.UNSPECIFIED_HOST, testIDPath, response -> {
            final int statusCode = response.statusCode();
            if (statusCode == HTTP.OK) {
                // aw man, this shouldn't be here still
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_036));
            } else {
                asyncTask.complete();
            } // end if/else
        }); // end HttpClient.getNow
    } // end method
} // end class
