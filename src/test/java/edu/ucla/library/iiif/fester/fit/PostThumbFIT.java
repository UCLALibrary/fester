
package edu.ucla.library.iiif.fester.fit;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.ThumbnailUtilsTest;
import edu.ucla.library.iiif.fester.utils.TestUtils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.multipart.MultipartForm;

/**
 * Tests related to adding thumbnail data to a CSV file.
 */
public class PostThumbFIT {

    /* A logger for the tests */
    private static final Logger LOGGER = LoggerFactory.getLogger(PostThumbFIT.class, Constants.MESSAGES);

    /* base directory for test resources */
    private static final File DIR = new File("src/test/resources");

    /* file with V2 presentation manifests, no thumbnails */
    private static final File V2_TEST_CSV = new File(DIR, "csv/allied.csv");

    /* file with V2 presentation manifests and thumbnails */
    private static final File V2_THUMB_CSV = new File(DIR, "csv/allied.thumbs.csv");

    /* file with V3 presentation manifests and thumbnails */
    private static final File V3_TEST_CSV = new File(DIR, "csv/hollywoodland.csv");

    /* file with V3 presentation manifests and thumbnails */
    private static final File V3_THUMB_CSV = new File(DIR, "csv/hollywoodland.thumbs.csv");

    /**
     * Functional tests for the CSV thumbnail feature.
     */
    @RunWith(VertxUnitRunner.class)
    public static class PostThumbFT extends BaseFesterFT {

        @Rule
        public Timeout myTimeout = Timeout.seconds(600);

        /**
         * Tests the V2 thumbnail workflow by posting a CSV.
         *
         * @param aContext A test context
         */
        @Test
        @Ignore
        public final void testV2ThumbCSV(final TestContext aContext) {
            final Async asyncTask = aContext.async();

            postCSV(V2_TEST_CSV, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> response = post.result();
                    final int statusCode = response.statusCode();
                    final String statusMessage = response.statusMessage();

                    if (statusCode == HTTP.OK) {
                        final Buffer actual = response.body();
                        final String contentType = response.getHeader(Constants.CONTENT_TYPE);
                        final List<String[]> expected;

                        try {
                            expected = ThumbnailUtilsTest.read(V2_THUMB_CSV.getAbsolutePath());
                            check(aContext, expected, actual);
                        } catch (CsvException | IOException details) {
                            LOGGER.error(details, details.getMessage());
                            aContext.fail(details);
                        }

                        aContext.assertEquals(Constants.CSV_MEDIA_TYPE, contentType);
                        TestUtils.complete(asyncTask);
                    } else {
                        aContext.fail(LOGGER.getMessage(MessageCodes.MFS_039, statusCode, statusMessage));
                    }
                } else {
                    final Throwable exception = post.cause();

                    LOGGER.error(exception, exception.getMessage());
                    aContext.fail(exception);
                }
            });
        }

        /**
         * Tests the V3 thumbnail workflow by posting a CSV.
         *
         * @param aContext A test context
         */
        @Test
        @Ignore
        public final void testV3ThumbCSV(final TestContext aContext) {
            final Async asyncTask = aContext.async();

            postCSV(V3_TEST_CSV, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> response = post.result();
                    final int statusCode = response.statusCode();
                    final String statusMessage = response.statusMessage();

                    if (statusCode == HTTP.OK) {
                        final Buffer actual = response.body();
                        final String contentType = response.getHeader(Constants.CONTENT_TYPE);
                        final List<String[]> expected;

                        try {
                            expected = ThumbnailUtilsTest.read(V3_THUMB_CSV.getAbsolutePath());
                            check(aContext, expected, actual);
                        } catch (CsvException | IOException details) {
                            LOGGER.error(details, details.getMessage());
                            aContext.fail(details);
                        }

                        aContext.assertEquals(Constants.CSV_MEDIA_TYPE, contentType);
                        TestUtils.complete(asyncTask);
                    } else {
                        aContext.fail(LOGGER.getMessage(MessageCodes.MFS_039, statusCode, statusMessage));
                    }
                } else {
                    final Throwable exception = post.cause();

                    LOGGER.error(exception, exception.getMessage());
                    aContext.fail(exception);
                }
            });
        }

        /**
         * Handles posting the test CSV file to the test Fester service.
         *
         * @param aTestFile A CSV file being POSTed
         * @param aHandler A handler to handle the result of the post
         */
        private void postCSV(final File aTestFile, final Handler<AsyncResult<HttpResponse<Buffer>>> aHandler) {
            final MultipartForm form = MultipartForm.create().textFileUpload(Constants.CSV_FILE, aTestFile.getName(),
                    aTestFile.getAbsolutePath(), Constants.CSV_MEDIA_TYPE);

            myWebClient.post(FESTER_PORT, Constants.UNSPECIFIED_HOST, Constants.POST_THUMB_ROUTE)
                    .sendMultipartForm(form, aHandler);
        }

        /**
         * Checks our CSV structure/contents.
         *
         * @param aContext A testing context
         * @param aExpected An expected CSV structure
         * @param aFound A found CSV structure
         */
        private void check(final TestContext aContext, final List<String[]> aExpected, final Buffer aFound) {
            try (CSVReader reader = new CSVReader(new StringReader(aFound.toString()))) {
                final List<String[]> found = reader.readAll();

                aContext.assertEquals(aExpected.size(), found.size());

                for (int index = 0; index < aExpected.size(); index++) {
                    final String[] expectedValues = aExpected.get(index);
                    final String[] foundValues = found.get(index);

                    aContext.assertEquals(expectedValues.length, foundValues.length);

                    for (int valueIndex = 0; valueIndex < expectedValues.length; valueIndex++) {
                        aContext.assertEquals(expectedValues[valueIndex], foundValues[valueIndex]);
                    }
                }
            } catch (IOException | CsvException details) {
                aContext.fail(details);
            }
        }
    }
}
