
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
    @SuppressWarnings("deprecation")
    public void testDeleteManifestHandler(final TestContext aContext) throws IOException {
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String requestPath = IDUtils.getResourceURIPath(myManifestS3Key);

        LOGGER.debug(MessageCodes.MFS_012, myManifestS3Key);

        myVertx.createHttpClient().delete(port, Constants.UNSPECIFIED_HOST, requestPath, response -> {
            final int statusCode = response.statusCode();

            if (response.statusCode() == HTTP.SUCCESS_NO_CONTENT) {
                aContext.assertFalse(myS3Client.doesObjectExist(myS3Bucket, myManifestS3Key));
                if (!asyncTask.isCompleted()) {
                    asyncTask.complete();
                }
            } else {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_004, HTTP.SUCCESS_NO_CONTENT, statusCode));
            }
        }).end();
    }

    /**
     * Confirm that a bad path request returns a 404 response.
     *
     * @param aContext A testing context
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testDeleteManifestHandler404(final TestContext aContext) {
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String testIDPath = "/testIdentifier"; // path should be: /{id}/manifest

        myVertx.createHttpClient().delete(port, Constants.UNSPECIFIED_HOST, testIDPath, response -> {
            final int statusCode = response.statusCode();

            if (response.statusCode() != HTTP.NOT_FOUND) {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_004, HTTP.NOT_FOUND, statusCode));
            }

            if (!asyncTask.isCompleted()) {
                asyncTask.complete();
            }
        }).end();
    }

}
