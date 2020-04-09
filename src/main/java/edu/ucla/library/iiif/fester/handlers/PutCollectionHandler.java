
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

public class PutCollectionHandler extends AbstractFesterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutCollectionHandler.class, Constants.MESSAGES);

    /**
     * Creates a handler that creates IIIF collection manifests in Fester.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A JSON configuration
     */
    public PutCollectionHandler(final Vertx aVertx, final JsonObject aConfig) {
        super(aVertx, aConfig);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final String collectionName = aContext.request().getParam(Constants.COLLECTION_NAME);
        final JsonObject message = new JsonObject();
        final DeliveryOptions options = new DeliveryOptions();

        message.put(Constants.COLLECTION_NAME, collectionName).put(Constants.DATA, aContext.getBodyAsJson());
        options.addHeader(Constants.ACTION, Op.PUT_COLLECTION);

        sendMessage(S3BucketVerticle.class.getName(), message, options, send -> {
            if (send.succeeded()) {
                response.setStatusCode(HTTP.OK).end();
            } else {
                final Throwable aThrowable = send.cause();
                final String exceptionMessage = aThrowable.getMessage();
                final String errorMessage = LOGGER.getMessage(MessageCodes.MFS_015, exceptionMessage);

                LOGGER.error(aThrowable, errorMessage);

                response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                response.setStatusMessage(exceptionMessage);
                response.putHeader(Constants.CONTENT_TYPE, Constants.PLAIN_TEXT_TYPE);
                response.end(errorMessage);
            }
        });
    }
}
