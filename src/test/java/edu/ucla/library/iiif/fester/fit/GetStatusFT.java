
package edu.ucla.library.iiif.fester.fit;

import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.Status;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * Functional tests related to the Fester "status" endpoint.
 */
@RunWith(VertxUnitRunner.class)
public class GetStatusFT extends BaseFesterFT {

    /* Our status endpoint contains the service name because there are other /status options */
    private static final String API_PATH = "/status/fester";

    /**
     * Tests the status endpoint.
     *
     * @param aContext A testing context
     */
    @Test
    public final void testStatus(final TestContext aContext) {
        final Async asyncTask = aContext.async();

        myWebClient.get(FESTER_PORT, Constants.UNSPECIFIED_HOST, API_PATH).send(request -> {
            if (request.succeeded()) {
                final JsonObject response = request.result().bodyAsJsonObject();
                final String status = response.getString(Status.STATUS);
                final JsonObject memory = response.getJsonObject(Status.MEMORY);
                final long freeMemory = memory.getLong(Status.FREE_MEMORY);
                final long totalMemory = memory.getLong(Status.TOTAL_MEMORY);

                aContext.assertEquals(Status.OK, status);
                aContext.assertTrue(totalMemory > freeMemory);

                complete(asyncTask);
            } else {
                aContext.fail(request.cause());
            }
        });
    }

}
