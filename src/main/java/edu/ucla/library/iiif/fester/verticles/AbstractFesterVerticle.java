
package edu.ucla.library.iiif.fester.verticles;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;

public abstract class AbstractFesterVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFesterVerticle.class, Constants.MESSAGES);

    @Override
    public void start() throws Exception {
        LOGGER.debug(MessageCodes.MFS_110, getClass().getName(), deploymentID());

        // Register our verticle name with its deployment ID.
        final LocalMap<String, String> verticleMap = vertx.sharedData().getLocalMap(Constants.VERTICLE_MAP);
        final String verticleName = getClass().getSimpleName();
        final String deploymentID = deploymentID();

        LOGGER.debug(MessageCodes.MFS_076, verticleName, deploymentID);

        // Add a deployment ID to the verticle map
        if (verticleMap.containsKey(verticleName)) {
            verticleMap.put(verticleName, verticleMap.get(verticleName) + "|" + deploymentID);
        } else {
            verticleMap.put(verticleName, deploymentID);
        }
    }

    @Override
    public void stop() {
        LOGGER.debug(MessageCodes.MFS_100, getClass().getName(), deploymentID());
    }

    /**
     * Gets the verticle's JSON consumer.
     *
     * @return A message consumer for JSON objects
     */
    protected MessageConsumer<JsonObject> getJsonConsumer() {
        LOGGER.debug(MessageCodes.MFS_101, getClass().getName());
        return vertx.eventBus().consumer(getClass().getName());
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
        vertx.eventBus().request(aVerticleName, aMessage, aHeaders, aHandler);
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
