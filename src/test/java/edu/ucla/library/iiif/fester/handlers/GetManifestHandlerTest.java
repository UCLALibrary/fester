
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
import edu.ucla.library.iiif.fester.utils.IDUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class GetManifestHandlerTest extends AbstractFesterHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetManifestHandlerTest.class, Constants.MESSAGES);

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
        final String requestPath = IDUtils.getResourceURIPath(myManifestS3Key);

        LOGGER.debug(MessageCodes.MFS_008, requestPath);

        myVertx.createHttpClient().getNow(port, Constants.UNSPECIFIED_HOST, requestPath, response -> {
            final int statusCode = response.statusCode();

            if (response.statusCode() == HTTP.OK) {
                response.bodyHandler(body -> {
                    final String foundManifest = body.toString(StandardCharsets.UTF_8);

                    // Verify that the CORS header is permissive
                    aContext.assertEquals(Constants.STAR, response.getHeader(Constants.CORS_HEADER));

                    // Verify that our retrieved JSON is as we expect it
                    aContext.assertEquals(new JsonObject(expectedManifest), new JsonObject(foundManifest));
                    asyncTask.complete();
                });
            } else {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_003, HTTP.OK, statusCode));
            }
        });
    }

    /**
     * Test the GetManifestHandler with a resource ID that has a ".json" suffix.
     *
     * TODO: remove in 1.0.0
     *
     * @param aContext A testing context
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testGetManifestHandlerJsonSuffixedID(final TestContext aContext) throws IOException {
        final String expectedManifest = StringUtils.read(MANIFEST_FILE);
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        // Put a .json suffix on the resource identifier (ARK)
        final String manifestS3Key = IDUtils.getWorkS3Key(IDUtils.getResourceID(myManifestS3Key), Constants.JSON_EXT);
        final String requestPath = IDUtils.getResourceURIPath(manifestS3Key);

        LOGGER.debug(MessageCodes.MFS_008, requestPath);

        myVertx.createHttpClient().getNow(port, Constants.UNSPECIFIED_HOST, requestPath, response -> {
            final int statusCode = response.statusCode();

            if (response.statusCode() == HTTP.OK) {
                response.bodyHandler(body -> {
                    final String foundManifest = body.toString(StandardCharsets.UTF_8);

                    // Verify that the CORS header is permissive
                    aContext.assertEquals(Constants.STAR, response.getHeader(Constants.CORS_HEADER));

                    // Verify that our retrieved JSON is as we expect it
                    aContext.assertEquals(new JsonObject(expectedManifest), new JsonObject(foundManifest));
                    asyncTask.complete();
                });
            } else {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_003, HTTP.OK, statusCode));
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
        final String missingPath = "/missingIdentifier/manifest"; // path should be: /{id}/manifest

        myVertx.createHttpClient().getNow(port, Constants.UNSPECIFIED_HOST, missingPath, response -> {
            final int statusCode = response.statusCode();

            if (response.statusCode() != HTTP.NOT_FOUND) {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_004, HTTP.NOT_FOUND, statusCode));
            }

            asyncTask.complete();
        });
    }
}
