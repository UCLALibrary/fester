
package edu.ucla.library.iiif.fester.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class GetManifestHandlerTest extends AbstractFesterHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetManifestHandlerTest.class, Constants.MESSAGES);

    private static final String MANIFEST_PATH = "/{}/manifest";

    /**
     * Test the GetManifestHandler.
     *
     * @param aContext A testing context
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testGetManifestHandler(final TestContext aContext) throws IOException {
        final String expectedManifest = StringUtils.read(MANIFEST_FILE);
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String testIDPath = StringUtils.format(MANIFEST_PATH, myManifestID);

        LOGGER.debug(MessageCodes.MFS_008, myManifestID);

        myVertx.createHttpClient().getNow(port, Constants.UNSPECIFIED_HOST, testIDPath, response -> {
            final int statusCode = response.statusCode();

            if (response.statusCode() == HTTP.OK) {
                response.bodyHandler(body -> {
                    final String foundManifest = body.toString(StandardCharsets.UTF_8);

                    // Verify that the COR header is permissive
                    aContext.assertEquals(Constants.STAR, response.getHeader(Constants.CORS_HEADER));

                    // Verify that our retrieved JSON is as we expect it
                    aContext.assertEquals(new JsonObject(expectedManifest), new JsonObject(foundManifest));
                    asyncTask.complete();
                });
            } else {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_003, HTTP.OK, statusCode));
                asyncTask.complete();
            }
        });
    }

    /**
     * Test the GetManifestHandler with .json-less ID.
     *
     * @param aContext A testing context
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testGetManifestHandlerJson(final TestContext aContext) throws IOException {
        final String expectedManifest = StringUtils.read(MANIFEST_FILE);
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String testIDPath = StringUtils.format(MANIFEST_PATH, myJsonlessManifestID);

        LOGGER.debug(MessageCodes.MFS_008, myJsonlessManifestID);

        myVertx.createHttpClient().getNow(port, Constants.UNSPECIFIED_HOST, testIDPath, response -> {
            final int statusCode = response.statusCode();

            if (response.statusCode() == HTTP.OK) {
                response.bodyHandler(body -> {
                    final String foundManifest = body.toString(StandardCharsets.UTF_8);

                    // Check that what we retrieve is the same as what we stored
                    aContext.assertEquals(new JsonObject(expectedManifest), new JsonObject(foundManifest));
                    asyncTask.complete();
                });
            } else {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_003, HTTP.OK, statusCode));
                asyncTask.complete();
            }
        });
    }

    /**
     * Confirm that a bad path request returns a 404 response.
     *
     * @param aContext A testing context
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testGetManifestHandler404(final TestContext aContext) {
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String testIDPath = "/testIdentifier"; // path should be: /{id}/manifest

        myVertx.createHttpClient().getNow(port, Constants.UNSPECIFIED_HOST, testIDPath, response -> {
            final int statusCode = response.statusCode();

            if (response.statusCode() != HTTP.NOT_FOUND) {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_004, HTTP.NOT_FOUND, statusCode));
            }

            asyncTask.complete();
        });
    }

}
