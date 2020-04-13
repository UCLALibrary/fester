
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
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * A IIIF manifest retriever.
 */
public class GetManifestHandler extends AbstractFesterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetManifestHandler.class, Constants.MESSAGES);

    /**
     * Creates a handler that returns IIIF manifests from Fester.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A JSON configuration
     */
    public GetManifestHandler(final Vertx aVertx, final JsonObject aConfig) {
        super(aVertx, aConfig);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final String manifestID = aContext.request().getParam(Constants.MANIFEST_ID);
        final JsonObject message = new JsonObject();
        final DeliveryOptions options = new DeliveryOptions();

        message.put(Constants.MANIFEST_ID, manifestID);
        options.addHeader(Constants.ACTION, Op.GET_MANIFEST);

        sendMessage(S3BucketVerticle.class.getName(), message, options, send -> {
            response.headers().set(Constants.CORS_HEADER, Constants.STAR);

            if (send.succeeded()) {
                final String manifest = send.result().body().toString();

                response.setStatusCode(HTTP.OK);
                response.putHeader(Constants.CONTENT_TYPE, Constants.JSON_MEDIA_TYPE);
                response.end(manifest);
            } else {
                final Throwable aThrowable = send.cause();
                final String exceptionMessage = aThrowable.getMessage();
                final String errorMessage = LOGGER.getMessage(MessageCodes.MFS_009, manifestID, HTTP.NOT_FOUND,
                        exceptionMessage);

                LOGGER.error(aThrowable, errorMessage);

                response.setStatusCode(HTTP.NOT_FOUND);
                response.setStatusMessage(exceptionMessage);
                response.putHeader(Constants.CONTENT_TYPE, Constants.PLAIN_TEXT_TYPE);
                response.end(errorMessage);
            }
        });
    }

}
