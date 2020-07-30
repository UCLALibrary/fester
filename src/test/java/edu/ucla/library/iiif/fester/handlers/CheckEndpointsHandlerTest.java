
package edu.ucla.library.iiif.fester.handlers;

import org.junit.Test;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.Status;
import edu.ucla.library.iiif.fester.MessageCodes;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

/**
 * A test class to evaluate CheckEndpointsHandler
 */

public class CheckEndpointsHandlerTest extends AbstractFesterHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckEndpointsHandlerTest.class, Constants.MESSAGES);

    /**
     * Test the CheckEndpointsHandler
     *
     * @param aContext A testing context
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testCheckEndpointsHandler(final TestContext aContext) {
        final String expectedStatus = Status.OK;
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String requestPath = "/fester/endpoints";
        final int expectedPutStatus = HTTP.OK;

        LOGGER.debug(MessageCodes.MFS_150, requestPath);

        myVertx.createHttpClient().getNow(port, Constants.UNSPECIFIED_HOST, requestPath, response -> {
            final int statusCode = response.statusCode();

            if (response.statusCode() == HTTP.OK) {
                response.bodyHandler(body -> {
                    final JsonObject aResult = body.toJsonObject();
                    aContext.assertEquals(expectedStatus, aResult.getValue(Status.STATUS));
                });
            } else {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_003, HTTP.OK, statusCode));
            }

            if (!asyncTask.isCompleted()) {
                asyncTask.complete();
            }
        });
    }

}
