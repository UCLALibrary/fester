
package edu.ucla.library.iiif.manifeststore.handlers;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amazonaws.SdkClientException;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.manifeststore.Config;
import edu.ucla.library.iiif.manifeststore.Constants;
import edu.ucla.library.iiif.manifeststore.HTTP;
import edu.ucla.library.iiif.manifeststore.MessageCodes;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class PutManifestHandlerTest extends AbstractManifestHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutManifestHandlerTest.class, Constants.MESSAGES);

    private static final String MANIFEST_PATH = "/manifests/";

    private String myPutManifestID;

    /**
     * Test set up.
     *
     * @param aContext A testing context
     */
    @Override
    @Before
    public void setUp(final TestContext aContext) throws IOException {
        super.setUp(aContext);

        myPutManifestID = "PUT_" + myManifestID;
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
            myS3Client.deleteObject(myS3Bucket, myPutManifestID);
        } catch (final SdkClientException details) {
            aContext.fail(details);
        }

        myVertx.close(aContext.asyncAssertSuccess());
    }

    /**
     * Test the PutManifestHandler.
     *
     * @param aContext A testing context
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testPutManifestHandler(final TestContext aContext) throws IOException {
        final String manifestPath = MANIFEST_FILE.getAbsolutePath();
        final Buffer manifest = myVertx.fileSystem().readFileBlocking(manifestPath);
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String testIDPath = MANIFEST_PATH + myPutManifestID;
        final RequestOptions requestOpts = new RequestOptions();

        LOGGER.debug(MessageCodes.MFS_016, testIDPath);

        requestOpts.setPort(port).setHost(Constants.UNSPECIFIED_HOST).setURI(testIDPath);
        requestOpts.addHeader(Constants.CONTENT_TYPE, Constants.JSON_MEDIA_TYPE);

        myVertx.createHttpClient().put(requestOpts, response -> {
            final int statusCode = response.statusCode();

            switch (statusCode) {
                case HTTP.OK:
                    aContext.assertTrue(myS3Client.doesObjectExist(myS3Bucket, myPutManifestID));
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
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testPutManifestHandlerMissingMediaType(final TestContext aContext) throws IOException {
        final String manifestPath = MANIFEST_FILE.getAbsolutePath();
        final Buffer manifest = myVertx.fileSystem().readFileBlocking(manifestPath);
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String testIDPath = MANIFEST_PATH + myPutManifestID;
        final RequestOptions requestOpts = new RequestOptions();

        LOGGER.debug(MessageCodes.MFS_016, testIDPath);

        requestOpts.setPort(port).setHost(Constants.UNSPECIFIED_HOST).setURI(testIDPath);
        // Fail to set the content-type header

        myVertx.createHttpClient().put(requestOpts, response -> {
            final int statusCode = response.statusCode();

            switch (statusCode) {
                case HTTP.UNSUPPORTED_MEDIA_TYPE:
                    aContext.assertFalse(myS3Client.doesObjectExist(myS3Bucket, myPutManifestID));
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
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testPutManifestHandlerUnsupportedMediaType(final TestContext aContext) throws IOException {
        final String manifestPath = MANIFEST_FILE.getAbsolutePath();
        final Buffer manifest = myVertx.fileSystem().readFileBlocking(manifestPath);
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);
        final String testIDPath = MANIFEST_PATH + myPutManifestID;
        final RequestOptions requestOpts = new RequestOptions();

        LOGGER.debug(MessageCodes.MFS_016, testIDPath);

        requestOpts.setPort(port).setHost(Constants.UNSPECIFIED_HOST).setURI(testIDPath);
        requestOpts.addHeader(Constants.CONTENT_TYPE, "text/plain"); // wrong media type

        myVertx.createHttpClient().put(requestOpts, response -> {
            final int statusCode = response.statusCode();

            switch (statusCode) {
                case HTTP.UNSUPPORTED_MEDIA_TYPE:
                    aContext.assertFalse(myS3Client.doesObjectExist(myS3Bucket, myPutManifestID));
                    asyncTask.complete();

                    break;
                default:
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_022, statusCode));
            }
        }).end(manifest);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
