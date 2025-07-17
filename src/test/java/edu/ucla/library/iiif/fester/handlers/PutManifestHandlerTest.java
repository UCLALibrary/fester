
package edu.ucla.library.iiif.fester.handlers;

import java.io.IOException;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.SdkClientException;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import edu.ucla.library.iiif.fester.utils.TestUtils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * Tests the handler that puts manifests into the manifest store.
 */
public class PutManifestHandlerTest extends AbstractFesterHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutManifestHandlerTest.class, Constants.MESSAGES);

    private String myPutManifestID;

    private String myPutManifestS3Key;

    /**
     * Test set up.
     *
     * @param aContext A testing context
     */
    @Override
    @Before
    public void setUp(final TestContext aContext) throws IOException {
        super.setUp(aContext);

        final String putPrefix = "PUT_";

        myPutManifestID = putPrefix + UUID.randomUUID().toString();
        myPutManifestS3Key = IDUtils.getWorkS3Key(myPutManifestID);
    }

    /**
     * Test tear down.
     *
     * @param aContext A testing context
     */
    @Override
    @After
    public void tearDown(final TestContext aContext) {
        super.tearDown(aContext);

        try {
            // If object doesn't exist, this still completes successfully
            myS3Client.deleteObject(myS3Bucket, myPutManifestS3Key);
        } catch (final SdkClientException details) {
            aContext.fail(details);
        }

        myVertx.close(aContext.asyncAssertSuccess());
    }

    /**
     * Test the PutManifestHandler.
     *
     * @param aContext A testing context
     * @throws IOException If there is trouble reading the manifest
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testPutManifestHandler(final TestContext aContext) throws IOException {
        final String manifestPath = V2_MANIFEST_FILE.getAbsolutePath();
        final Buffer manifest = myVertx.fileSystem().readFileBlocking(manifestPath);
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String requestPath = IDUtils.getResourceURIPath(myPutManifestS3Key);
        final RequestOptions requestOpts = new RequestOptions();
        final HttpClient httpClient;

        LOGGER.debug(MessageCodes.MFS_016, requestPath);

        requestOpts.setPort(port).setHost(Constants.UNSPECIFIED_HOST).setURI(requestPath);
        requestOpts.addHeader(Constants.CONTENT_TYPE, Constants.JSON_MEDIA_TYPE);
        httpClient = myVertx.createHttpClient();

        httpClient.put(requestOpts, putResponse -> {
            switch (putResponse.statusCode()) {
                case HTTP.OK:
                    httpClient.get(requestOpts, getResponse -> {
                        if (getResponse.statusCode() != HTTP.OK) {
                            aContext.fail(
                                    LOGGER.getMessage(MessageCodes.MFS_018, manifestPath, getResponse.statusCode()));
                        }

                        TestUtils.complete(asyncTask);
                    }).exceptionHandler(error -> {
                        aContext.fail(error);
                        TestUtils.complete(asyncTask);
                    }).end();

                    break;
                default:
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_018, manifestPath, putResponse.statusCode()));
                    TestUtils.complete(asyncTask);
            }
        }).exceptionHandler(error -> {
            aContext.fail(error);
            TestUtils.complete(asyncTask);
        }).end(manifest);
    }

    /**
     * Test the PutManifestHandler.
     *
     * @param aContext A testing context
     * @throws IOException If there is trouble reading the manifest
     */
    @Test
    public void testPutManifestHandlerMissingMediaType(final TestContext aContext) throws IOException {
        final Buffer manifest = myVertx.fileSystem().readFileBlocking(V2_MANIFEST_FILE.getAbsolutePath());
        final String requestPath = IDUtils.getResourceURIPath(myPutManifestS3Key);
        final WebClient httpClient = WebClient.create(myVertx);
        final int port = aContext.get(Config.HTTP_PORT);
        final Async asyncTask = aContext.async();

        LOGGER.debug(MessageCodes.MFS_016, requestPath);

        // Fail to set the content-type header
        httpClient.put(port, Constants.UNSPECIFIED_HOST, requestPath).sendBuffer(manifest, handler -> {
            if (handler.succeeded()) {
                final HttpResponse<Buffer> response = handler.result();
                final int statusCode = response.statusCode();

                if (statusCode == HTTP.BAD_REQUEST) {
                    aContext.assertFalse(myS3Client.doesObjectExist(myS3Bucket, myPutManifestS3Key));
                    TestUtils.complete(asyncTask);
                } else {
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_019, statusCode));
                }
            } else {
                aContext.fail(handler.cause());
            }
        });
    }

    /**
     * Test the PutManifestHandler.
     *
     * @param aContext A testing context
     * @throws IOException If there is trouble reading the manifest
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testPutManifestHandlerUnsupportedMediaType(final TestContext aContext) throws IOException {
        final String manifestPath = V2_MANIFEST_FILE.getAbsolutePath();
        final Buffer manifest = myVertx.fileSystem().readFileBlocking(manifestPath);
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String requestPath = IDUtils.getResourceURIPath(myPutManifestS3Key);
        final RequestOptions requestOpts = new RequestOptions();

        LOGGER.debug(MessageCodes.MFS_016, requestPath);

        requestOpts.setPort(port).setHost(Constants.UNSPECIFIED_HOST).setURI(requestPath);
        requestOpts.addHeader(Constants.CONTENT_TYPE, "text/plain"); // wrong media type

        myVertx.createHttpClient().put(requestOpts, response -> {
            final int statusCode = response.statusCode();

            switch (statusCode) {
                case HTTP.UNSUPPORTED_MEDIA_TYPE:
                case HTTP.METHOD_NOT_ALLOWED:
                    aContext.assertFalse(myS3Client.doesObjectExist(myS3Bucket, myPutManifestS3Key));
                    TestUtils.complete(asyncTask);

                    break;
                default:
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_022, statusCode));
            }
        }).end(manifest);
    }

}
