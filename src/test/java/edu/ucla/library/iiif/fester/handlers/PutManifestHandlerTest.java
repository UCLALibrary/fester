
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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

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
        final String manifestPath = MANIFEST_FILE.getAbsolutePath();
        final Buffer manifest = myVertx.fileSystem().readFileBlocking(manifestPath);
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String requestPath = IDUtils.getResourceURIPath(myPutManifestS3Key);
        final RequestOptions requestOpts = new RequestOptions();

        LOGGER.debug(MessageCodes.MFS_016, requestPath);

        requestOpts.setPort(port).setHost(Constants.UNSPECIFIED_HOST).setURI(requestPath);
        requestOpts.addHeader(Constants.CONTENT_TYPE, Constants.JSON_MEDIA_TYPE);

        myVertx.createHttpClient().put(requestOpts, response -> {
            final int statusCode = response.statusCode();

            switch (statusCode) {
                case HTTP.OK:
                    aContext.assertTrue(myS3Client.doesObjectExist(myS3Bucket, myPutManifestS3Key));
                    asyncTask.complete();

                    break;
                default:
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_018, manifestPath, statusCode));
            }
        }).end(manifest);
    }

    /**
     * Test the PutManifestHandler.
     *
     * @param aContext A testing context
     * @throws IOException If there is trouble reading the manifest
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testPutManifestHandlerMissingMediaType(final TestContext aContext) throws IOException {
        final String manifestPath = MANIFEST_FILE.getAbsolutePath();
        final Buffer manifest = myVertx.fileSystem().readFileBlocking(manifestPath);
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String requestPath = IDUtils.getResourceURIPath(myPutManifestS3Key);
        final RequestOptions requestOpts = new RequestOptions();

        LOGGER.debug(MessageCodes.MFS_016, requestPath);

        requestOpts.setPort(port).setHost(Constants.UNSPECIFIED_HOST).setURI(requestPath);
        // Fail to set the content-type header

        myVertx.createHttpClient().put(requestOpts, response -> {
            final int statusCode = response.statusCode();

            switch (statusCode) {
                case HTTP.UNSUPPORTED_MEDIA_TYPE:
                case HTTP.METHOD_NOT_ALLOWED:
                    aContext.assertFalse(myS3Client.doesObjectExist(myS3Bucket, myPutManifestS3Key));
                    asyncTask.complete();

                    break;
                default:
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_019, statusCode));
            }

        }).end(manifest);
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
        final String manifestPath = MANIFEST_FILE.getAbsolutePath();
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
                    asyncTask.complete();

                    break;
                default:
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_022, statusCode));
            }
        }).end(manifest);
    }

}
