
package edu.ucla.library.iiif.fester.verticles;

import static edu.ucla.library.iiif.fester.Constants.MESSAGES;

import java.io.IOException;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.handlers.DeleteManifestHandler;
import edu.ucla.library.iiif.fester.handlers.GetManifestHandler;
import edu.ucla.library.iiif.fester.handlers.GetStatusHandler;
import edu.ucla.library.iiif.fester.handlers.MatchingOpNotFoundHandler;
import edu.ucla.library.iiif.fester.handlers.PostCollectionHandler;
import edu.ucla.library.iiif.fester.handlers.PutManifestHandler;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * Main verticle that starts the application.
 */
public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class, MESSAGES);

    private static final String DEFAULT_SPEC = "fester.yaml";

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
                        final OpenAPI3RouterFactory factory = creation.result();
                        final int port = config.getInteger(Config.HTTP_PORT, DEFAULT_PORT);
                        final Vertx vertx = getVertx();
                        final Router router;

                        // Next, we associate handlers with routes from our specification
                        factory.addHandlerByOperationId(Op.GET_STATUS, new GetStatusHandler());
                        factory.addHandlerByOperationId(Op.GET_MANIFEST, new GetManifestHandler(vertx, config));
                        factory.addHandlerByOperationId(Op.PUT_MANIFEST, new PutManifestHandler(vertx, config));
                        factory.addHandlerByOperationId(Op.DELETE_MANIFEST, new DeleteManifestHandler(vertx, config));

                        try {
                            factory.addHandlerByOperationId(Op.POST_COLLECTION, new PostCollectionHandler(vertx,
                                    config));

                            // After that, we can get a router that's been configured by our OpenAPI spec
                            router = factory.getRouter();

                            // Serve Fester documentation
                            router.get("/docs/fester/*").handler(StaticHandler.create().setWebRoot("webroot"));

                            // If an incoming request doesn't match one of our spec operations, it's treated as a 404;
                            // catch these generic 404s with the handler below and return more specific response codes
                            router.errorHandler(HTTP.NOT_FOUND, new MatchingOpNotFoundHandler());

                            // Start our server
                            server.requestHandler(router).listen(port);

                            aFuture.complete();
                        } catch (final IOException details) {
                            LOGGER.error(details, details.getMessage());
                            aFuture.fail(details);
                        }
                    } else {
                        final Throwable exception = creation.cause();

                        LOGGER.error(exception, exception.getMessage());
                        aFuture.fail(exception);
                    }
                });
            }
        });
    }

}
