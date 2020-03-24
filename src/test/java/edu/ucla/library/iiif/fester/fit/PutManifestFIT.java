
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
 * Tests related to putting a manifest using Fester's API.
 */
public class PutManifestFIT {

    /* A logger for the tests */
    private static final Logger LOGGER = LoggerFactory.getLogger(PutManifestFIT.class, Constants.MESSAGES);

    /* The API endpoint being tested */
    private static final String API_PATH = "/{}/manifest";

    /* The manifest we're using to test */
    private static final String TEST_FILE_PATH = "json/pages-ordered.json";

    /**
     * Functional tests related to the Fester PUT manifest endpoint.
     */
    @RunWith(VertxUnitRunner.class)
    public static class PutManifestFT extends BaseFesterFT {

        /**
         * Tests the PUT manifest endpoint
         *
         * @param aContext A testing context
         */
        @Test
        public void testPutManifest(final TestContext aContext) {
            final JsonObject testData = new JsonObject(TestUtils.getTestData(VERTX_INSTANCE, TEST_FILE_PATH));
            final String apiPath = StringUtils.format(API_PATH, myID);
            final Async asyncTask = aContext.async();

            // Set up what we're going to test
            myS3Client.createBucket(BUCKET);

            myWebClient.put(FESTER_PORT, Constants.UNSPECIFIED_HOST, apiPath).sendJsonObject(testData, put -> {
                if (put.succeeded()) {
                    final HttpResponse<Buffer> response = put.result();
                    final int statusCode = response.statusCode();

                    if (statusCode == HTTP.OK) {
                        final String s3Key = IDUtils.getWorkS3Key(myID);

                        if (!myS3Client.doesObjectExist(BUCKET, s3Key)) {
                            aContext.fail(LOGGER.getMessage(MessageCodes.MFS_018, myID, false));
                        } else {
                            LOGGER.debug(MessageCodes.MFS_083, s3Key);

                            myS3Client.deleteObject(BUCKET, s3Key);
                        }
                    } else {
                        aContext.fail(LOGGER.getMessage(MessageCodes.MFS_018, myID, response.statusMessage()));
                    }

                    complete(asyncTask);
                } else {
                    aContext.fail(put.cause());
                }
            });
        }
    }

    /**
     * Integration tests related to the Fester PUT manifest endpoint.
     */
    @RunWith(VertxUnitRunner.class)
    public static class PutManifestIT extends BaseFesterIT {

        /**
         * Tests the PUT manifest endpoint
         *
         * @param aContext A testing context
         */
        @Test
        public void testPutManifest(final TestContext aContext) {
            final JsonObject testData = new JsonObject(TestUtils.getTestData(VERTX_INSTANCE, TEST_FILE_PATH));
            final String apiPath = StringUtils.format(API_PATH, myID);
            final Async asyncTask = aContext.async();

            myWebClient.put(FESTER_PORT, Constants.UNSPECIFIED_HOST, apiPath).sendJsonObject(testData, put -> {
                if (put.succeeded()) {
                    final HttpResponse<Buffer> response = put.result();
                    final int statusCode = response.statusCode();

                    if (statusCode == HTTP.OK) {
                        final String s3Key = IDUtils.getWorkS3Key(myID);

                        if (!myS3Client.doesObjectExist(BUCKET, s3Key)) {
                            aContext.fail(LOGGER.getMessage(MessageCodes.MFS_018, myID, false));
                        } else {
                            LOGGER.debug(MessageCodes.MFS_083, s3Key);

                            myS3Client.deleteObject(BUCKET, s3Key);
                        }
                    } else {
                        aContext.fail(LOGGER.getMessage(MessageCodes.MFS_018, myID, response.statusMessage()));
                    }

                    complete(asyncTask);
                } else {
                    aContext.fail(put.cause());
                }
            });
        }
    }
}
