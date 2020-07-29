
package edu.ucla.library.iiif.fester.handlers;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Status;

import info.freelibrary.iiif.presentation.Manifest;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that checks S3 endpoint status
 *
 */
public class CheckEndpointsHandler extends AbstractFesterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckEndpointsHandler.class, Constants.MESSAGES);
    private static final String UPLOAD_KEY = "test_load.json";
    private static final String APPEND = " : ";
    /**
     *  Creates a handler that checks S3 endpoint statuses.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A JSON configuration
     */
    public CheckEndpointsHandler( final Vertx aVertx, final JsonObject aConfig ) {
        super( aVertx, aConfig );
    }

    @Override
    public void handle( final RoutingContext aContext ) {
        final HttpServerResponse response = aContext.response();
        final Manifest upload = new Manifest("id","label");
        final JsonObject endpoints = new JsonObject();
        final JsonObject status = new JsonObject();

        try {
            final Future<Object> composite = Future.future( put -> {
                myS3Client.put(myS3Bucket, UPLOAD_KEY, Buffer.buffer(upload.toJSON().encodePrettily()), putResponse -> {
                    final int statusCode = putResponse.statusCode();
                    final String statusMessage = putResponse.statusMessage();
                    endpoints.put(Status.PUT_RESPONSE, statusCode);
                    determineEndpointStatus(statusCode,statusMessage,endpoints,put);
                });
            } ).compose( addGet -> {
                return Future.future( get -> {
                    myS3Client.get(myS3Bucket, UPLOAD_KEY, getResponse -> {
                        final int statusCode = getResponse.statusCode();
                        final String statusMessage = getResponse.statusMessage();
                        endpoints.put(Status.GET_RESPONSE, statusCode);
                        determineEndpointStatus(statusCode,statusMessage,endpoints,get);
                    });
                } );
            } ).compose( addDel -> {
                return Future.future( delete -> {
                    myS3Client.delete(myS3Bucket, UPLOAD_KEY, deleteResponse -> {
                        final int statusCode = deleteResponse.statusCode();
                        final String statusMessage = deleteResponse.statusMessage();
                        endpoints.put(Status.DELETE_RESPONSE, statusCode);
                        determineEndpointStatus(statusCode,statusMessage,endpoints,delete);
                    });
                } );
            } );

            composite.onSuccess( success -> {
                status.put( Status.STATUS, determineOverallStatus(endpoints) ).put( Status.ENDPOINTS, endpoints );

                response.setStatusCode(HTTP.OK);
                response.putHeader(Constants.CONTENT_TYPE, Constants.JSON_MEDIA_TYPE).end(status.encodePrettily());
            } ).onFailure( failure -> {
                final String statusMessage = failure.getMessage();
                final String errorMessage = LOGGER.getMessage(MessageCodes.MFS_148, statusMessage);
                LOGGER.error(errorMessage);
                response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                response.putHeader(Constants.CONTENT_TYPE, Constants.PLAIN_TEXT_TYPE);
                response.end(errorMessage);
            } );

        } catch (final Throwable aThrowable) {
            final String exceptionMessage = aThrowable.getMessage();

            LOGGER.error(aThrowable, exceptionMessage);

            response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
            response.putHeader(Constants.CONTENT_TYPE, Constants.PLAIN_TEXT_TYPE);
            response.end(exceptionMessage);
        }

    }

    private void determineEndpointStatus(final int aCode, final String aStatus,
                                         final JsonObject aMessage, final Promise<Object> aPromise) {
        if (aCode >= HTTP.BAD_REQUEST && aCode < HTTP.INTERNAL_SERVER_ERROR) {
            final String failMessage = LOGGER.getMessage(MessageCodes.MFS_149, aStatus);
            aMessage.put(Status.STATUS, Status.WARN + APPEND + aStatus);
            aPromise.fail("4XX error: " + failMessage);
        } else if (aCode >= HTTP.INTERNAL_SERVER_ERROR) {
            final String failMessage = LOGGER.getMessage(MessageCodes.MFS_149, aStatus);
            aMessage.put(Status.STATUS, Status.ERROR + APPEND + aStatus);
            aPromise.fail("5XX error: " + failMessage);
        } else {
            aPromise.complete();
        }
    }

    private String determineOverallStatus(final JsonObject aMessage) {
        if (aMessage.encode().contains( Status.WARN )) {
            return Status.WARN;
        } else if (aMessage.encode().contains( Status.ERROR )) {
            return Status.ERROR;
        } else {
            return Status.OK;
        }
    }

}
