
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
    private static final String API_PATH = "/fester/status";
    private static final String MB_STR = "MB";

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
                final splitMemInterface x = str -> str.split(" ");
                final String[] freeMemory = x.splitMem(memory.getString(Status.FREE_MEMORY));
                final String[] totalMemory = x.splitMem(memory.getString(Status.TOTAL_MEMORY));
                final String[] usedMemory = x.splitMem(memory.getString(Status.USED_MEMORY));

                aContext.assertEquals(Status.OK, status);
                aContext.assertTrue(freeMemory[1].equals(MB_STR) && totalMemory[1].equals(MB_STR)
                    && usedMemory[1].equals(MB_STR));

                complete(asyncTask);
            } else {
                aContext.fail(request.cause());
            }
        });
    }

    @FunctionalInterface
    interface splitMemInterface{
        String[] splitMem(String aMemStr);
    }

}
