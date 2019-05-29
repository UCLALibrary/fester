
package edu.ucla.library.iiif.manifeststore.verticles;

import static edu.ucla.library.iiif.manifeststore.Constants.MESSAGES;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.manifeststore.Config;
import edu.ucla.library.iiif.manifeststore.Op;
import edu.ucla.library.iiif.manifeststore.handlers.GetPingHandler;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;

/**
 * Main verticle that starts the application.
 */
public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class, MESSAGES);

    private static final String DEFAULT_SPEC = "manifeststore.yaml";

    private static final int DEFAULT_PORT = 8888;

    /**
     * Starts a Web server.
     */
    @Override
    public void start(final Future<Void> aFuture) {
        final ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
        final HttpServer server = vertx.createHttpServer();

        // We pull our application's configuration before configuring the server
        configRetriever.getConfig(configuration -> {
            if (configuration.failed()) {
                aFuture.fail(configuration.cause());
            } else {
                final JsonObject config = configuration.result();
                final String apiSpec = config.getString(Config.OPENAPI_SPEC_PATH, DEFAULT_SPEC);

                // We can use our OpenAPI specification file to configure our app's router
                OpenAPI3RouterFactory.create(vertx, apiSpec, creation -> {
                    if (creation.succeeded()) {
                        final OpenAPI3RouterFactory routerFactory = creation.result();
                        final int port = config.getInteger(Config.HTTP_PORT, DEFAULT_PORT);

                        // Next, we associate handlers with routes from our specification
                        routerFactory.addHandlerByOperationId(Op.GET_PING, new GetPingHandler());
                        server.requestHandler(routerFactory.getRouter()).listen(port);

                        aFuture.complete();
                    } else {
                        aFuture.fail(creation.cause());
                    }
                });
            }
        });
    }

}
