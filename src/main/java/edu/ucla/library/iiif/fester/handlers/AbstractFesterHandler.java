
package edu.ucla.library.iiif.fester.handlers;

import com.amazonaws.regions.RegionUtils;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import info.freelibrary.vertx.s3.LocalStackEndpoint;
import info.freelibrary.vertx.s3.S3Client;
import info.freelibrary.vertx.s3.S3ClientOptions;
import info.freelibrary.vertx.s3.S3Endpoint;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Creates an abstract handler so that other instantiated handlers can use its S3Client.
 */
abstract class AbstractFesterHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFesterHandler.class, Constants.MESSAGES);

    protected final Vertx myVertx;

    protected S3Client myS3Client;

    protected String myS3Bucket;

    /**
     * An abstract handler that initializes an S3 client.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig An application configuration
     */
    AbstractFesterHandler(final Vertx aVertx, final JsonObject aConfig) {
        if (myS3Client == null) {
            final String s3AccessKey = aConfig.getString(Config.S3_ACCESS_KEY);
            final String s3SecretKey = aConfig.getString(Config.S3_SECRET_KEY);
            final String s3RegionName = aConfig.getString(Config.S3_REGION);
            final S3ClientOptions config = new S3ClientOptions().setCredentials(s3AccessKey, s3SecretKey);

            LOGGER.debug(MessageCodes.MFS_003, s3RegionName);

            if (RegionUtils.getRegion(s3RegionName) != null) {
                final String endpoint = aConfig.getString(Config.S3_ENDPOINT);

                if (endpoint == null) {
                    config.setEndpoint(S3Endpoint.fromRegion(s3RegionName));
                    LOGGER.debug(MessageCodes.MFS_034, config.getEndpoint().getRegion(), "region");
                } else if (Constants.S3_ENDPOINT.equals(endpoint)) {
                    config.setEndpoint(S3Endpoint.US_EAST_1);
                    LOGGER.debug(MessageCodes.MFS_034, S3Endpoint.US_EAST_1.getRegion(), "default");
                } else {
                    config.setEndpoint(new LocalStackEndpoint(endpoint));
                    LOGGER.debug(MessageCodes.MFS_034, endpoint, "supplied");
                }
            }

            myS3Client = new S3Client(aVertx, config);
            myS3Bucket = aConfig.getString(Config.S3_BUCKET);
        }

        myVertx = aVertx;
    }

    /**
     * Sends a message to another verticle with a supplied timeout value.
     *
     * @param aVerticleName A verticle name that will respond to the message
     * @param aMessage A JSON message
     * @param aHeaders Message headers
     * @param aHandler A handler to handle the result of the message delivery
     * @param aTimeout A timeout measured in milliseconds
     */
    protected void sendMessage(final String aVerticleName, final JsonObject aMessage, final DeliveryOptions aHeaders,
            final long aTimeout, final Handler<AsyncResult<Message<JsonObject>>> aHandler) {
        aHeaders.setSendTimeout(aTimeout);

        LOGGER.debug(MessageCodes.MFS_102, aVerticleName, aMessage.encodePrettily());
        myVertx.eventBus().request(aVerticleName, aMessage, aHeaders, aHandler);
    }

    /**
     * Send a message to another verticle.
     *
     * @param aVerticleName A verticle name that will respond to the message
     * @param aMessage A JSON message
     * @param aHeaders Message headers
     * @param aHandler A handler to handle the result of the message delivery
     */
    protected void sendMessage(final String aVerticleName, final JsonObject aMessage, final DeliveryOptions aHeaders,
            final Handler<AsyncResult<Message<JsonObject>>> aHandler) {
        sendMessage(aVerticleName, aMessage, aHeaders, DeliveryOptions.DEFAULT_TIMEOUT, aHandler);
    }
}
