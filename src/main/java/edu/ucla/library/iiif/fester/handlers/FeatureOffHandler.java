
package edu.ucla.library.iiif.fester.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import info.freelibrary.util.IOUtils;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.Features;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that shows a informational page when a feature is turned off.
 */
public class FeatureOffHandler extends AbstractFesterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureOffHandler.class, Constants.MESSAGES);

    /** A template HTML page for the feature off notification */
    private static final String TEMPLATE = "/webroot/feature-off.html";

    /** The contents of the HTML page for the feature off notification */
    private final String myHtmlPage;

    /** A feature that's turned off */
    private final String myFeatureKey;

    /**
     * Creates a handler for a feature that's been turned off.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig An application configuration
     * @param aFeatureKey The key of the feature that's turned off
     * @throws IOException If there is trouble reading the page template
     */
    public FeatureOffHandler(final Vertx aVertx, final JsonObject aConfig, final String aFeatureKey)
            throws IOException {
        super(aVertx, aConfig);

        myHtmlPage = new String(IOUtils.readBytes(getClass().getResourceAsStream(TEMPLATE)), StandardCharsets.UTF_8);
        myFeatureKey = aFeatureKey;
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final String featureName = Features.getDisplayName(myFeatureKey);

        response.setStatusCode(HTTP.OK);
        response.setStatusMessage(LOGGER.getMessage(MessageCodes.MFS_085, featureName));
        response.putHeader(Constants.CONTENT_TYPE, Constants.HTML_MEDIA_TYPE);
        response.end(StringUtils.format(myHtmlPage, featureName));
    }

}
