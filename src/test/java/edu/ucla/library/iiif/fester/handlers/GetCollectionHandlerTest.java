
package edu.ucla.library.iiif.fester.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import ch.qos.logback.classic.Level;
import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

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
    @SuppressWarnings("deprecation")
    public void testGetCollectionHandler(final TestContext aContext) throws IOException {
        final String expectedCollection = StringUtils.read(COLLECTION_FILE);
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String requestPath = IDUtils.getResourceURIPath(myCollectionS3Key);

        LOGGER.debug(MessageCodes.MFS_137, requestPath);

        myVertx.createHttpClient().getNow(port, Constants.UNSPECIFIED_HOST, requestPath, response -> {
            final int statusCode = response.statusCode();

            if (response.statusCode() == HTTP.OK) {
                response.bodyHandler(body -> {
                    final String foundCollection = body.toString(StandardCharsets.UTF_8);

                    // Verify that the CORS header is permissive
                    aContext.assertEquals(Constants.STAR, response.getHeader(Constants.CORS_HEADER));

                    // Verify that our retrieved JSON is as we expect it
                    aContext.assertEquals(new JsonObject(expectedCollection.replaceAll(myUrlPlaceholderPattern, myUrl)),
                            new JsonObject(foundCollection));
                });
            } else {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_003, HTTP.OK, statusCode));
            }

            if (!asyncTask.isCompleted()) {
                asyncTask.complete();
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

            if (!asyncTask.isCompleted()) {
                asyncTask.complete();
            }
        });
    }
}
