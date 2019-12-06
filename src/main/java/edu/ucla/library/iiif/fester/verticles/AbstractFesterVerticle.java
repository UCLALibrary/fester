
package edu.ucla.library.iiif.fester.verticles;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;

public abstract class AbstractFesterVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFesterVerticle.class, Constants.MESSAGES);

    @SuppressWarnings({ "deprecation" })
    @Override
    public void start(final Future<Void> aFuture) throws Exception {
        LOGGER.debug(MessageCodes.MFS_110, getClass().getName(), deploymentID());

        // Register our verticle name with its deployment ID.
        final LocalMap<String, String> verticleMap = vertx.sharedData().getLocalMap(Constants.VERTICLE_MAP);
        final String verticleName = getClass().getSimpleName();

        // Add a deployment ID to the verticle map
        if (verticleMap.containsKey(verticleName)) {
            verticleMap.put(verticleName, verticleMap.get(verticleName) + "|" + deploymentID());
        } else {
            verticleMap.put(verticleName, deploymentID());
        }

        aFuture.complete();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void stop(final Future<Void> aFuture) {
        LOGGER.debug(MessageCodes.MFS_100, getClass().getName(), deploymentID());
        aFuture.complete();
    }

    protected MessageConsumer<JsonObject> getJsonConsumer() {
        LOGGER.debug(MessageCodes.MFS_101, getClass().getName());
        return vertx.eventBus().consumer(getClass().getName());
    }

    /**
     * Sends a message to another verticle with a supplied timeout value.
     *
     * @param aJsonObject A JSON message
     * @param aVerticleName A verticle name that will respond to the message
     * @param aTimeout A timeout measured in milliseconds
     * @param aHandler A handler to handle the result of the message delivery
     */
    protected void sendMessage(final JsonObject aJsonObject, final String aVerticleName, final long aTimeout,
            final Handler<AsyncResult<Message<JsonObject>>> aHandler) {
        final DeliveryOptions options = new DeliveryOptions().setSendTimeout(aTimeout);

        LOGGER.debug(MessageCodes.MFS_102, aVerticleName, aJsonObject.encode());
        vertx.eventBus().request(aVerticleName, aJsonObject, options, aHandler);
    }

    /**
     * Send a message to another verticle.
     *
     * @param aJsonObject A JSON message
     * @param aVerticleName A verticle name that will respond to the message
     * @param aHandler A handler to handle the result of the message delivery
     */
    protected void sendMessage(final JsonObject aJsonObject, final String aVerticleName,
            final Handler<AsyncResult<Message<JsonObject>>> aHandler) {
        sendMessage(aJsonObject, aVerticleName, DeliveryOptions.DEFAULT_TIMEOUT, aHandler);
    }

}
