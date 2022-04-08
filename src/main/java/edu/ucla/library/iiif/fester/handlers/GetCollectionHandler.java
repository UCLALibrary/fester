
package edu.ucla.library.iiif.fester.handlers;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.verticles.S3BucketVerticle;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that processes requests to get a collection document.
 */
public class GetCollectionHandler extends AbstractFesterHandler implements Handler<RoutingContext> {

    /**
     * The logger for this handler.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GetCollectionHandler.class, Constants.MESSAGES);

    /**
     * Creates a handler that returns IIIF collection manifests from Fester.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A JSON configuration
     */
    public GetCollectionHandler(final Vertx aVertx, final JsonObject aConfig) {
        super(aVertx, aConfig);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final String collectionName = aContext.request().getParam(Constants.COLLECTION_NAME);
        final JsonObject message = new JsonObject();
        final DeliveryOptions options = new DeliveryOptions();

        message.put(Constants.COLLECTION_NAME, collectionName);
        options.addHeader(Constants.ACTION, Op.GET_COLLECTION);

        sendMessage(S3BucketVerticle.class.getName(), message, options, send -> {
            response.headers().set(Constants.CORS_HEADER, Constants.STAR);

            if (send.succeeded()) {
                final String collection = send.result().body().toString();

                response.setStatusCode(HTTP.OK);
                response.putHeader(Constants.CONTENT_TYPE, Constants.JSON_MEDIA_TYPE);
                response.end(collection);
            } else {
                final ReplyException failure = (ReplyException) send.cause();
                final int statusCode = failure.failureCode();
                final String statusMessage = failure.getMessage();
                final String errorMessage =
                        LOGGER.getMessage(MessageCodes.MFS_009, collectionName, statusCode, statusMessage);

                LOGGER.error(errorMessage);

                response.setStatusCode(statusCode);
                response.setStatusMessage(statusMessage);
                response.putHeader(Constants.CONTENT_TYPE, Constants.PLAIN_TEXT_TYPE);
                response.end(errorMessage);
            }
        });
    }
}
