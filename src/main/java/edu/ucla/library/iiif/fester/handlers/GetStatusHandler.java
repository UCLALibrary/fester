
package edu.ucla.library.iiif.fester.handlers;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.Status;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that gets the status of the application.
 */
public class GetStatusHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetStatusHandler.class, Constants.MESSAGES);

    private static final int MB = 1024 * 1024;

    private static final double WARN_PERCENT = 85.0D;

    private static final double ERROR_PERCENT = 95.0D;

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final JsonObject status = new JsonObject();

        try {
            final Runtime runtime = Runtime.getRuntime();
            final long totalMem = runtime.totalMemory() / MB;
            final long freeMem = runtime.freeMemory() / MB;
            final long usedMem = totalMem - freeMem;
            final double percentMem = (double) usedMem / (double) totalMem * 100D;
            final JsonObject memory = new JsonObject();
            final String totalMemStr = totalMem + Constants.SPACE + Constants.MB_STR;
            final String freeMemStr = freeMem + Constants.SPACE + Constants.MB_STR;
            final String usedMemStr = usedMem + Constants.SPACE + Constants.MB_STR;


            if (percentMem >= WARN_PERCENT && percentMem < ERROR_PERCENT) {
                status.put(Status.STATUS, Status.WARN);
            } else if (percentMem >= ERROR_PERCENT) {
                status.put(Status.STATUS, Status.ERROR);
            } else {
                status.put(Status.STATUS, Status.OK);
            }
            status.put(Status.MEMORY, memory);
            memory.put(Status.TOTAL_MEMORY, totalMemStr).put(Status.FREE_MEMORY, freeMemStr)
                .put(Status.USED_MEMORY, usedMemStr).put(Status.PERCENT_MEMORY, percentMem);

            response.setStatusCode(200);
            response.putHeader(Constants.CONTENT_TYPE, Constants.JSON_MEDIA_TYPE).end(status.encodePrettily());
        } catch (final Throwable aThrowable) {
            final String exceptionMessage = aThrowable.getMessage();

            LOGGER.error(aThrowable, exceptionMessage);

            response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
            response.putHeader(Constants.CONTENT_TYPE, Constants.PLAIN_TEXT_TYPE);
            response.end(exceptionMessage);
        }
    }

}
