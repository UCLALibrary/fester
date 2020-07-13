
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

public class GetStatusHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetStatusHandler.class, Constants.MESSAGES);

    private static final int MB = 1024 * 1024;

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final JsonObject status = new JsonObject();

        try {
            final Runtime runtime = Runtime.getRuntime();
            final long totalMem = runtime.totalMemory() / MB;
            final long freeMem = runtime.freeMemory() / MB;
            final long usedMem = totalMem - freeMem;
            final JsonObject memory = new JsonObject();

            status.put(Status.STATUS, Status.OK).put(Status.MEMORY, memory);
            memory.put(Status.TOTAL_MEMORY, totalMem).put(Status.FREE_MEMORY, freeMem).put(Status.USED_MEMORY, usedMem);

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
