
package edu.ucla.library.iiif.fester.fit;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.ThumbnailUtils;
import edu.ucla.library.iiif.fester.utils.LinkUtilsTest;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.multipart.MultipartForm;

/**
 * Tests related to adding thumbnail  data to a CSV file.
 */

public class PostThumbFIT {

    /* A logger for the tests */
    private static final Logger LOGGER = LoggerFactory.getLogger(PostThumbFIT.class, Constants.MESSAGES);

    private static final File DIR = new File("src/test/resources");

    private static final File TEST_CSV = new File(DIR, "csv/fowler.csv");

    private static final String THUMB_URL = "how.do.we.do.this?";

    /**
     * Functional tests for the CSV thumbnail feature.
     */
    @RunWith(VertxUnitRunner.class)
    public static class PostThumbFT extends BaseFesterFT {

        @Override
        @Before
        public void setUpTest() {
            super.setUpTest();
        }

        @Override
        @After
        public void cleanUpTest() {
            super.cleanUpTest();
        }

        /**
         * Tests the thumbnail workflow by posting a CSV.
         *
         * @param aContext A test context
         */
        @Test
        public final void testThumbCSV(final TestContext aContext) {
            final Async asyncTask = aContext.async();

            postCSV(TEST_CSV, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> response = post.result();
                    final int statusCode = response.statusCode();
                    final String statusMessage = response.statusMessage();

                    if (statusCode == HTTP.OK) {
                        final Buffer actual = response.body();
                        final String contentType = response.getHeader(Constants.CONTENT_TYPE);
                        final List<String[]> expected;

                        // Check that what we get back is the same as what we sent
                        try {
                            expected = ThumbnailUtils.addThumbnailColumn(
                                       LinkUtilsTest.read(TEST_CSV.getAbsolutePath()));
                            check(aContext, ThumbnailUtils.addThumbnailURL(expected, THUMB_URL), actual);
                        } catch (CsvException | IOException aDetails) {
                            LOGGER.error(aDetails, aDetails.getMessage());
                            aContext.fail(aDetails);
                        }

                        // Check that what we get back has the correct media type
                        aContext.assertEquals(Constants.CSV_MEDIA_TYPE, contentType);
                        complete(asyncTask);
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

        private void postCSV(final File aTestFile, final Handler<AsyncResult<HttpResponse<Buffer>>> aHandler) {
            final MultipartForm form =
                    MultipartForm.create()
                            .textFileUpload(Constants.CSV_FILE, aTestFile.getName(), aTestFile.getAbsolutePath(),
                                    Constants.CSV_MEDIA_TYPE);

            myWebClient.post(FESTER_PORT, Constants.UNSPECIFIED_HOST,
                             Constants.POST_THUMB_ROUTE).sendMultipartForm(form, aHandler);
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
