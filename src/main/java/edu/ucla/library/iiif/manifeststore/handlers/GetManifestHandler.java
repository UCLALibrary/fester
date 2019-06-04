
package edu.ucla.library.iiif.manifeststore.handlers;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * A IIIF manifest retriever.
 */
public class GetManifestHandler extends AbstractManifestHandler {

    /**
     * Creates a handler that returns IIIF manifests from the manifest store.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A JSON configuration
     */
    public GetManifestHandler(final Vertx aVertx, final JsonObject aConfig) {
        super(aVertx, aConfig);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();

        response.setStatusCode(200);
        response.putHeader("content-type", "text/plain").end("Success!");
    }

}
