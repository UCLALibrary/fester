
package edu.ucla.library.iiif.manifeststore.handlers;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * A IIIF manifest creator.
 */
public class PostManifestHandler extends AbstractManifestHandler {

    /**
     * Creates a handler that creates IIIF manifests in the manifest store.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A JSON configuration
     */
    public PostManifestHandler(final Vertx aVertx, final JsonObject aConfig) {
        super(aVertx, aConfig);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();

        response.setStatusCode(200);
        response.putHeader("content-type", "text/plain").end("Success!");
    }

}
