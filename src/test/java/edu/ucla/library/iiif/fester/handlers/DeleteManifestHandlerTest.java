
package edu.ucla.library.iiif.fester.handlers;

import java.io.IOException;

import org.junit.Test;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import edu.ucla.library.iiif.fester.utils.TestUtils;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

/**
 * Tests the handler that processes requests to delete a manifest.
 */
public class DeleteManifestHandlerTest extends AbstractFesterHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteManifestHandlerTest.class, Constants.MESSAGES);

    /**
     * Test the DeleteManifestHandler.
     *
     * @param aContext A testing context
     * @throws IOException If there is trouble reading a manifest
     */
    @Test
    public void testDeleteManifestHandler(final TestContext aContext) throws IOException {
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String requestPath = IDUtils.getResourceURIPath(myManifestS3Key);
        final HttpClient client = myVertx.createHttpClient();

        LOGGER.debug(MessageCodes.MFS_012, myManifestS3Key);

        client.request(HttpMethod.DELETE, port, Constants.UNSPECIFIED_HOST, requestPath).onSuccess(handler -> {
            handler.response(response -> {
                final int statusCode = response.result().statusCode();

                if (statusCode == HTTP.SUCCESS_NO_CONTENT) {
                    aContext.assertFalse(myS3Client.doesObjectExist(myS3Bucket, myManifestS3Key));
                    TestUtils.complete(asyncTask);
                } else {
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_004, HTTP.SUCCESS_NO_CONTENT, statusCode));
                }
            }).end();
        }).onFailure(aContext::fail);
    }

    /**
     * Confirm that a bad path request returns a 404 response.
     *
     * @param aContext A testing context
     */
    @Test
    public void testDeleteManifestHandler404(final TestContext aContext) {
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String testIDPath = "/testIdentifier"; // path should be: /{id}/manifest
        final HttpClient client = myVertx.createHttpClient();

        client.request(HttpMethod.DELETE, port, Constants.UNSPECIFIED_HOST, testIDPath).onSuccess(handler -> {
            handler.response(response -> {
                final int statusCode = response.result().statusCode();

                if (statusCode != HTTP.NOT_FOUND) {
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_004, HTTP.NOT_FOUND, statusCode));
                }

                TestUtils.complete(asyncTask);
            }).end();
        }).onFailure(aContext::fail);
    }

}
