
package edu.ucla.library.iiif.fester.handlers;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import info.freelibrary.util.IOUtils;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.LinkUtils;
import edu.ucla.library.iiif.fester.verticles.ManifestVerticle;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
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

    private static final String ATTACHMENT = "attachment; filename=\"{}\"";

    private static final String BR_TAG = "<br>";

    private final String myExceptionPage;

    private final String myUrl;

    private final String myFesterizeVersion;

    private final Pattern myFesterizeUserAgentPattern;

    /**
     * Creates a handler to handle POSTs to generate collection manifests.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A application configuration
     * @throws IOException If there is trouble reading the HTML template files
     */
    public PostCsvHandler(final Vertx aVertx, final JsonObject aConfig) throws IOException {
        super(aVertx, aConfig);

        final byte[] bytes = IOUtils.readBytes(getClass().getResourceAsStream("/webroot/error.html"));

        myExceptionPage = new String(bytes, StandardCharsets.UTF_8);
        myUrl = aConfig.getString(Config.FESTER_URL);
        myFesterizeVersion = aConfig.getString(Config.FESTERIZE_VERSION);
        myFesterizeUserAgentPattern = Pattern.compile("Festerize/(?<version>\\d+\\.\\d+\\.\\d+)");
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerRequest request = aContext.request();
        final HttpServerResponse response = aContext.response();
        final Set<FileUpload> csvUploads = aContext.fileUploads();
        final String festerizeUserAgent = StringUtils.trimTo(request.getHeader("User-Agent"), Constants.EMPTY);
        final Matcher festerizeUserAgentMatcher = myFesterizeUserAgentPattern.matcher(festerizeUserAgent);
        final String errorMessage;

        // An uploaded CSV is required
        if (csvUploads.size() == 0) {
            errorMessage = LOGGER.getMessage(MessageCodes.MFS_037);

            response.setStatusCode(HTTP.BAD_REQUEST);
            response.setStatusMessage(errorMessage);
            response.putHeader(Constants.CONTENT_TYPE, Constants.HTML_MEDIA_TYPE);
            response.end(StringUtils.format(myExceptionPage, errorMessage));
        } else if (festerizeUserAgentMatcher.matches() &&
                !festerizeUserAgentMatcher.group("version").equals(myFesterizeVersion)) { // Festerize version mismatch
            errorMessage = LOGGER.getMessage(MessageCodes.MFS_147, myFesterizeVersion);

            response.setStatusCode(HTTP.BAD_REQUEST);
            response.setStatusMessage(errorMessage);
            response.putHeader(Constants.CONTENT_TYPE, Constants.HTML_MEDIA_TYPE);
            response.end(StringUtils.format(myExceptionPage, errorMessage));
        } else {
            final FileUpload csvFile = csvUploads.iterator().next();
            final DeliveryOptions options = new DeliveryOptions();
            final String filePath = csvFile.uploadedFileName();
            final String fileName = csvFile.fileName();
            final JsonObject message = new JsonObject();
            final String iiifHost = StringUtils.trimToNull(request.getFormAttribute(Constants.IIIF_HOST));
            final String iiifVersion = StringUtils.trimToNull(request.getFormAttribute(Constants.IIIF_API_VERSION));
            final boolean update = StringUtils.trimToBool(request.getFormAttribute(Constants.METADATA_UPDATE), false);

            // Store the information that the manifest generator will need
            message.put(Constants.CSV_FILE_NAME, fileName);
            message.put(Constants.CSV_FILE_PATH, filePath);
            // For now, our default IIIF API version is v2... TODO: make this a configuration option?
            message.put(Constants.IIIF_API_VERSION, iiifVersion == null ? Constants.IIIF_API_V2 : iiifVersion);

            if (update) {
                options.addHeader(Constants.ACTION, Op.POST_UPDATE_CSV);
            } else {
                options.addHeader(Constants.ACTION, Op.POST_CSV);
            }

            if (iiifHost != null) {
                message.put(Constants.IIIF_HOST, iiifHost);
            }

            // Send a message to the manifest generator
            sendMessage(ManifestVerticle.class.getName(), message, options, Integer.MAX_VALUE, send -> {
                if (send.succeeded()) {
                    updateCSV(fileName, filePath, response);
                } else {
                    final ReplyException error = (ReplyException) send.cause();
                    returnError(response, error.failureCode(), error);
                }
            });

        }
    }

    private void updateCSV(final String aFileName, final String aFilePath, final HttpServerResponse aResponse) {
        final String responseMessage = LOGGER.getMessage(MessageCodes.MFS_038, aFileName, aFilePath);
        final FileSystem fileSystem = myVertx.fileSystem();

        // Read the uploaded CSV file and updating it before sending it back
        fileSystem.readFile(aFilePath, read -> {
            if (read.succeeded()) {
                final String csvString = read.result().toString(StandardCharsets.UTF_8);
                final StringWriter writer = new StringWriter();

                try (CSVReader csvReader = new CSVReader(new StringReader(csvString));
                        CSVWriter csvWriter = new CSVWriter(writer)) {
                    csvWriter.writeAll(LinkUtils.addManifests(myUrl, csvReader.readAll()));

                    aResponse.setStatusCode(HTTP.CREATED);
                    aResponse.setStatusMessage(responseMessage);
                    aResponse.putHeader(Constants.CONTENT_TYPE, Constants.CSV_MEDIA_TYPE);
                    aResponse.putHeader(Constants.CONTENT_DISPOSITION, StringUtils.format(ATTACHMENT, aFileName));
                    aResponse.end(Buffer.buffer(writer.toString()));
                } catch (final IOException details) {
                    returnError(aResponse, HTTP.INTERNAL_SERVER_ERROR, details);
                } catch (final CsvException details) {
                    returnError(aResponse, HTTP.BAD_REQUEST, details);
                }
            } else {
                returnError(aResponse, HTTP.INTERNAL_SERVER_ERROR, read.cause());
            }
        });
    }

    /**
     * Return an error page (and response code) to the requester.
     *
     * @param aResponse A HTTP response
     * @param aThrowable A throwable exception
     */
    private void returnError(final HttpServerResponse aResponse, final int aStatusCode, final Throwable aThrowable) {
        final String error = aThrowable.getMessage();
        final String body = LOGGER.getMessage(MessageCodes.MFS_103, error.replaceAll(Constants.EOL_REGEX, BR_TAG));

        LOGGER.error(aThrowable, LOGGER.getMessage(MessageCodes.MFS_103, error));

        // Handle errors that don't have a status code set
        if (aStatusCode >= 0) {
            aResponse.setStatusCode(aStatusCode);
        } else {
            aResponse.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
        }

        aResponse.setStatusMessage(error.replaceAll(Constants.EOL_REGEX, Constants.EMPTY));
        aResponse.putHeader(Constants.CONTENT_TYPE, Constants.HTML_MEDIA_TYPE);
        aResponse.end(StringUtils.format(myExceptionPage, body));
    }
}
