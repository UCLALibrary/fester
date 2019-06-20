
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
 * A IIIF manifest creator.
 */
public class PutManifestHandler extends AbstractManifestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutManifestHandler.class, Constants.MESSAGES);

    /**
     * Creates a handler that creates IIIF manifests in the manifest store.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A JSON configuration
     */
    public PutManifestHandler(final Vertx aVertx, final JsonObject aConfig) {
        super(aVertx, aConfig);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final HttpServerRequest request = aContext.request();
        final String manifestID = request.getParam(Constants.MANIFEST_ID);
        final JsonObject body = aContext.getBodyAsJson();

        // For now we're not going to check if it exists before we overwrite it
        myS3Client.put(myS3Bucket, manifestID, body.toBuffer(), put -> {
            final int statusCode = put.statusCode();

            switch (statusCode) {
                case HTTP.OK:
                    response.setStatusCode(HTTP.OK);
                    response.end();

                    break;
                case HTTP.FORBIDDEN:
                    LOGGER.debug(MessageCodes.MFS_023, manifestID);

                    response.setStatusCode(HTTP.FORBIDDEN);
                    response.end();

                    break;
                case HTTP.INTERNAL_SERVER_ERROR:
                    LOGGER.error(MessageCodes.MFS_015, manifestID);

                    response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                    response.end();

                    break;
                default:
                    LOGGER.warn(MessageCodes.MFS_013, statusCode, manifestID);

                    response.setStatusCode(statusCode);
                    response.end();
            }
        });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
