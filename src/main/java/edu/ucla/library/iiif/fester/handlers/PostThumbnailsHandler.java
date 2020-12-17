
package edu.ucla.library.iiif.fester.handlers;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import info.freelibrary.util.IOUtils;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.FileUpload;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.CSV;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.ThumbnailUtils;

/**
 * 
 */
public class PostThumbnailsHandler extends AbstractFesterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostThumbnailsHandler.class, Constants.MESSAGES);

    private static final String MANIFESTS = "manifests";

    private static final String ATTACHMENT = "attachment; filename=\"{}\"";

    private static final String BR_TAG = "<br>";

    private final String myExceptionPage;

    private final String myUrl;

    private final String myFesterizeVersion;

    private final Pattern myFesterizeUserAgentPattern;

    /**
     * @param aVertx
     * @param aConfig
     */
    public PostThumbnailsHandler(final Vertx aVertx, final JsonObject aConfig) throws IOException {
        super(aVertx, aConfig);

        final byte[] bytes = IOUtils.readBytes(getClass().getResourceAsStream("/webroot/error.html"));

        myExceptionPage = new String(bytes, StandardCharsets.UTF_8);
        myUrl = aConfig.getString(Config.FESTER_URL);
        myFesterizeVersion = aConfig.getString(Config.FESTERIZE_VERSION);
        myFesterizeUserAgentPattern = Pattern.compile("Festerize/(?<version>\\d+\\.\\d+\\.\\d+)");
    }

    /*
     * (non-Javadoc)
     * @see io.vertx.core.Handler#handle(java.lang.Object)
     */
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
            returnError(response, HTTP.BAD_REQUEST, errorMessage);
        } else if (festerizeUserAgentMatcher.matches() &&
                !festerizeUserAgentMatcher.group("version").equals(myFesterizeVersion)) { // Festerize version mismatch
            errorMessage = LOGGER.getMessage(MessageCodes.MFS_147, myFesterizeVersion);
            returnError(response, HTTP.BAD_REQUEST, errorMessage);
        } else {
            final FileUpload csvFile = csvUploads.iterator().next();
            final String filePath = csvFile.uploadedFileName();
            final String fileName = csvFile.fileName();
            final JsonObject message = new JsonObject();
            final DeliveryOptions options = new DeliveryOptions();
            final String iiifHost = StringUtils.trimToNull(request.getFormAttribute(Constants.IIIF_HOST));
            final String iiifVersion = StringUtils.trimToNull(request.getFormAttribute(Constants.IIIF_API_VERSION));

            try {
                final List<String[]> originalLines =
                    new CSVReader(Files.newBufferedReader(Paths.get(filePath))).readAll();
                final List<String[]> linesWithThumbs = ThumbnailUtils.addThumbnailColumn(originalLines);
                final int manifestIndex = Arrays.asList(linesWithThumbs.get(0)).indexOf(CSV.MANIFEST_URL);
                final String collectionURL = linesWithThumbs.get(1)[manifestIndex];
                final List<String> errors = new ArrayList<>();
                final Future<JsonObject> collection = getManifest(collectionURL);
                collection.onComplete(result -> {
                    if (result.failed()) {
                        errors.add(result.cause().getMessage());
                    } else {
                        final JsonArray canvases = result.result().getJsonArray(MANIFESTS);
                        final int canvasCount = canvases.size();
                        final int canvasIndex;
                        if (canvasCount < 3) {
                            canvasIndex = 0;
                        } else {
                            canvasIndex = ThumbnailUtils.pickThumbnailIndex(canvasCount - 1);
                        }
                        final String canvasURL = canvases.getJsonObject(canvasIndex).getString("id");
                        final Future<JsonObject> canvas = getManifest(canvasURL);
                        canvas.onComplete(canvasResult -> {
                            if (canvasResult.failed()) {
                                errors.add(canvasResult.cause().getMessage());
                            } else {
                                final JsonObject thumbCanvas = canvasResult.result();
                                final String thumbURL = thumbCanvas.getJsonArray("sequences").getJsonObject(0)
                                      .getJsonArray("canvases").getJsonObject(0).getJsonArray("images")
                                      .getJsonObject(0).getJsonObject("resource").getString("@id");
                                ThumbnailUtils.addThumbnailURL(linesWithThumbs, thumbURL);
                            }
                        });
                    }
                });
                if (errors.size() > 0) {
                    final StringBuilder details = new StringBuilder();
                    errors.stream().forEach(error -> details.append(error).append(" "));
                    returnError(response, HTTP.INTERNAL_SERVER_ERROR, details.toString().trim());
                } else {
                    returnCSV(fileName, filePath, linesWithThumbs, response);
                }
            } catch (CsvException | IOException details) {
                logError(details);
                returnError(response, HTTP.INTERNAL_SERVER_ERROR, details.getMessage());
            }

        }
    }

    private Future<JsonObject> getManifest(final String aUrl) {
        final Promise<JsonObject> promise = Promise.promise();
        final HttpClientOptions options = new HttpClientOptions().setSsl(true).setUseAlpn(true)
            .setProtocolVersion(HttpVersion.HTTP_2).setTrustAll(true);
        final HttpClient httpClient = myVertx.createHttpClient(options);
        httpClient.get(443, aUrl, "", httpResponse -> {
            if (httpResponse.statusCode() == HTTP.OK) {
                httpResponse.bodyHandler(body -> {
                    promise.complete(new JsonObject(body.toString()));
                });
            } else {
                promise.fail(httpResponse.statusMessage());
            }
        });
        return promise.future();
    }

    private void returnCSV(final String aFileName, final String aFilePath, final List<String[]> aCsvList,
                           final HttpServerResponse aResponse) {
        final StringWriter writer = new StringWriter();
        final String responseMessage = LOGGER.getMessage(MessageCodes.MFS_038, aFileName, aFilePath);
        try (CSVWriter csvWriter = new CSVWriter(writer)) {
            csvWriter.writeAll(aCsvList);

            aResponse.setStatusCode(HTTP.OK);
            aResponse.setStatusMessage(responseMessage);
            aResponse.putHeader(Constants.CONTENT_TYPE, Constants.CSV_MEDIA_TYPE);
            aResponse.putHeader(Constants.CONTENT_DISPOSITION, StringUtils.format(ATTACHMENT, aFileName));
            aResponse.end(Buffer.buffer(writer.toString()));
        } catch (final IOException details) {
            logError(details);
            returnError(aResponse, HTTP.INTERNAL_SERVER_ERROR, details.getMessage());
        }
    }

    private void logError(final Throwable aThrowable) {
        LOGGER.error(aThrowable, LOGGER.getMessage(MessageCodes.MFS_103, aThrowable.getMessage()));
    }

    private void returnError(final HttpServerResponse aResponse, final int aStatusCode, final String aError) {
        final String body = LOGGER.getMessage(MessageCodes.MFS_103, aError.replaceAll(Constants.EOL_REGEX, BR_TAG));

        aResponse.setStatusCode(aStatusCode);
        aResponse.setStatusMessage(aError.replaceAll(Constants.EOL_REGEX, Constants.EMPTY));
        aResponse.putHeader(Constants.CONTENT_TYPE, Constants.HTML_MEDIA_TYPE);
        aResponse.end(StringUtils.format(myExceptionPage, body));
    }
}
