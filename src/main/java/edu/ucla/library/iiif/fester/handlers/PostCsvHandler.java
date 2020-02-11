
package edu.ucla.library.iiif.fester.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.LinkUtils;
import edu.ucla.library.iiif.fester.verticles.ManifestVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that handles POSTs wanting to generate collection manifests.
 */
public class PostCsvHandler extends AbstractFesterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostCsvHandler.class, Constants.MESSAGES);

    private final String myExceptionPage;

    /**
     * Creates a handler to handle POSTs to generate collection manifests.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A application configuration
     * @throws IOException If there is trouble reading the HTML template files
     */
    public PostCsvHandler(final Vertx aVertx, final JsonObject aConfig) throws IOException {
        super(aVertx, aConfig);

        final StringBuilder templateBuilder = new StringBuilder();

        // Load a template used for returning the error page
        final InputStream templateStream = getClass().getResourceAsStream("/webroot/error.html");
        final BufferedReader templateReader = new BufferedReader(new InputStreamReader(templateStream));

        String line;

        while ((line = templateReader.readLine()) != null) {
            templateBuilder.append(line);
        }

        templateReader.close();
        myExceptionPage = templateBuilder.toString();
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final Set<FileUpload> csvUploads = aContext.fileUploads();

        // An uploaded CSV is required
        if (csvUploads.size() == 0) {
            final String errorMessage = LOGGER.getMessage(MessageCodes.MFS_037);

            response.setStatusCode(HTTP.BAD_REQUEST);
            response.setStatusMessage(errorMessage);
            response.putHeader(Constants.CONTENT_TYPE, Constants.HTML_MEDIA_TYPE);
            response.end(StringUtils.format(myExceptionPage, errorMessage));
        } else {
            final FileUpload csvFile = csvUploads.iterator().next();
            final String filePath = csvFile.uploadedFileName();
            final String fileName = csvFile.fileName();
            final JsonObject message = new JsonObject();
            final DeliveryOptions options = new DeliveryOptions();
            final HttpServerRequest request = aContext.request();
            final String protocol = request.connection().isSsl() ? "https://" : "http://";
            final String iiifHost = StringUtils.trimToNull(request.getFormAttribute(Constants.IIIF_HOST));
            final String festerHost = protocol + request.host();

            // Store the information that the manifest generator will need
            message.put(Constants.CSV_FILE_NAME, fileName).put(Constants.CSV_FILE_PATH, filePath);
            message.put(Constants.FESTER_HOST, festerHost);
            options.addHeader(Constants.ACTION, Op.POST_CSV);

            if (iiifHost != null) {
                message.put(Constants.IIIF_HOST, iiifHost);
            }

            // Send a message to the manifest generator
            sendMessage(ManifestVerticle.class.getName(), message, options, Integer.MAX_VALUE, send -> {
                if (send.succeeded()) {
                    updateCSV(fileName, filePath, festerHost, response);
                } else {
                    returnError(response, send.cause());
                }
            });
        }
    }

    private void updateCSV(final String aFileName, final String aFilePath, final String aHost,
            final HttpServerResponse aResponse) {
        final String responseMessage = LOGGER.getMessage(MessageCodes.MFS_038, aFileName, aFilePath);
        final FileSystem fileSystem = myVertx.fileSystem();

        // Read the uploaded CSV file and updating it before sending it back
        fileSystem.readFile(aFilePath, read -> {
            if (read.succeeded()) {
                final String csvString = read.result().toString(StandardCharsets.UTF_8);
                final CSVReader csvReader = new CSVReader(new StringReader(csvString));
                final StringWriter writer = new StringWriter();
                final CSVWriter csvWriter = new CSVWriter(writer);

                try {
                    csvWriter.writeAll(LinkUtils.addManifests(aHost, csvReader.readAll()));

                    aResponse.setStatusCode(HTTP.CREATED);
                    aResponse.setStatusMessage(responseMessage);
                    aResponse.putHeader(Constants.CONTENT_TYPE, Constants.CSV_MEDIA_TYPE);
                    aResponse.end(Buffer.buffer(writer.toString()));
                } catch (final IOException | CsvException details) {
                    returnError(aResponse, details);
                } finally {
                    IOUtils.closeQuietly(csvReader);
                    IOUtils.closeQuietly(csvWriter);
                }
            } else {
                returnError(aResponse, read.cause());
            }
        });
    }

    /**
     * Return an error page (and response code) to the requester.
     *
     * @param aResponse A HTTP response
     * @param aThrowable A throwable exception
     */
    private void returnError(final HttpServerResponse aResponse, final Throwable aThrowable) {
        final String exceptionMessage = aThrowable.getMessage();
        final String errorMessage = LOGGER.getMessage(MessageCodes.MFS_103, exceptionMessage);

        LOGGER.error(aThrowable, errorMessage);

        aResponse.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
        aResponse.setStatusMessage(exceptionMessage);
        aResponse.putHeader(Constants.CONTENT_TYPE, Constants.HTML_MEDIA_TYPE);
        aResponse.end(StringUtils.format(myExceptionPage, errorMessage));
    }
}
