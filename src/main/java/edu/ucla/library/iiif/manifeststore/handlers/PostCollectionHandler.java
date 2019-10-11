
package edu.ucla.library.iiif.manifeststore.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.manifeststore.Constants;
import edu.ucla.library.iiif.manifeststore.HTTP;
import edu.ucla.library.iiif.manifeststore.MessageCodes;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that handles POSTs wanting to generate collection manifests.
 */
public class PostCollectionHandler extends AbstractManifestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostCollectionHandler.class, Constants.MESSAGES);

    private final String myExceptionPage;

    private final String mySuccessPage;

    /**
     * Creates a handler to handle POSTs to generate collection manifests.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A application configuration
     * @throws IOException If there is trouble reading the HTML template files
     */
    public PostCollectionHandler(final Vertx aVertx, final JsonObject aConfig) throws IOException {
        super(aVertx, aConfig);

        final StringBuilder templateBuilder = new StringBuilder();

        // Load a template used for returning the error page
        InputStream templateStream = getClass().getResourceAsStream("/webroot/error.html");
        BufferedReader templateReader = new BufferedReader(new InputStreamReader(templateStream));
        String line;

        while ((line = templateReader.readLine()) != null) {
            templateBuilder.append(line);
        }

        templateReader.close();
        myExceptionPage = templateBuilder.toString();

        // Load a template used for returning the success page
        templateBuilder.delete(0, templateBuilder.length());
        templateStream = getClass().getResourceAsStream("/webroot/success.html");
        templateReader = new BufferedReader(new InputStreamReader(templateStream));

        while ((line = templateReader.readLine()) != null) {
            templateBuilder.append(line);
        }

        templateReader.close();
        mySuccessPage = templateBuilder.toString();
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final Set<FileUpload> csvUploads = aContext.fileUploads();

        // An uploaded CSV is required
        if (csvUploads.size() == 0) {
            response.setStatusCode(HTTP.BAD_REQUEST);
            response.setStatusMessage(LOGGER.getMessage(MessageCodes.MFS_037));
            response.putHeader(Constants.CONTENT_TYPE, Constants.HTML_MEDIA_TYPE);
            response.end(StringUtils.format(myExceptionPage, LOGGER.getMessage(MessageCodes.MFS_037)));
        } else {
            final FileUpload csvFile = csvUploads.iterator().next();
            final String filePath = csvFile.uploadedFileName();
            final String fileName = csvFile.fileName();
            final String message = LOGGER.getMessage(MessageCodes.MFS_038, fileName, filePath);

            response.setStatusCode(HTTP.CREATED);
            response.setStatusMessage(message);
            response.putHeader(Constants.CONTENT_TYPE, Constants.HTML_MEDIA_TYPE);
            response.end(StringUtils.format(mySuccessPage, message));
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
