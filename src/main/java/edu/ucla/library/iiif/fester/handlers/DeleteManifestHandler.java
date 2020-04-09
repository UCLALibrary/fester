
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
 * A IIIF manifest deleter.
 */
public class DeleteManifestHandler extends AbstractFesterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteManifestHandler.class, Constants.MESSAGES);

    /**
     * Creates a handler that deletes IIIF manifests from Fester.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A JSON configuration
     */
    public DeleteManifestHandler(final Vertx aVertx, final JsonObject aConfig) {
        super(aVertx, aConfig);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final HttpServerRequest request = aContext.request();
        final String manifestID = request.getParam(Constants.MANIFEST_ID);
        final String manifestS3Key = IDUtils.getWorkS3Key(manifestId);

        myS3Client.delete(myS3Bucket, manifestS3Key, deleteResponse -> {
            final int statusCode = deleteResponse.statusCode();

            switch (statusCode) {
                case HTTP.SUCCESS_NO_CONTENT:
                    response.setStatusCode(HTTP.SUCCESS_NO_CONTENT);
                    response.putHeader(Constants.CONTENT_TYPE, Constants.PLAIN_TEXT_TYPE);
                    response.end(LOGGER.getMessage(MessageCodes.MFS_088, manifestID));

                    break;
                case HTTP.FORBIDDEN:
                    response.setStatusCode(HTTP.FORBIDDEN);
                    response.putHeader(Constants.CONTENT_TYPE, Constants.PLAIN_TEXT_TYPE);
                    response.end(LOGGER.getMessage(MessageCodes.MFS_089, manifestID));

                    break;
                case HTTP.INTERNAL_SERVER_ERROR:
                    final String serverErrorMessage = LOGGER.getMessage(MessageCodes.MFS_014, manifestID);

                    LOGGER.error(serverErrorMessage);

                    response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                    response.putHeader(Constants.CONTENT_TYPE, Constants.PLAIN_TEXT_TYPE);
                    response.end(serverErrorMessage);

                    break;
                default:
                    final String genericErrorMessage = LOGGER.getMessage(MessageCodes.MFS_013, manifestID);

                    LOGGER.warn(genericErrorMessage);

                    response.setStatusCode(statusCode);
                    response.putHeader(Constants.CONTENT_TYPE, Constants.PLAIN_TEXT_TYPE);
                    response.end(genericErrorMessage);
            }
        });
    }

}
