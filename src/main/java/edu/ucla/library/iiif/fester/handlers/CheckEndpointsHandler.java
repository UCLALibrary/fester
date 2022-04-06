
package edu.ucla.library.iiif.fester.handlers;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import info.freelibrary.iiif.presentation.v2.Manifest;

import info.freelibrary.vertx.s3.UnexpectedStatusException;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Status;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that checks S3 endpoint status
 */
public class CheckEndpointsHandler extends AbstractFesterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckEndpointsHandler.class, Constants.MESSAGES);

    private static final String UPLOAD_KEY = "test_load.json";

    private static final String APPEND = " : ";

    /**
     * Creates a handler that checks S3 endpoint statuses.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A JSON configuration
     */
    public CheckEndpointsHandler(final Vertx aVertx, final JsonObject aConfig) {
        super(aVertx, aConfig);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final Manifest upload = new Manifest("id", "label");
        final JsonObject endpoints = new JsonObject();
        final JsonObject status = new JsonObject();

        Future.future(put -> {
            myS3Client.put(myS3Bucket, UPLOAD_KEY, Buffer.buffer(upload.toJSON().encodePrettily()), putResponse -> {
                if (putResponse.failed()) {
                    final UnexpectedStatusException error = (UnexpectedStatusException) putResponse.cause();
                    final int statusCode = error.getStatusCode();
                    final String message = error.getMessage();

                    determineEndpointStatus(Status.PUT_RESPONSE, statusCode, message, endpoints, put);
                } else {
                    determineEndpointStatus(Status.PUT_RESPONSE, HTTP.OK, Status.OK, endpoints, put);
                }
            });
        }).compose(addGet -> {
            return Future.future(get -> {
                myS3Client.get(myS3Bucket, UPLOAD_KEY, getResponse -> {
                    if (getResponse.failed()) {
                        final UnexpectedStatusException error = (UnexpectedStatusException) getResponse.cause();
                        final int statusCode = error.getStatusCode();
                        final String message = error.getMessage();

                        determineEndpointStatus(Status.GET_RESPONSE, statusCode, message, endpoints, get);
                    } else {
                        determineEndpointStatus(Status.GET_RESPONSE, HTTP.OK, Status.OK, endpoints, get);
                    }
                });
            });
        }).compose(addDel -> {
            return Future.future(delete -> {
                myS3Client.delete(myS3Bucket, UPLOAD_KEY, deleteResponse -> {
                    if (deleteResponse.failed()) {
                        final UnexpectedStatusException error = (UnexpectedStatusException) deleteResponse.cause();
                        final int statusCode = error.getStatusCode();
                        final String message = error.getMessage();

                        determineEndpointStatus(Status.GET_RESPONSE, statusCode, message, endpoints, delete);
                    } else {
                        determineEndpointStatus(Status.DELETE_RESPONSE, HTTP.OK, Status.OK, endpoints, delete);
                    }
                });
            });
        }).onSuccess(success -> {
            status.put(Status.STATUS, determineOverallStatus(endpoints)).put(Status.ENDPOINTS, endpoints);

            response.setStatusCode(HTTP.OK);
            response.putHeader(Constants.CONTENT_TYPE, Constants.JSON_MEDIA_TYPE).end(status.encodePrettily());
        }).onFailure(failure -> {
            final String statusMessage = failure.getMessage();
            final String errorMessage = LOGGER.getMessage(MessageCodes.MFS_155, statusMessage);

            LOGGER.error(errorMessage);

            response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
            response.putHeader(Constants.CONTENT_TYPE, Constants.PLAIN_TEXT_TYPE);
            response.end(errorMessage);
        });

    }

    /**
     * Determines the endpoint's status.
     *
     * @param aCode A status code
     * @param aStatus A status message
     * @param aMessage Additional details in the form of a JsonObject
     * @param aPromise A promise of completion
     */
    private void determineEndpointStatus(final String aEndpoint, final int aCode, final String aStatus,
            final JsonObject aMessage, final Promise<Object> aPromise) {
        aMessage.put(aEndpoint, aCode);

        if (aCode >= HTTP.BAD_REQUEST && aCode < HTTP.INTERNAL_SERVER_ERROR) {
            aMessage.put(Status.STATUS, Status.WARN + APPEND + aStatus);
            aPromise.fail("4XX error: " + LOGGER.getMessage(MessageCodes.MFS_156, aStatus));
        } else if (aCode >= HTTP.INTERNAL_SERVER_ERROR) {
            aMessage.put(Status.STATUS, Status.ERROR + APPEND + aStatus);
            aPromise.fail("5XX error: " + LOGGER.getMessage(MessageCodes.MFS_156, aStatus));
        } else {
            aPromise.complete();
        }
    }

    /**
     * Determines the overall status from the details in the supplied JsonObject.
     *
     * @param aMessage Additional details in the form of a JsonObject
     * @return The overall status
     */
    private String determineOverallStatus(final JsonObject aMessage) {
        if (aMessage.encode().contains(Status.WARN)) {
            return Status.WARN;
        }

        if (aMessage.encode().contains(Status.ERROR)) {
            return Status.ERROR;
        }

        return Status.OK;
    }

}
