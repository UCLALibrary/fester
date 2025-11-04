
package edu.ucla.library.iiif.fester.handlers;

import java.io.IOException;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handles ZIP file uploads.
 */
public class PostZipHandler extends AbstractFesterHandler {

    /** A logger for the handler. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PostZipHandler.class, Constants.MESSAGES);

    /**
     * Creates a handler to handle POSTs that upload Zip files.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A application configuration
     * @throws IOException If there is trouble reading the HTML template files
     */
    public PostZipHandler(final Vertx aVertx, final JsonObject aConfig) throws IOException {
        super(aVertx, aConfig);
    }

    @Override
    public void handle(RoutingContext aContext) {
        final Buffer body = aContext.getBody();

        if (body == null || body.length() == 0) {
            aContext.response().setStatusCode(400).end("Bad Request: no body provided");
        } else {
            aContext.response().setStatusCode(200).end("Uploaded");
        }
    }

}
