
package edu.ucla.library.iiif.fester.handlers;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler for requests that do not match the operations defined in the fester OpenAPI specification.
 * <p>
 * This handler can fail the context with any response code other than a 404. Because it's set up to catch 404s,
 * failing the routing context with a 404 will create a loop. To return a legitimate 404 from this handler, use
 * HttpServerReponse and set 404 as the response code, remembering to call <code>.end()</code> on the response.
 */
public class MatchingOpNotFoundHandler implements Handler<RoutingContext> {

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerRequest request = aContext.request();
        final HttpServerResponse response = aContext.response();
        final HttpMethod method = request.method();

        if (method.equals(HttpMethod.PUT)) {
            handlePuts(aContext);
        } else {
            response.setStatusCode(HTTP.NOT_FOUND).end();
        }
    }

    private void handlePuts(final RoutingContext aContext) {
        final HttpServerRequest request = aContext.request();
        final HttpServerResponse response = aContext.response();

        final String mediaType = request.getHeader(Constants.CONTENT_TYPE);

        if (mediaType != null && !mediaType.equals(Constants.JSON_MEDIA_TYPE)) {
            aContext.fail(HTTP.UNSUPPORTED_MEDIA_TYPE, new IncorrectMediaTypeException(mediaType));
        } else if (mediaType == null) {
            aContext.fail(HTTP.UNSUPPORTED_MEDIA_TYPE, new MissingMediaTypeException());
        } else {
            response.setStatusCode(HTTP.NOT_FOUND).end();
        }
    }
}
