
package edu.ucla.library.iiif.manifeststore.handlers;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class GetStatusHandler implements Handler<RoutingContext> {

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();

        response.setStatusCode(200);
        response.putHeader("content-type", "text/plain").end("Hello");
    }

}
