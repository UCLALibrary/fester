
package edu.ucla.library.iiif.fester.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.SdkClientException;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import ch.qos.logback.classic.Level;
import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class PutCollectionHandlerTest extends AbstractFesterHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutCollectionHandlerTest.class, Constants.MESSAGES);

    private String myPutCollectionID;

    private String myPutCollectionS3Key;

    @Override
    @Before
    public void setUp(final TestContext aContext) throws IOException {
        super.setUp(aContext);

        final String putPrefix = "PUT_";

        myPutCollectionID = putPrefix + UUID.randomUUID().toString();
        myPutCollectionS3Key = IDUtils.getCollectionS3Key(myPutCollectionID);
    }

    @Override
    @After
    public void tearDown(final TestContext aContext) {
        super.tearDown(aContext);

        try {
            // If object doesn't exist, this still completes successfully
            myS3Client.deleteObject(myS3Bucket, myPutCollectionS3Key);
        } catch (final SdkClientException details) {
            aContext.fail(details);
        }

        myVertx.close(aContext.asyncAssertSuccess());
    }

    /**
     * Test the PutCollectionHandler.
     *
     * @param aContext A testing context
     * @throws IOException If there is trouble reading a manifest
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testPutCollectionHandler(final TestContext aContext) throws IOException {
        final String manifestPath = COLLECTION_FILE.getAbsolutePath();
        final Buffer manifest = myVertx.fileSystem().readFileBlocking(manifestPath);
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String requestPath = IDUtils.getResourceURIPath(myPutCollectionS3Key);
        final RequestOptions requestOpts = new RequestOptions();

        LOGGER.debug(MessageCodes.MFS_016, requestPath);

        requestOpts.setPort(port).setHost(Constants.UNSPECIFIED_HOST).setURI(requestPath);
        requestOpts.addHeader(Constants.CONTENT_TYPE, Constants.JSON_MEDIA_TYPE);

        myVertx.createHttpClient().put(requestOpts, putResponse -> {
            final int putStatusCode = putResponse.statusCode();

            if (putStatusCode == HTTP.OK) {
                // Send a GET request to the same path to make sure PUT succeeded
                myVertx.createHttpClient().getNow(port, Constants.UNSPECIFIED_HOST, requestPath, getResponse -> {
                    final int getStatusCode = getResponse.statusCode();

                    if (getStatusCode == HTTP.OK) {
                        getResponse.bodyHandler(body -> {
                            final JsonObject expected = new JsonObject(manifest.toString(StandardCharsets.UTF_8)
                                    .replaceAll(myUrlPlaceholderPattern, myUrl));
                            final JsonObject found = new JsonObject(body);
                            aContext.assertEquals(expected, found);

                            if (!asyncTask.isCompleted()) {
                                asyncTask.complete();
                            }
                        });
                    } else {
                        aContext.fail(LOGGER.getMessage(MessageCodes.MFS_140, myPutCollectionID));
                    }
                });
            } else {
                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_018, manifestPath, putStatusCode));
            }
        }).end(manifest);
    }

    /**
     * Test the PutCollectionHandler.
     *
     * @param aContext A testing context
     * @throws IOException If there is trouble reading a manifest
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testPutCollectionHandlerMissingMediaType(final TestContext aContext) throws IOException {
        final String manifestPath = COLLECTION_FILE.getAbsolutePath();
        final Buffer manifest = myVertx.fileSystem().readFileBlocking(manifestPath);
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String requestPath = IDUtils.getResourceURIPath(myPutCollectionS3Key);
        final RequestOptions requestOpts = new RequestOptions();
        final Level logLevel = setLogLevel(GetCollectionHandler.class, Level.OFF);

        LOGGER.debug(MessageCodes.MFS_016, requestPath);

        requestOpts.setPort(port).setHost(Constants.UNSPECIFIED_HOST).setURI(requestPath);
        // Fail to set the content-type header

        myVertx.createHttpClient().put(requestOpts, putResponse -> {
            final int putStatusCode = putResponse.statusCode();

            switch (putStatusCode) {
                case HTTP.UNSUPPORTED_MEDIA_TYPE:
                case HTTP.METHOD_NOT_ALLOWED:
                    // Send a GET request to the same path to make sure PUT failed
                    myVertx.createHttpClient().getNow(port, Constants.UNSPECIFIED_HOST, requestPath, getResponse -> {
                        final int getStatusCode = getResponse.statusCode();

                        setLogLevel(GetCollectionHandler.class, logLevel); // Turn logger back on after expected error

                        if (getStatusCode == HTTP.NOT_FOUND) {
                            if (!asyncTask.isCompleted()) {
                                asyncTask.complete();
                            }
                        } else if (getStatusCode == HTTP.OK) {
                            aContext.fail(LOGGER.getMessage(MessageCodes.MFS_141, myPutCollectionID));
                        } else {
                            aContext.fail(LOGGER.getMessage(MessageCodes.MFS_010, getStatusCode));
                        }
                    });

                    break;
                default:
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_019, manifestPath, putStatusCode));
            }
        }).end(manifest);
    }

    /**
     * Test the PutCollectionHandler.
     *
     * @param aContext A testing context
     * @throws IOException If there is trouble reading a manifest
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testPutCollectionHandlerUnsupportedMediaType(final TestContext aContext) throws IOException {
        final String manifestPath = COLLECTION_FILE.getAbsolutePath();
        final Buffer manifest = myVertx.fileSystem().readFileBlocking(manifestPath);
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String requestPath = IDUtils.getResourceURIPath(myPutCollectionS3Key);
        final RequestOptions requestOpts = new RequestOptions();
        final Level logLevel = setLogLevel(GetCollectionHandler.class, Level.OFF);

        LOGGER.debug(MessageCodes.MFS_016, requestPath);

        requestOpts.setPort(port).setHost(Constants.UNSPECIFIED_HOST).setURI(requestPath);
        requestOpts.addHeader(Constants.CONTENT_TYPE, "text/plain"); // wrong media type

        myVertx.createHttpClient().put(requestOpts, putResponse -> {
            final int putStatusCode = putResponse.statusCode();

            switch (putStatusCode) {
                case HTTP.UNSUPPORTED_MEDIA_TYPE:
                case HTTP.METHOD_NOT_ALLOWED:
                    // Send a GET request to the same path to make sure PUT failed
                    myVertx.createHttpClient().getNow(port, Constants.UNSPECIFIED_HOST, requestPath, getResponse -> {
                        final int getStatusCode = getResponse.statusCode();

                        setLogLevel(GetCollectionHandler.class, logLevel); // Turn logger back on after expected error

                        if (getStatusCode == HTTP.NOT_FOUND) {
                            if (!asyncTask.isCompleted()) {
                                asyncTask.complete();
                            }
                        } else if (getStatusCode == HTTP.OK) {
                            aContext.fail(LOGGER.getMessage(MessageCodes.MFS_141, myPutCollectionID));
                        } else {
                            aContext.fail(LOGGER.getMessage(MessageCodes.MFS_010, getStatusCode));
                        }
                    });
                    break;
                default:
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_022, manifestPath, putStatusCode));
            }
        }).end(manifest);
    }
}
