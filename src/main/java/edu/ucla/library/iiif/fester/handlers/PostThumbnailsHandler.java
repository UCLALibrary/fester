
package edu.ucla.library.iiif.fester.handlers;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import info.freelibrary.util.IOUtils;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
//import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
//import io.vertx.core.eventbus.ReplyException;
//import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.FileUpload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
//import java.nio.file.Path;
import java.nio.file.Paths;
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
//import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.ThumbnailUtils;

/**
 * 
 */
public class PostThumbnailsHandler extends AbstractFesterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostThumbnailsHandler.class, Constants.MESSAGES);

    private final String myExceptionPage;

    private final String myUrl;

    private final String myFesterizeVersion;

    private final Pattern myFesterizeUserAgentPattern;

    private final int myPort;

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
        myPort = Integer.parseInt(aConfig.getString(Config.HTTP_PORT));
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
            final String filePath = csvFile.uploadedFileName();
            final String fileName = csvFile.fileName();
            final JsonObject message = new JsonObject();
            final DeliveryOptions options = new DeliveryOptions();
            final String iiifHost = StringUtils.trimToNull(request.getFormAttribute(Constants.IIIF_HOST));
            final String iiifVersion = StringUtils.trimToNull(request.getFormAttribute(Constants.IIIF_API_VERSION));

            try {
                List<String[]> csvLines = new CSVReader(Files.newBufferedReader( Paths.get(filePath))).readAll();
                csvLines = ThumbnailUtils.addThumbnailColumn(csvLines);
                final int manifestIndex = Arrays.asList(csvLines.get(0)).indexOf(CSV.ITEM_ARK);
                final String manifestID = csvLines.get(1)[manifestIndex];
                final HttpClient httpClient = myVertx.createHttpClient();
                final int thumbIndex;
                final String thumbID;
                httpClient.get(myPort, myUrl, "/" + manifestID + "/manifest", httpResponse -> {
                    if (httpResponse.statusCode() == HTTP.OK) {
                    } else {
                    }
                });
            } catch (CsvException | IOException details) {
                errorMessage = details.getMessage();
                response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                response.setStatusMessage(errorMessage);
                response.putHeader(Constants.CONTENT_TYPE, Constants.HTML_MEDIA_TYPE);
                response.end(StringUtils.format(myExceptionPage, errorMessage));
            }

            //get manifest column, retrieve collection manifest, get items list, decide on thumbnail from items length,
            //add thumbnail to works rows
        }
    }

    private Future<JsonObject> getByURL(final String aUrl) {
	final Promise<JsonObject> promise = Promise.promise();
        final HttpClientOptions options = new HttpClientOptions().setSsl(true).setUseAlpn(true)
            .setProtocolVersion(HttpVersion.HTTP_2).setTrustAll(true);
        final HttpClient httpClient = myVertx.createHttpClient(options);
        httpClient.get(myPort, aUrl, "", httpResponse -> {
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
}
