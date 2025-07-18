
package edu.ucla.library.iiif.fester.handlers;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.verticles.S3BucketVerticle;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * A IIIF manifest creator.
 */
public class PutManifestHandler extends AbstractFesterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutManifestHandler.class, Constants.MESSAGES);

    /**
     * Creates a handler that creates IIIF manifests in Fester.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A JSON configuration
     */
    public PutManifestHandler(final Vertx aVertx, final JsonObject aConfig) {
        super(aVertx, aConfig);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final HttpServerRequest request = aContext.request();
        final String manifestID = request.getParam(Constants.MANIFEST_ID);
        final DeliveryOptions options = new DeliveryOptions();
        final JsonObject message = new JsonObject();

        message.put(Constants.MANIFEST_ID, manifestID);
        message.put(Constants.DATA, aContext.getBodyAsJson());
        options.addHeader(Constants.ACTION, Op.PUT_MANIFEST);

        sendMessage(S3BucketVerticle.class.getName(), message, options, send -> {
            response.headers().set(Constants.CORS_HEADER, Constants.STAR);

            if (send.succeeded()) {
                response.setStatusCode(HTTP.OK);
                response.end(Op.SUCCESS);
            } else {
                final ReplyException failure = (ReplyException) send.cause();
                final int status = failure.failureCode();
                final String statusMessage = failure.getMessage();
                final String error = LOGGER.getMessage(MessageCodes.MFS_009, manifestID, status, statusMessage);

                LOGGER.error(error);

                response.setStatusCode(status);
                response.setStatusMessage(statusMessage);
                response.putHeader(Constants.CONTENT_TYPE, Constants.PLAIN_TEXT_TYPE);
                response.end(error);
            }
        });
    }

}
