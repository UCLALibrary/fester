
package edu.ucla.library.iiif.fester.verticles;

import static edu.ucla.library.iiif.fester.Constants.MESSAGES;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.handlers.DeleteManifestHandler;
import edu.ucla.library.iiif.fester.handlers.GetManifestHandler;
import edu.ucla.library.iiif.fester.handlers.GetStatusHandler;
import edu.ucla.library.iiif.fester.handlers.MatchingOpNotFoundHandler;
import edu.ucla.library.iiif.fester.handlers.PostCsvHandler;
import edu.ucla.library.iiif.fester.handlers.PutManifestHandler;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
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
    @SuppressWarnings({ "deprecation" })
    public void start(final Future<Void> aFuture) {
        final JsonObject deploymentConfig = config();
        final HttpServer server = vertx.createHttpServer();

        // We pull our application's configuration before configuring the server
        getConfigRetriever().getConfig(configuration -> {
            if (configuration.failed()) {
                aFuture.fail(configuration.cause());
            } else {
                final JsonObject config = configuration.result();
                final String apiSpec = config.getString(Config.OPENAPI_SPEC_PATH, DEFAULT_SPEC);

                // We want the deployment configuration to override other configuration values
                config.mergeIn(deploymentConfig);

                // We can use our OpenAPI specification file to configure our app's router
                OpenAPI3RouterFactory.create(vertx, apiSpec, creation -> {
                    if (creation.succeeded()) {
                        final OpenAPI3RouterFactory factory = creation.result();
                        final Vertx vertx = getVertx();
                        final Router router;

                        // Next, we associate handlers with routes from our specification
                        factory.addHandlerByOperationId(Op.GET_STATUS, new GetStatusHandler());
                        factory.addHandlerByOperationId(Op.GET_MANIFEST, new GetManifestHandler(vertx, config));
                        factory.addHandlerByOperationId(Op.PUT_MANIFEST, new PutManifestHandler(vertx, config));
                        factory.addHandlerByOperationId(Op.DELETE_MANIFEST, new DeleteManifestHandler(vertx, config));

                        try {
                            final int port = config.getInteger(Config.HTTP_PORT, DEFAULT_PORT);
                            final PostCsvHandler postCsvHandler = new PostCsvHandler(vertx, config);

                            factory.addHandlerByOperationId(Op.POST_CSV, postCsvHandler);

                            // After that, we can get a router that's been configured by our OpenAPI spec
                            router = factory.getRouter();

                            // Serve Fester documentation
                            router.get("/docs/fester/*").handler(StaticHandler.create().setWebRoot("webroot"));

                            // If an incoming request doesn't match one of our spec operations, it's treated as a 404;
                            // catch these generic 404s with the handler below and return more specific response codes
                            router.errorHandler(HTTP.NOT_FOUND, new MatchingOpNotFoundHandler());

                            LOGGER.info(MessageCodes.MFS_041, port);

                            // Start our server
                            server.requestHandler(router).listen(port);

                            // Start up our Fester verticles
                            startVerticles(config, aFuture);
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

    // Start verticles -- this is where to add any new verticles that we create and want to load
    @SuppressWarnings({ "deprecation" })
    private void startVerticles(final JsonObject aConfig, final Future<Void> aFuture) {
        final DeploymentOptions uploaderOptions = new DeploymentOptions();
        final DeploymentOptions manifestorOptions = new DeploymentOptions();
        final List<Future> futures = new ArrayList<>();

        uploaderOptions.setConfig(aConfig);
        manifestorOptions.setWorker(true).setWorkerPoolName(ManifestVerticle.class.getSimpleName());
        manifestorOptions.setWorkerPoolSize(5).setConfig(aConfig);

        // Start up any necessary Fester verticles
        futures.add(deployVerticle(ManifestVerticle.class.getName(), manifestorOptions, Future.future()));
        futures.add(deployVerticle(S3BucketVerticle.class.getName(), uploaderOptions, Future.future()));

        // Confirm all our verticles were successfully deployed
        CompositeFuture.all(futures).setHandler(handler -> {
            if (handler.succeeded()) {
                aFuture.complete();
            } else {
                aFuture.fail(handler.cause());
            }
        });
    }

    /**
     * Deploys a particular verticle.
     *
     * @param aVerticleName The name of the verticle to deploy
     * @param aOptions Any deployment options that should be considered
     */
    @SuppressWarnings({ "deprecation" })
    private Future<Void> deployVerticle(final String aVerticleName, final DeploymentOptions aOptions,
            final Future<Void> aFuture) {
        vertx.deployVerticle(aVerticleName, aOptions, response -> {
            try {
                final String verticleName = Class.forName(aVerticleName).getSimpleName();

                if (response.succeeded()) {
                    LOGGER.debug(MessageCodes.MFS_116, verticleName, response.result());
                    aFuture.complete();
                } else {
                    LOGGER.error(MessageCodes.MFS_117, verticleName, response.cause());
                    aFuture.fail(response.cause());
                }
            } catch (final ClassNotFoundException details) {
                aFuture.fail(details);
            }
        });

        return aFuture;
    }

    /**
     * Gets a configuration retriever that allows system properties to override the configuration file.
     *
     * @return A configuration retriever
     */
    private ConfigRetriever getConfigRetriever() {
        final String configFilePath = StringUtils.trimToNull(System.getProperty("vertx-config-path"));
        final ConfigRetrieverOptions configOptions = new ConfigRetrieverOptions().setIncludeDefaultStores(false);
        final ConfigStoreOptions fileConfigs = new ConfigStoreOptions().setType("file").setFormat("properties");
        final ConfigStoreOptions systemConfigs = new ConfigStoreOptions().setType("sys");

        if (configFilePath != null) {
            configOptions.addStore(fileConfigs.setConfig(new JsonObject().put("path", configFilePath)));
        } else {
            LOGGER.warn(MessageCodes.MFS_040);
        }

        configOptions.addStore(systemConfigs);

        return ConfigRetriever.create(vertx, configOptions);
    }
}
