
package edu.ucla.library.iiif.fester.handlers;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
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
    @SuppressWarnings("checkstyle:indentation") // Checkstyle doesn't yet handle the exception lambda indentation
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final HttpServerRequest request = aContext.request();
        final String manifestId = request.getParam(Constants.MANIFEST_ID);
        final String manifestS3Key = IDUtils.getWorkS3Key(manifestId);

        // set a very permissive CORS response header
        response.headers().set(Constants.CORS_HEADER, Constants.STAR);

        try {
            myS3Client.get(myS3Bucket, manifestS3Key, getResponse -> {
                final int statusCode = getResponse.statusCode();
                final String statusMessage = getResponse.statusMessage();
                final String message;

                switch (statusCode) {
                    case HTTP.OK:
                        getResponse.bodyHandler(bodyHandler -> {
                            final String manifest = bodyHandler.getString(0, bodyHandler.length());

                            response.setStatusCode(HTTP.OK);
                            response.putHeader(Constants.CONTENT_TYPE, Constants.JSON_MEDIA_TYPE);
                            response.end(manifest);
                        });

                        break;
                    case HTTP.NOT_FOUND:
                        message = LOGGER.getMessage(MessageCodes.MFS_009, manifestId, statusCode, statusMessage);
                        response.setStatusCode(HTTP.NOT_FOUND);
                        response.setStatusMessage(message);
                        response.end(message);

                        break;
                    case HTTP.INTERNAL_SERVER_ERROR:
                        message = LOGGER.getMessage(MessageCodes.MFS_010, manifestId, statusCode, statusMessage);
                        response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                        response.setStatusMessage(message);
                        response.end(message);

                        LOGGER.error(message);

                        break;
                    default:
                        message = LOGGER.getMessage(MessageCodes.MFS_011, manifestId, statusCode, statusMessage);
                        response.setStatusCode(statusCode);
                        response.setStatusMessage(message);
                        response.end(message);

                        LOGGER.error(message);
                }
            }, exception -> {
                final String message = LOGGER.getMessage(MessageCodes.MFS_009, manifestId, HTTP.INTERNAL_SERVER_ERROR,
                        exception.getMessage());

                response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                response.setStatusMessage(message);
                response.end(message);

                LOGGER.error(exception, message);
            });
        } catch (final Throwable aThrowable) {
            final String message = LOGGER.getMessage(MessageCodes.MFS_009, manifestId, HTTP.INTERNAL_SERVER_ERROR,
                    aThrowable.getMessage());

            response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
            response.setStatusMessage(message);
            response.end(message);

            LOGGER.error(message);
        }
    }

}
