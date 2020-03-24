
package edu.ucla.library.iiif.fester.fit;

import org.junit.Test;
import org.junit.runner.RunWith;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import edu.ucla.library.iiif.fester.utils.TestUtils;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;

/**
 * Tests related to getting a manifest from Fester's API.
 */
public class GetManifestFIT {

    /* The logger for the GET manifest tests */
    private static final Logger LOGGER = LoggerFactory.getLogger(GetManifestFIT.class, Constants.MESSAGES);

    /* The API endpoint being tested */
    private static final String API_PATH = "/{}/manifest";

    /* The path to the test file we using in testing */
    private static final String TEST_FILE_PATH = "json/pages-ordered.json";

    /**
     * Functional tests run against a local S3 clone.
     */
    @RunWith(VertxUnitRunner.class)
    public static class GetManifestFT extends BaseFesterFT {

        /**
         * Tests the GetManifestHandler.
         *
         * @param aContext A testing context
         */
        @Test
        public void testGetManifest(final TestContext aContext) {
            final String testData = TestUtils.getTestData(VERTX_INSTANCE, TEST_FILE_PATH);
            final String s3ManifestKey = IDUtils.getWorkS3Key(myID);
            final String apiPath = StringUtils.format(API_PATH, myID);
            final Async asyncTask = aContext.async();

            // Set up what we're going to test
            myS3Client.createBucket(BUCKET);
            myS3Client.putObject(BUCKET, s3ManifestKey, testData);

            // Run our test of the GET manifest handler
            myWebClient.get(FESTER_PORT, Constants.UNSPECIFIED_HOST, apiPath).send(request -> {
                if (request.succeeded()) {
                    final HttpResponse<Buffer> response = request.result();
                    final int statusCode = response.statusCode();

                    // Verify that the CORS header is permissive
                    aContext.assertEquals(Constants.STAR, response.getHeader(Constants.CORS_HEADER));

                    // Verify that what we get back is what we put in
                    if (statusCode == HTTP.OK) {
                        aContext.assertEquals(new JsonObject(testData), response.bodyAsJsonObject());
                    } else {
                        aContext.fail(LOGGER.getMessage(MessageCodes.MFS_004, HTTP.OK, statusCode));
                    }

                    complete(asyncTask);
                } else {
                    aContext.fail(request.cause());
                }
            });
        }

        /**
         * Confirm that a bad path request returns a 404 response.
         *
         * @param aContext A testing context
         */
        @Test
        public void testGetManifest404(final TestContext aContext) {
            final String apiPath = StringUtils.format(API_PATH, myID);
            final Async asyncTask = aContext.async();

            // Run our 404 test of the GET manifest handler
            myWebClient.get(FESTER_PORT, Constants.UNSPECIFIED_HOST, apiPath).send(request -> {
                if (request.succeeded()) {
                    final HttpResponse<Buffer> response = request.result();
                    final int statusCode = response.statusCode();

                    // Verify that we get a 404 response code
                    if (statusCode != HTTP.NOT_FOUND) {
                        aContext.fail(LOGGER.getMessage(MessageCodes.MFS_004, HTTP.NOT_FOUND, statusCode));
                    }

                    complete(asyncTask);
                } else {
                    aContext.fail(request.cause());
                }
            });
        }
    }

    /**
     * Functional tests run against S3.
     */
    @RunWith(VertxUnitRunner.class)
    public static class GetManifestIT extends BaseFesterIT {

        /**
         * Tests the GetManifestHandler.
         *
         * @param aContext A testing context
         */
        @Test
        public void testGetManifest(final TestContext aContext) {
            final String testData = TestUtils.getTestData(VERTX_INSTANCE, TEST_FILE_PATH);
            final String s3ManifestKey = IDUtils.getWorkS3Key(myID);
            final String apiPath = StringUtils.format(API_PATH, myID);
            final Async asyncTask = aContext.async();

            // Set up what we're going to test
            myS3Client.createBucket(BUCKET);
            myS3Client.putObject(BUCKET, s3ManifestKey, testData);

            // Run our test of the GET manifest handler
            myWebClient.get(FESTER_PORT, Constants.UNSPECIFIED_HOST, apiPath).send(request -> {
                if (request.succeeded()) {
                    final HttpResponse<Buffer> response = request.result();
                    final int statusCode = response.statusCode();

                    // Verify that the CORS header is permissive
                    aContext.assertEquals(Constants.STAR, response.getHeader(Constants.CORS_HEADER));

                    // Verify that what we get back is what we put in
                    if (statusCode == HTTP.OK) {
                        aContext.assertEquals(new JsonObject(testData), response.bodyAsJsonObject());
                    } else {
                        aContext.fail(LOGGER.getMessage(MessageCodes.MFS_004, HTTP.OK, statusCode));
                    }

                    complete(asyncTask);
                } else {
                    aContext.fail(request.cause());
                }
            });
        }

        /**
         * Confirm that a bad path request returns a 404 response.
         *
         * @param aContext A testing context
         */
        @Test
        public void testGetManifest404(final TestContext aContext) {
            final String apiPath = StringUtils.format(API_PATH, myID);
            final Async asyncTask = aContext.async();

            // Run our 404 test of the GET manifest handler
            myWebClient.get(FESTER_PORT, Constants.UNSPECIFIED_HOST, apiPath).send(request -> {
                if (request.succeeded()) {
                    final HttpResponse<Buffer> response = request.result();
                    final int statusCode = response.statusCode();

                    // Verify that we get a 404 response code
                    if (statusCode != HTTP.NOT_FOUND) {
                        aContext.fail(LOGGER.getMessage(MessageCodes.MFS_004, HTTP.NOT_FOUND, statusCode));
                    }

                    complete(asyncTask);
                } else {
                    aContext.fail(request.cause());
                }
            });
        }
    }

}
