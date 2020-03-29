
package edu.ucla.library.iiif.fester.verticles;

import static edu.ucla.library.iiif.fester.Constants.MESSAGES;

import java.util.ArrayList;
import java.util.List;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.handlers.EndpointConfigHandler;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;

/**
 * The verticle that starts the Fester service.
 */
public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class, MESSAGES);

    private static final String DEFAULT_SPEC = "fester.yaml";

    private static final int DEFAULT_PORT = 8888;

    /**
     * Starts the Fester service.
     */
    @Override
    public void start(final Promise<Void> aPromise) {
        getConfigRetriever().getConfig(configuration -> {
            if (configuration.failed()) {
                aPromise.fail(configuration.cause());
            } else {
                final JsonObject config = configuration.result();
                final String apiSpec = config.getString(Config.OPENAPI_SPEC_PATH, DEFAULT_SPEC);
                final Promise<Router> promise = Promise.promise();

                // We want the deployment configuration to override other configuration values
                config.mergeIn(config());

                // Set up the server after we've configured the router
                promise.future().setHandler(handler -> {
                    if (handler.succeeded()) {
                        final int port = config.getInteger(Config.HTTP_PORT, DEFAULT_PORT);
                        final HttpServer server = vertx.createHttpServer();
                        final Router router = handler.result();

                        LOGGER.info(MessageCodes.MFS_041, port);

                        // Start our server
                        server.requestHandler(router).listen(port);

                        // Start up our Fester verticles
                        startVerticles(config, aPromise);
                    } else {
                        aPromise.fail(handler.cause());
                    }
                });

                // We can use our OpenAPI specification file to configure our app's router
                OpenAPI3RouterFactory.create(vertx, apiSpec, new EndpointConfigHandler(vertx, config, promise));
            }
        });
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

        return ConfigRetriever.create(vertx, configOptions.addStore(systemConfigs));
    }

    /**
     * Starts Fester's verticles.
     *
     * @param aConfig A application configuration
     * @param aPromise A startup promise
     */
    private void startVerticles(final JsonObject aConfig, final Promise<Void> aPromise) {
        final DeploymentOptions uploaderOptions = new DeploymentOptions();
        final DeploymentOptions manifestorOptions = new DeploymentOptions();
        final List<Future> futures = new ArrayList<>();

        uploaderOptions.setConfig(aConfig);
        manifestorOptions.setWorker(true).setWorkerPoolName(ManifestVerticle.class.getSimpleName());
        manifestorOptions.setWorkerPoolSize(1).setConfig(aConfig);

        // Start up any necessary Fester verticles
        futures.add(deployVerticle(ManifestVerticle.class.getName(), manifestorOptions, Promise.promise()));
        futures.add(deployVerticle(S3BucketVerticle.class.getName(), uploaderOptions, Promise.promise()));

        // Confirm all our verticles were successfully deployed
        CompositeFuture.all(futures).setHandler(handler -> {
            if (handler.succeeded()) {
                aPromise.complete();
            } else {
                aPromise.fail(handler.cause());
            }
        });
    }

    /**
     * Deploys a particular verticle.
     *
     * @param aVerticleName The name of the verticle to deploy
     * @param aOptions Any deployment options that should be considered
     * @param aPromise A promise to deploy the requested verticle
     */
    private Future<Void> deployVerticle(final String aVerticleName, final DeploymentOptions aOptions,
            final Promise<Void> aPromise) {
        vertx.deployVerticle(aVerticleName, aOptions, response -> {
            try {
                final String verticleName = Class.forName(aVerticleName).getSimpleName();

                if (response.succeeded()) {
                    LOGGER.debug(MessageCodes.MFS_116, verticleName, response.result());
                    aPromise.complete();
                } else {
                    LOGGER.error(MessageCodes.MFS_117, verticleName, response.cause());
                    aPromise.fail(response.cause());
                }
            } catch (final ClassNotFoundException details) {
                aPromise.fail(details);
            }
        });

        return aPromise.future();
    }
}
