
package edu.ucla.library.iiif.manifeststore.handlers;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.manifeststore.Constants;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * A manifest creator or updater.
 */
public class PutManifestHandler extends AbstractManifestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutManifestHandler.class, Constants.MESSAGES);

    /**
     * Creates a handler that creates or updates IIIF manifests in the manifest store.
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

        response.setStatusCode(200);
        response.putHeader("content-type", "text/plain").end("Success!");
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
