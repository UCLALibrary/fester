
package edu.ucla.library.iiif.fester.handlers;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import com.nike.moirai.ConfigFeatureFlagChecker;
import com.nike.moirai.FeatureFlagChecker;
import com.nike.moirai.Suppliers;
import com.nike.moirai.resource.FileResourceLoaders;
import com.nike.moirai.typesafeconfig.TypesafeConfigDecider;
import com.nike.moirai.typesafeconfig.TypesafeConfigReader;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.Features;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * An OpenAPI endpoint configuration handler.
 */
public class EndpointConfigHandler implements Handler<AsyncResult<OpenAPI3RouterFactory>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointConfigHandler.class, Constants.MESSAGES);

    private static final String FEATURE_FLAGS_FILE = "/etc/fester/fester-features.conf";

    private static final String BATCH_UPLOAD_FORM = "/fester/upload/csv";

    private final Promise<Router> myPromise;

    private final JsonObject myConfig;

    private final Vertx myVertx;

    /**
     * Creates a new endpoint configuration handler.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig An application configuration
     * @param aPromise A router configuration promise
     */
    public EndpointConfigHandler(final Vertx aVertx, final JsonObject aConfig, final Promise<Router> aPromise) {
        myPromise = aPromise;
        myConfig = aConfig;
        myVertx = aVertx;
    }

    @Override
    @SuppressWarnings("checkstyle:indentation") // Checkstyle can't handle the ifPresentOrElse syntax
    public void handle(final AsyncResult<OpenAPI3RouterFactory> aConfiguration) {
        final Optional<FeatureFlagChecker> featureFlagChecker = getFeatureFlagChecker();

        if (aConfiguration.succeeded()) {
            final OpenAPI3RouterFactory factory = aConfiguration.result();
            final Promise<Boolean> promise = Promise.promise();

            // We need to associate endpoint handlers with routes from our specification
            factory.addHandlerByOperationId(Op.GET_STATUS, new GetStatusHandler());
            factory.addHandlerByOperationId(Op.GET_MANIFEST, new GetManifestHandler(myVertx, myConfig));
            factory.addHandlerByOperationId(Op.PUT_MANIFEST, new PutManifestHandler(myVertx, myConfig));
            factory.addHandlerByOperationId(Op.DELETE_MANIFEST, new DeleteManifestHandler(myVertx, myConfig));
            factory.addHandlerByOperationId(Op.GET_COLLECTION, new GetCollectionHandler(myVertx, myConfig));
            factory.addHandlerByOperationId(Op.PUT_COLLECTION, new PutCollectionHandler(myVertx, myConfig));
            factory.addHandlerByOperationId(Op.CHECK_ENDPOINTS, new CheckEndpointsHandler(myVertx, myConfig));

            // After the batch ingest feature is configured (or not), we complete the router configuration
            promise.future().onComplete(handler -> {
                if (handler.succeeded()) {
                    configureRouter(factory, handler.result());
                } else {
                    myPromise.fail(handler.cause());
                }
            });

            // If we have a feature checker, check it; else, go ahead and configure the ingest endpoint
            featureFlagChecker.ifPresentOrElse(checker -> {
                if (checker.isFeatureEnabled(Features.BATCH_INGEST)) {
                    configureBatchIngest(factory, myConfig, promise);
                } else {
                    LOGGER.info(MessageCodes.MFS_086, Features.getDisplayName(Features.BATCH_INGEST));
                    configureBatchIngestPlaceholder(factory, promise);
                }
            }, () -> {
                configureBatchIngest(factory, myConfig, promise);
            });
        } else {
            myPromise.fail(aConfiguration.cause());
        }
    }

    /**
     * Configure the router with additional paths that are not handled by the OpenAPI router factory
     *
     * @param aFactory An OpenAPI router factory
     * @param aBatchIngestEnabled Whether the batch ingest is disabled or enabled
     */
    private void configureRouter(final OpenAPI3RouterFactory aFactory, final boolean aBatchIngestEnabled) {
        final StaticHandler staticHandler = StaticHandler.create().setWebRoot("webroot");
        final Router router = aFactory.getRouter();
        final FeatureOffHandler featureOffHandler;

        try {
            router.getWithRegex("/fester/?").last().handler(event -> {
                event.reroute("/fester/docs");
            });

            router.getWithRegex("/fester/upload/?").last().handler(event -> {
                event.reroute(BATCH_UPLOAD_FORM);
            });

            if (!aBatchIngestEnabled) {
                featureOffHandler = new FeatureOffHandler(myVertx, myConfig, Features.BATCH_INGEST);
                router.get(BATCH_UPLOAD_FORM).handler(featureOffHandler);
            }

            // Serve Fester HTML pages
            router.get("/fester*").handler(staticHandler.setIndexPage("index.html"));

            // If an incoming request doesn't match one of our spec operations, it's treated as a 404;
            // catch these generic 404s with the handler below and return more specific response codes
            router.errorHandler(HTTP.NOT_FOUND, new MatchingOpNotFoundHandler());

            // Indicate we're done configuring the router
            myPromise.complete(router);
        } catch (final IOException details) {
            myPromise.fail(details);
        }
    }

    /**
     * Configure the ingest endpoint.
     *
     * @param aFactory A router factory
     * @param aConfig An application configuration
     * @param aPromise A configuration promise
     */
    private void configureBatchIngest(final OpenAPI3RouterFactory aFactory, final JsonObject aConfig,
            final Promise<Boolean> aPromise) {
        try {
            final PostCsvHandler postHandler = new PostCsvHandler(myVertx, aConfig);
            final BodyHandler bodyHandlerCSV = BodyHandler.create().setDeleteUploadedFilesOnEnd(true);

            final PostThumbnailsHandler thumbHandler = new PostThumbnailsHandler(myVertx, aConfig);
            final BodyHandler bodyHandlerThumb = BodyHandler.create().setDeleteUploadedFilesOnEnd(true);

            final PostZipHandler zipHandler = new PostZipHandler(myVertx, aConfig);
            final BodyHandler bodyHandlerZip = BodyHandler.create().setDeleteUploadedFilesOnEnd(true);

            aFactory.addHandlerByOperationId(Op.POST_CSV, postHandler).setBodyHandler(bodyHandlerCSV);
            aFactory.addHandlerByOperationId(Op.POST_THUMB, thumbHandler).setBodyHandler(bodyHandlerThumb);
            aFactory.addHandlerByOperationId(Op.POST_ZIP, zipHandler).setBodyHandler(bodyHandlerZip);

            aPromise.complete(true);
        } catch (final IOException details) {
            aPromise.fail(details);
        }
    }

    /**
     * Configure a placeholder for the ingest endpoint
     *
     * @param aFactory A router factory
     * @param aPromise A configuration promise
     */
    private void configureBatchIngestPlaceholder(final OpenAPI3RouterFactory aFactory,
            final Promise<Boolean> aPromise) {
        aFactory.addHandlerByOperationId(Op.POST_CSV, handler -> {
            final String featureName = Features.getDisplayName(Features.BATCH_INGEST);
            final String message = LOGGER.getMessage(MessageCodes.MFS_085, featureName);

            handler.response().setStatusCode(HTTP.SERVICE_UNAVAILABLE).setStatusMessage(message).end();
        });

        aPromise.complete(false);
    }

    /**
     * Gets a feature flag checker.
     *
     * @return An optional feature flag checker
     */
    private Optional<FeatureFlagChecker> getFeatureFlagChecker() {
        if (myVertx.fileSystem().existsBlocking(FEATURE_FLAGS_FILE)) {
            return Optional.of(ConfigFeatureFlagChecker.forConfigSupplier(
                    Suppliers.supplierAndThen(FileResourceLoaders.forFile(new File(FEATURE_FLAGS_FILE)),
                            TypesafeConfigReader.FROM_STRING),
                    TypesafeConfigDecider.FEATURE_ENABLED));
        } else {
            return Optional.empty();
        }
    }
}
