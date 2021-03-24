
package edu.ucla.library.iiif.fester.handlers;

import static info.freelibrary.iiif.presentation.v2.utils.Constants.CANVASES;
import static info.freelibrary.iiif.presentation.v2.utils.Constants.IMAGE_CONTENT;
import static info.freelibrary.iiif.presentation.v2.utils.Constants.RESOURCE;
import static info.freelibrary.iiif.presentation.v2.utils.Constants.SEQUENCES;

import static info.freelibrary.iiif.presentation.v3.Constants.BODY;
import static info.freelibrary.iiif.presentation.v3.Constants.CONTEXT;
import static info.freelibrary.iiif.presentation.v3.Constants.ITEMS;
import static info.freelibrary.iiif.presentation.v3.Constants.SERVICE;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.CsvParser;
import edu.ucla.library.iiif.fester.CsvParsingException;
import edu.ucla.library.iiif.fester.CSV;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.ObjectType;
import edu.ucla.library.iiif.fester.utils.ThumbnailUtils;

import info.freelibrary.util.IOUtils;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to select and add thumbnail links to uploaded CSV
 */
public class PostThumbnailsHandler extends AbstractFesterHandler {

    /* A logger for the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(PostThumbnailsHandler.class, Constants.MESSAGES);

    private static final String ATTACHMENT = "attachment; filename=\"{}\"";

    private static final String BR_TAG = "<br>";

    private final String myExceptionPage;

    private final String myFesterizeVersion;

    /* FUA = Festerize User Agent; agent to verify festerize version, acronymed for codacy */
    private final Pattern myFUAPattern;

    /**
     * Creates a handler to handle POSTs to add thumbnail URLs.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A application configuration
     * @throws IOException If there is trouble reading the HTML template files
     */
    public PostThumbnailsHandler(final Vertx aVertx, final JsonObject aConfig) throws IOException {
        super(aVertx, aConfig);

        final byte[] bytes = IOUtils.readBytes(getClass().getResourceAsStream("/webroot/error.html"));

        myExceptionPage = new String(bytes, StandardCharsets.UTF_8);
        myFesterizeVersion = aConfig.getString(Config.FESTERIZE_VERSION);
        myFUAPattern = Pattern.compile("Festerize/(?<version>\\d+\\.\\d+\\.\\d+)");
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
        final Matcher festerizeUserAgentMatcher = myFUAPattern.matcher(festerizeUserAgent);
        final String errorMessage;

        // An uploaded CSV is required
        if (csvUploads.isEmpty()) {
            errorMessage = LOGGER.getMessage(MessageCodes.MFS_037);
            returnError(response, HTTP.BAD_REQUEST, errorMessage);
        } else if (festerizeUserAgentMatcher.matches() &&
                !festerizeUserAgentMatcher.group("version").equals(myFesterizeVersion)) {
            errorMessage = LOGGER.getMessage(MessageCodes.MFS_147, myFesterizeVersion);
            returnError(response, HTTP.BAD_REQUEST, errorMessage);
        } else {
            final FileUpload csvFile = csvUploads.iterator().next();
            final String filePath = csvFile.uploadedFileName();
            final String fileName = csvFile.fileName();

            try (CSVReader csvReader = new CSVReader(Files.newBufferedReader(Paths.get(filePath)))) {
                final CsvParser parser = new CsvParser().parse(Paths.get(filePath));
                final List<String[]> originalLines = csvReader.readAll();
                final List<String[]> linesWithThumbs = ThumbnailUtils.addThumbnailColumn(originalLines);
                final int manifestIndex = Arrays.asList(linesWithThumbs.get(0)).indexOf(CSV.MANIFEST_URL);
                @SuppressWarnings("rawtypes")
                final List<Future> futures = new ArrayList<>();
                for (int rowIndex = 1; rowIndex < linesWithThumbs.size(); rowIndex++) {
                    final ObjectType rowType = CsvParser.getObjectType(
                            linesWithThumbs.get(rowIndex), parser.getCsvHeaders());
                    if (rowType.equals(ObjectType.WORK)) {
                        futures.add(processRow(linesWithThumbs, manifestIndex, rowIndex));
                    }
                }
                CompositeFuture.all(futures).onComplete(handler -> {
                    if (handler.succeeded()) {
                        returnCSV(fileName, filePath, linesWithThumbs, response);
                    } else {
                        returnError(response, HTTP.INTERNAL_SERVER_ERROR, handler.cause().getMessage());
                    }
                });
            } catch (CsvException | IOException | CsvParsingException details) {
                logError(details);
                returnError(response, HTTP.INTERNAL_SERVER_ERROR, details.getMessage());
            }
        }
    }

    /**
     * Process a row from a CSV to add a thumbnail URL.
     *
     * @param aCsvList A CSV parsed into a list of string arrays
     * @param aManifestIndex The column index in this CSV where manifest URLs are stored
     * @param aRowIndex The row in the CSV being updated
     */
    private Future<Void> processRow(final List<String[]> aCsvList, final int aManifestIndex, final int aRowIndex) {
        final String manifestURL = aCsvList.get(aRowIndex)[aManifestIndex];
        final Promise<Void> promise = Promise.promise();
        final HttpRequest<JsonObject> request;
        request = WebClient.create(myVertx)
                .getAbs(manifestURL)
                .putHeader("Accept", Constants.JSON_MEDIA_TYPE)
                .as(BodyCodec.jsonObject())
                .expect(ResponsePredicate.SC_OK);
        if (manifestURL.startsWith("https")) {
            request.ssl(true);
        }
        request.send(asyncResult -> {
            if (asyncResult.succeeded()) {
                final JsonObject manifestBody = asyncResult.result().body();
                final String context = manifestBody.getString(CONTEXT);
                final int thumbIndex = ThumbnailUtils.findThumbHeaderIndex(aCsvList.get(0));
                if (context.contains(Constants.CONTEXT_V2)) {
                    addV2Thumb(thumbIndex, aRowIndex, manifestBody, aCsvList);
                } else if (context.contains(Constants.CONTEXT_V3)) {
                    addV3Thumb(thumbIndex, aRowIndex, manifestBody, aCsvList);
                } else {
                    LOGGER.info(LOGGER.getMessage(MessageCodes.MFS_167, manifestURL));
                }
                promise.complete();
            } else {
                promise.fail(asyncResult.result().statusMessage());
            }
        });

        return promise.future();

    }

    /**
     * Retrieve the base thumbnail URL from IIIF V2 presentation manifests.
     *
     * @param aColumnIndex The column in the CSV where thumbnail will be added
     * @param aRowIndex The CSV row being updated
     * @param aManifest A IIIF work manifest
     * @param aCsvList A CSV file parsed as a list of string arrays
     */
    private void addV2Thumb(final int aColumnIndex, final int aRowIndex,
            final JsonObject aManifest, final List<String[]> aCsvList) {
        final JsonArray canvases = aManifest.getJsonArray(SEQUENCES).getJsonObject(0).getJsonArray(CANVASES);
        final int canvasIndex = chooseThumbIndex(canvases.size());
        final String thumbURL = canvases.getJsonObject(canvasIndex).getJsonArray(IMAGE_CONTENT)
                .getJsonObject(0).getJsonObject(RESOURCE).getJsonObject(SERVICE)
                .getString(Constants.ID_V2);
        ThumbnailUtils.addThumbnailURL(aColumnIndex, aRowIndex, thumbURL, aCsvList);
    }

    /**
     * Retrieve the base thumbnail URL from IIIF V3 presentation manifests.
     *
     * @param aColumnIndex The column in the CSV where thumbnail will be added
     * @param aRowIndex The CSV row being updated
     * @param aManifest A IIIF work manifest
     * @param aCsvList A CSV file parsed as a list of string arrays
     */
    private void addV3Thumb(final int aColumnIndex, final int aRowIndex,
            final JsonObject aManifest, final List<String[]> aCsvList) {

        final JsonArray canvases = aManifest.getJsonArray(ITEMS);
        final int canvasIndex = chooseThumbIndex(canvases.size());
        final JsonObject canvas = canvases.getJsonObject(canvasIndex);
        final JsonObject image = canvas.getJsonArray(ITEMS).getJsonObject(0).getJsonArray(ITEMS)
                .getJsonObject(0).getJsonObject(BODY);
        final String thumbURL = image.getJsonArray(SERVICE).getJsonObject(0).getString(Constants.ID_V3);

        ThumbnailUtils.addThumbnailURL(aColumnIndex, aRowIndex, thumbURL, aCsvList);
    }

    /**
     * Select the index for the thumbnail from a list of images.
     *
     * @param aCount The number of images
     */
    private int chooseThumbIndex(final int aCount) {
        if (aCount <= 3) {
            return 0;
        } else {
            return ThumbnailUtils.pickThumbnailIndex(aCount - 1);
        }
    }

    /**
     * Return the processed CSV to user.
     *
     * @param aFileName Name of CSV file
     * @param aFilePath Path to CSV file
     * @param aCsvList CSV file parsed as list of string arrays
     * @param aResponse Response returned to caller
     */
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

    /**
     * Logs error for developers/administrators.
     *
     * @param aThrowable The error that halded processing
     */
    private void logError(final Throwable aThrowable) {
        LOGGER.error(aThrowable, LOGGER.getMessage(MessageCodes.MFS_166, aThrowable.getMessage()));
    }

    /**
     * Return an error page (and response code) to the requester.
     *
     * @param aResponse A HTTP response
     * @param aStatusCode A HTTP response code
     * @param aThrowable A throwable exception
     */
    private void returnError(final HttpServerResponse aResponse, final int aStatusCode, final String aError) {
        final String body = LOGGER.getMessage(MessageCodes.MFS_166, aError.replaceAll(Constants.EOL_REGEX, BR_TAG));

        aResponse.setStatusCode(aStatusCode);
        aResponse.setStatusMessage(aError.replaceAll(Constants.EOL_REGEX, Constants.EMPTY));
        aResponse.putHeader(Constants.CONTENT_TYPE, Constants.HTML_MEDIA_TYPE);
        aResponse.end(StringUtils.format(myExceptionPage, body));
    }
}
