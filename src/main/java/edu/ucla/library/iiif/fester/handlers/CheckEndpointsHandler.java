
package edu.ucla.library.iiif.fester.handlers;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.Status;

import info.freelibrary.iiif.presentation.Manifest;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * @author drickard1967
 *
 */
public class CheckEndpointsHandler extends AbstractFesterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckEndpointsHandler.class, Constants.MESSAGES);
    private static final String UPLOAD_KEY = "test_object.json";

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

        //upload.put("a_field","a value").put("another_field","another value").put("last_field","last value");

        try {
            myS3Client.put(myS3Bucket, UPLOAD_KEY, Buffer.buffer(upload.toJSON().encodePrettily()), putResponse -> {
                final int statusCode = putResponse.statusCode();
                LOGGER.info("PUT response = " + statusCode);
                endpoints.put(Status.PUT_RESPONSE, statusCode);
                determineEndpointStatus(statusCode,endpoints);
            });

            myS3Client.get(myS3Bucket, UPLOAD_KEY, getResponse -> {
                final int statusCode = getResponse.statusCode();
                LOGGER.info("GET response = " + statusCode);
                endpoints.put(Status.GET_RESPONSE, statusCode);
                determineEndpointStatus(statusCode,endpoints);
            });

            myS3Client.delete(myS3Bucket, UPLOAD_KEY, deleteResponse -> {
                final int statusCode = deleteResponse.statusCode();
                LOGGER.info("DELETE response = " + statusCode);
                endpoints.put(Status.DELETE_RESPONSE, statusCode);
                determineEndpointStatus(statusCode,endpoints);
            });

            status.put( Status.STATUS, determineOverallStatus(endpoints) ).put( Status.ENDPOINTS, endpoints );

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

    private void determineEndpointStatus(final int aCode, final JsonObject aMessage ) {
        if (aCode >= 400 && aCode < 500 ) {
            aMessage.put(Status.STATUS, Status.WARN);
        } else if (aCode >= 500) {
            aMessage.put(Status.STATUS, Status.ERROR);
        } else {
            aMessage.put(Status.STATUS, Status.OK);
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
