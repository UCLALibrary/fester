
package edu.ucla.library.iiif.fester.handlers;

import org.junit.Test;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Status;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;

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
    public void testCheckEndpointsHandler(final TestContext aContext) {
        final Async asyncTask = aContext.async();
        final String requestPath = "/fester/endpoints";
        final WebClient webClient = WebClient.create(myVertx);
        final int port = aContext.get(Config.HTTP_PORT);

        LOGGER.debug(MessageCodes.MFS_157, requestPath);

        webClient.get(port, Constants.UNSPECIFIED_HOST, requestPath).expect(ResponsePredicate.SC_SUCCESS)
            .as(BodyCodec.jsonObject()).send(ar -> {
                if (ar.succeeded()) {
                    final HttpResponse<JsonObject> response = ar.result();

                    aContext.assertEquals(HTTP.OK, response.statusCode());
                    aContext.assertEquals(Status.OK, response.body().getValue(Status.STATUS));

                    if (!asyncTask.isCompleted()) {
                        asyncTask.complete();
                    }
                } else {
                    aContext.fail(ar.cause());
                }
            });

    }

}
