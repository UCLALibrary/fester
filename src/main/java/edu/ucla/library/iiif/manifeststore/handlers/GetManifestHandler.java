
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
        final String manifestID = request.getParam(Constants.MANIFEST_ID);

        myS3Client.get(myS3Bucket, manifestID, getResponse -> {
            final int statusCode = getResponse.statusCode();

            switch (statusCode) {
                case HTTP.OK:
                    getResponse.bodyHandler(bodyHandler -> {
                        final String manifest = bodyHandler.getString(0, bodyHandler.length());

                        response.setStatusCode(HTTP.OK);
                        response.putHeader(Constants.CONTENT_TYPE, Constants.JSON_MEDIA_TYPE).end(manifest);
                    });

                    break;
                case HTTP.NOT_FOUND:
                    response.setStatusCode(HTTP.NOT_FOUND);
                    response.setStatusMessage(LOGGER.getMessage(MessageCodes.MFS_009, manifestID));

                    break;
                case HTTP.INTERNAL_SERVER_ERROR:
                    response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                    response.setStatusMessage(LOGGER.getMessage(MessageCodes.MFS_010, manifestID));

                    break;
                default:
                    response.setStatusCode(statusCode);
                    response.setStatusMessage(LOGGER.getMessage(MessageCodes.MFS_011, manifestID));
            }
        });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
