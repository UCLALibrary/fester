
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
 * A IIIF manifest creator.
 */
public class PutManifestHandler extends AbstractFesterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutManifestHandler.class, Constants.MESSAGES);

    /**
     * Creates a handler that creates IIIF manifests in Fester.
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
        final JsonObject body = aContext.getBodyAsJson();
        final String manifestId = request.getParam(Constants.MANIFEST_ID);
        final String manifestS3Key = IDUtils.getWorkS3Key(manifestId);

        // For now we're not going to check if it exists before we overwrite it
        myS3Client.put(myS3Bucket, manifestS3Key, body.toBuffer(), put -> {
            final int statusCode = put.statusCode();

            switch (statusCode) {
                case HTTP.OK:
                    response.setStatusCode(HTTP.OK);
                    response.end();

                    break;
                case HTTP.FORBIDDEN:
                    LOGGER.debug(MessageCodes.MFS_023, manifestId);

                    response.setStatusCode(HTTP.FORBIDDEN);
                    response.end();

                    break;
                case HTTP.INTERNAL_SERVER_ERROR:
                    LOGGER.error(MessageCodes.MFS_015, manifestId);

                    response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                    response.end();

                    break;
                default:
                    LOGGER.warn(MessageCodes.MFS_013, statusCode, manifestId);

                    response.setStatusCode(statusCode);
                    response.end();
            }
        });
    }

}
