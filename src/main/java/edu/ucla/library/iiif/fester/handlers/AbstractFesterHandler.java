
package edu.ucla.library.iiif.fester.handlers;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.vertx.s3.S3Client;

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
            final Region s3Region = RegionUtils.getRegion(s3RegionName);

            LOGGER.debug(MessageCodes.MFS_003, s3RegionName);

            if (s3Region != null) {
                myS3Client = new S3Client(aVertx, s3AccessKey, s3SecretKey, s3Region.getServiceEndpoint("s3"));
            } else {
                myS3Client = new S3Client(aVertx, s3AccessKey, s3SecretKey);
            }

            myS3Bucket = aConfig.getString(Config.S3_BUCKET);
        }

        myVertx = aVertx;
    }

    /**
     * Sends a message to another verticle with a supplied timeout value.
     *
     * @param aJsonObject A JSON message
     * @param aVerticleName A verticle name that will respond to the message
     * @param aTimeout A timeout measured in milliseconds
     * @param aHandler A handler to handle the result of the message delivery
     */
    protected void sendMessage(final String aVerticleName, final JsonObject aJsonObject, final long aTimeout,
            final Handler<AsyncResult<Message<JsonObject>>> aHandler) {
        final DeliveryOptions options = new DeliveryOptions().setSendTimeout(aTimeout);

        LOGGER.debug(MessageCodes.MFS_102, aVerticleName, aJsonObject.encodePrettily());
        myVertx.eventBus().request(aVerticleName, aJsonObject, options, aHandler);
    }

    /**
     * Send a message to another verticle.
     *
     * @param aJsonObject A JSON message
     * @param aVerticleName A verticle name that will respond to the message
     * @param aHandler A handler to handle the result of the message delivery
     */
    protected void sendMessage(final String aVerticleName, final JsonObject aJsonObject,
            final Handler<AsyncResult<Message<JsonObject>>> aHandler) {
        sendMessage(aVerticleName, aJsonObject, DeliveryOptions.DEFAULT_TIMEOUT, aHandler);
    }

}
