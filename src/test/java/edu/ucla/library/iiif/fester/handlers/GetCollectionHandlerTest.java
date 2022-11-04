
package edu.ucla.library.iiif.fester.handlers;

import java.io.IOException;

import org.junit.Test;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import edu.ucla.library.iiif.fester.utils.TestUtils;

import ch.qos.logback.classic.Level;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * Tests the handler that gets collections from the S3 bucket.
 */
public class GetCollectionHandlerTest extends AbstractFesterHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetCollectionHandlerTest.class, Constants.MESSAGES);

    private final String myCollectionS3Key = IDUtils.getCollectionS3Key("ark:/21198/zz0009gsq9");

    /**
     * Test the GetCollectionHandler.
     *
     * @param aContext A testing context
     * @throws IOException If there is trouble reading a manifest
     */
    @Test
    public void testGetCollectionHandler(final TestContext aContext) throws IOException {
        final String testCollectionDoc = StringUtils.read(V2_COLLECTION_FILE).replaceAll(myUrlPattern, myUrl);
        final String requestPath = IDUtils.getResourceURIPath(myCollectionS3Key);
        final JsonObject expected = new JsonObject(testCollectionDoc);
        final WebClient httpClient = WebClient.create(myVertx);
        final int port = aContext.get(Config.HTTP_PORT);
        final Async asyncTask = aContext.async();

        LOGGER.debug(MessageCodes.MFS_137, requestPath);

        httpClient.get(port, Constants.UNSPECIFIED_HOST, requestPath).send(handler -> {
            if (handler.succeeded()) {
                final HttpResponse<Buffer> response = handler.result();
                final int statusCode = response.statusCode();

                if (statusCode == HTTP.OK) {
                    aContext.assertEquals(Constants.STAR, response.getHeader(Constants.CORS_HEADER));
                    aContext.assertEquals(expected, response.bodyAsJsonObject());
                } else {
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_003, HTTP.OK, statusCode));
                }

                TestUtils.complete(asyncTask);
            } else {
                aContext.fail(handler.cause());
            }
        });
    }

    /**
     * Confirm that a bad path request returns a 404 response.
     *
     * @param aContext A testing context
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testGetCollectionHandler404(final TestContext aContext) {
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String missingPath = "/collections/missingIdentifier";
        final Level logLevel = setLogLevel(GetCollectionHandler.class, Level.OFF);

        myVertx.createHttpClient().getNow(port, Constants.UNSPECIFIED_HOST, missingPath, response -> {
            final int statusCode = response.statusCode();

            setLogLevel(GetCollectionHandler.class, logLevel); // Turn logger back on after expected error

            if (response.statusCode() != HTTP.NOT_FOUND) {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_004, HTTP.NOT_FOUND, statusCode));
            }

            TestUtils.complete(asyncTask);
        });
    }
}
