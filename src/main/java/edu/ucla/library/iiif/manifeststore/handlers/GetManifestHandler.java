
package edu.ucla.library.iiif.manifeststore.handlers;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.manifeststore.Constants;
import edu.ucla.library.iiif.manifeststore.HTTP;
import edu.ucla.library.iiif.manifeststore.MessageCodes;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * A IIIF manifest retriever.
 */
public class GetManifestHandler extends AbstractManifestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetManifestHandler.class, Constants.MESSAGES);

    /**
     * Creates a handler that returns IIIF manifests from the manifest store.
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
        final HttpServerRequest request = aContext.request();
        final String idParam = request.getParam(Constants.MANIFEST_ID);
        final String manifestId;

        // set a very permissive COR response header
        response.headers().set(Constants.COR_HEADER, Constants.STAR);

        // If our manifest ID doesn't end with '.json' add it for third party tool convenience
        manifestId = !idParam.endsWith(Constants.JSON_EXT) ? idParam + Constants.JSON_EXT : idParam;

        myS3Client.get(myS3Bucket, manifestId, getResponse -> {
            final int statusCode = getResponse.statusCode();
            final String statusMessage;

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
                    statusMessage = LOGGER.getMessage(MessageCodes.MFS_009, manifestId);
                    response.setStatusCode(HTTP.NOT_FOUND);
                    response.setStatusMessage(statusMessage);
                    response.end(statusMessage);

                    break;
                case HTTP.INTERNAL_SERVER_ERROR:
                    statusMessage = LOGGER.getMessage(MessageCodes.MFS_010, manifestId);
                    response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                    response.setStatusMessage(statusMessage);
                    response.end(statusMessage);

                    break;
                default:
                    statusMessage = LOGGER.getMessage(MessageCodes.MFS_011, manifestId);
                    response.setStatusCode(statusCode);
                    response.setStatusMessage(statusMessage);
                    response.end(statusMessage);
            }
        });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
