
package edu.ucla.library.iiif.fester.fit;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import info.freelibrary.iiif.presentation.Collection;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.ImageInfoLookup;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import edu.ucla.library.iiif.fester.utils.LinkUtils;
import edu.ucla.library.iiif.fester.utils.LinkUtilsTest;
import edu.ucla.library.iiif.fester.utils.ManifestLabelComparator;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.multipart.MultipartForm;

public class PostCsvFIT {

    /* A logger for the tests */
    private static final Logger LOGGER = LoggerFactory.getLogger(PostCsvFIT.class, Constants.MESSAGES);

    private static final File DIR = new File("src/test/resources");

    private static final File ALL_IN_ONE_CSV = new File(DIR, "csv/hathaway.csv");

    private static final File WORKS_CSV_COLLECTION = new File(DIR, "csv/hathaway/batch1/works.csv");

    private static final File WORKS_CSV_NO_COLLECTION = new File(DIR, "csv/hathaway/batch2/works.csv");

    private static final File HATHAWAY_COLLECTION_MANIFEST = new File(DIR, "json/ark%3A%2F21198%2Fzz0009gsq9.json");

    private static final File BLANK_LINE_CSV = new File(DIR, "csv/blankline.csv");

    private static final File WORKS_CSV_PROTESTA_1 = new File(DIR, "csv/lat_newspapers/protesta/protesta_works_1.csv");

    private static final File PROTESTA_COLLECTION_MANIFEST = new File(DIR, "json/ark%3A%2F21198%2Fzz0025hqmb.json");

    /* Uploaded CSV files are named with a UUID */
    private static final String UPLOADED_FILE_PATH_REGEX = "(" + BodyHandler.DEFAULT_UPLOADS_DIRECTORY + "\\/" +
            "[0-9a-f\\-]+" + ")";

    /**
     * Functional tests for the CSV upload feature.
     */
    @RunWith(VertxUnitRunner.class)
    public static class PostCsvFT extends BaseFesterFT {

        @Override
        @Before
        public void setUpTest() {
            super.setUpTest();
            myS3Client.createBucket(BUCKET);
        }

        @Override
        @After
        public void cleanUpTest() {
            super.cleanUpTest();

            // Delete the S3 bucket
            for (final S3ObjectSummary summary : myS3Client.listObjectsV2(BUCKET).getObjectSummaries()) {
                myS3Client.deleteObject(BUCKET, summary.getKey());
            }

            myS3Client.deleteBucket(BUCKET);
        }

        /**
         * Tests the single-upload workflow by posting an "all-in-one" CSV.
         *
         * @param aContext A test context
         */
        @Test
        public final void testFullCSV(final TestContext aContext) {
            final Async asyncTask = aContext.async();

            postCSV(ALL_IN_ONE_CSV, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> response = post.result();
                    final int statusCode = response.statusCode();
                    final String statusMessage = response.statusMessage();

                    if (statusCode == HTTP.CREATED) {
                        final Buffer actual = response.body();
                        final String contentType = response.getHeader(Constants.CONTENT_TYPE);
                        final List<String[]> expected;

                        // Check that what we get back is the same as what we sent
                        try {
                            expected = LinkUtilsTest.read(ALL_IN_ONE_CSV.getAbsolutePath());
                            check(aContext, LinkUtils.addManifests(FESTER_URL, expected), actual);
                        } catch (CsvException | IOException aDetails) {
                            LOGGER.error(aDetails, aDetails.getMessage());
                            aContext.fail(aDetails);
                        }

                        // Check that what we get back has the correct media type
                        aContext.assertEquals(Constants.CSV_MEDIA_TYPE, contentType);

                        if (!asyncTask.isCompleted()) {
                            asyncTask.complete();
                        }
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
         * Tests submitting a CSV file with a blank line. This should succeed with the blank line being ignored.
         *
         * @param aContext A testing context
         */
        @Test
        public final void testCsvWithBlankLine(final TestContext aContext) {
            final Async asyncTask = aContext.async();

            postCSV(BLANK_LINE_CSV, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> response = post.result();

                    aContext.assertEquals(HTTP.CREATED, response.statusCode(), response.statusMessage());

                    if (!asyncTask.isCompleted()) {
                        asyncTask.complete();
                    }
                } else {
                    aContext.fail(post.cause());
                }
            });
        }

        /**
         * Tests the multi-upload workflow by posting a works CSV with a collection row.
         *
         * @param aContext A test context
         * @throws IOException If there is trouble reading a manifest
         * @throws CsvException If there is trouble reading the CSV data
         */
        @Test
        public final void testWorksCSV(final TestContext aContext) {
            final Async asyncTask = aContext.async();

            postCSV(WORKS_CSV_COLLECTION, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> response = post.result();
                    final int statusCode = response.statusCode();
                    final String statusMessage = response.statusMessage();

                    if (statusCode == HTTP.CREATED) {
                        final Buffer actual = response.body();
                        final String contentType = response.getHeader(Constants.CONTENT_TYPE);
                        final List<String[]> expected;

                        // Check that what we get back is the same as what we sent
                        try {
                            expected = LinkUtilsTest.read(WORKS_CSV_COLLECTION.getAbsolutePath());
                            check(aContext, LinkUtils.addManifests(FESTER_URL, expected), actual);
                        } catch (CsvException | IOException aDetails) {
                            LOGGER.error(aDetails, aDetails.getMessage());
                            aContext.fail(aDetails);
                        }

                        // Check that what we get back has the correct media type
                        aContext.assertEquals(Constants.CSV_MEDIA_TYPE, contentType);

                        if (!asyncTask.isCompleted()) {
                            asyncTask.complete();
                        }
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
         * Tests the multi-upload workflow by posting a works CSV without a collection row, when a collection manifest
         * already exists for the included works.
         *
         * @param aContext A test context
         * @throws IOException If there is trouble reading a manifest
         * @throws CsvException If there is trouble reading the CSV data
         */
        @Test
        public final void testWorksCSVNoCollectionRow(final TestContext aContext) {
            final Async asyncTask = aContext.async();

            // Put a collection manifest in Fester
            myS3Client.putObject(BUCKET, IDUtils.getCollectionS3Key("ark:/21198/zz0009gsq9"),
                    HATHAWAY_COLLECTION_MANIFEST);

            postCSV(WORKS_CSV_NO_COLLECTION, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> response = post.result();
                    final int statusCode = response.statusCode();
                    final String statusMessage = response.statusMessage();

                    if (statusCode == HTTP.CREATED) {
                        final Buffer actual = response.body();
                        final String contentType = response.getHeader(Constants.CONTENT_TYPE);
                        final List<String[]> expected;

                        // Check that what we get back is the same as what we sent
                        try {
                            expected = LinkUtilsTest.read(WORKS_CSV_NO_COLLECTION.getAbsolutePath());
                            check(aContext, LinkUtils.addManifests(FESTER_URL, expected), actual);
                        } catch (CsvException | IOException aDetails) {
                            LOGGER.error(aDetails, aDetails.getMessage());
                            aContext.fail(aDetails);
                        }

                        // Check that what we get back has the correct media type
                        aContext.assertEquals(Constants.CSV_MEDIA_TYPE, contentType);

                        if (!asyncTask.isCompleted()) {
                            asyncTask.complete();
                        }
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
         * Tests the multi-upload workflow by posting a works CSV without a collection row, when a collection manifest
         * doesn't already exist for the included works.
         *
         * @param aContext A test context
         * @throws IOException If there is trouble reading a manifest
         * @throws CsvException If there is trouble reading the CSV data
         */
        @Test
        public final void testWorksCSVNoCollectionRow500(final TestContext aContext) {
            final Async asyncTask = aContext.async();

            postCSV(WORKS_CSV_NO_COLLECTION, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> response = post.result();

                    aContext.assertEquals(response.statusCode(), HTTP.INTERNAL_SERVER_ERROR);
                    aContext.assertEquals(response.getHeader(Constants.CONTENT_TYPE), Constants.HTML_MEDIA_TYPE);
                    aContext.assertTrue(response.bodyAsString().contains("Manifest generation failed: Not Found"));

                    if (!asyncTask.isCompleted()) {
                        asyncTask.complete();
                    }
                } else {
                    final Throwable exception = post.cause();

                    LOGGER.error(exception, exception.getMessage());
                    aContext.fail(exception);
                }
            });
        }

        /**
         * Ensures that uploaded files are deleted.
         *
         * @param aContext A test context
         * @throws IOException If there is trouble reading a manifest
         * @throws CsvException If there is trouble reading the CSV data
         */
        @Test
        public final void testDeleteUploadedFilesOnEnd(final TestContext aContext) throws IOException, CsvException {
            final Async asyncTask = aContext.async();
            final List<String[]> expected = LinkUtilsTest.read(ALL_IN_ONE_CSV.getAbsolutePath());

            postCSV(ALL_IN_ONE_CSV, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> response = post.result();
                    final int statusCode = response.statusCode();
                    final String statusMessage = response.statusMessage();

                    if (statusCode == HTTP.CREATED) {
                        final Buffer actual = response.body();
                        final String contentType = response.getHeader(Constants.CONTENT_TYPE);
                        final Matcher uploadedFilePathMatcher = Pattern.compile(UPLOADED_FILE_PATH_REGEX).matcher(
                                statusMessage);

                        // Check that what we get back is the same as what we sent
                        check(aContext, LinkUtils.addManifests(FESTER_URL, expected), actual);

                        // Check that what we get back has the correct media type
                        aContext.assertEquals(Constants.CSV_MEDIA_TYPE, contentType);

                        if (uploadedFilePathMatcher.find()) {
                            final String uploadedFilePath = uploadedFilePathMatcher.group(1);
                            // Wait for the file to get deleted; 500 ms should be long enough,
                            /// unless the file is huge
                            final Long timerDelay = 500L;

                            VERTX_INSTANCE.setTimer(timerDelay, timerId -> {
                                final boolean isDeleted = !VERTX_INSTANCE.fileSystem().existsBlocking(
                                        uploadedFilePath);
                                try {
                                    aContext.assertTrue(isDeleted);
                                } catch (final AssertionError details) {
                                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_134, uploadedFilePath,
                                            timerDelay));
                                }

                                if (!asyncTask.isCompleted()) {
                                    asyncTask.complete();
                                }
                            });
                        } else {
                            aContext.fail(LOGGER.getMessage(MessageCodes.MFS_135, UPLOADED_FILE_PATH_REGEX,
                                    statusMessage));
                        }
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
         * Tests the alpha-numeric sorting of manifests on a collection.
         *
         * @param aContext A test context
         */
        @Test
        public final void testCollectionManifestWorkSorting(final TestContext aContext) {
            final Async asyncTask = aContext.async();
            final String s3Key = IDUtils.getCollectionS3Key("ark:/21198/zz0025hqmb");

            // PUT a collection manifest (that already has work manifests on it) in S3
            myS3Client.putObject(BUCKET, s3Key, PROTESTA_COLLECTION_MANIFEST);

            // POST a work CSV with randomly sorted rows; all work manifests generated from these rows should be ordered
            // before all those already existing on the collection manifest
            postCSV(WORKS_CSV_PROTESTA_1, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> postResponse = post.result();
                    final int postStatusCode = postResponse.statusCode();
                    final String postStatusMessage = postResponse.statusMessage();

                    if (postStatusCode == HTTP.CREATED) {
                        final String getPath = IDUtils.getResourceURIPath(s3Key);

                        // Retrieve the updated manifest and check it
                        myWebClient.get(FESTER_PORT, Constants.UNSPECIFIED_HOST, getPath).send(get -> {
                            if (get.succeeded()) {
                                final HttpResponse<Buffer> getResponse = get.result();
                                final int getStatusCode = getResponse.statusCode();
                                final String getStatusMessage = getResponse.statusMessage();

                                if (getStatusCode == HTTP.OK) {
                                    final JsonObject json = get.result().bodyAsJsonObject();
                                    final List<Collection.Manifest> works = Collection.fromJSON(json).getManifests();
                                    final ManifestLabelComparator comparator = new ManifestLabelComparator();

                                    for (int i = 0; i < works.size() - 1; i++) {
                                        aContext.assertTrue(comparator.compare(works.get(i), works.get(i + 1)) < 0);
                                    }

                                    if (!asyncTask.isCompleted()) {
                                        asyncTask.complete();
                                    }
                                } else {
                                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_097, s3Key, getStatusMessage));
                                }
                            } else {
                                final Throwable getException = get.cause();

                                LOGGER.error(getException, getException.getMessage());
                                aContext.fail(getException);
                            }
                        });
                    } else {
                        aContext.fail(LOGGER.getMessage(MessageCodes.MFS_039, postStatusCode, postStatusMessage));
                    }
                } else {
                    final Throwable postException = post.cause();

                    LOGGER.error(postException, postException.getMessage());
                    aContext.fail(postException);
                }
            });
        }

        /**
         * Checks our CSV structures.
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

        /**
         * Handles posting the test CSV file to the test Fester service.
         *
         * @param aTestFile A CSV file being POSTed
         * @param aHandler A handler to handle the result of the post
         */
        private void postCSV(final File aTestFile, final Handler<AsyncResult<HttpResponse<Buffer>>> aHandler) {
            final MultipartForm form = MultipartForm.create().textFileUpload(Constants.CSV_FILE, aTestFile.getName(),
                    aTestFile.getAbsolutePath(), Constants.CSV_MEDIA_TYPE).attribute(Constants.IIIF_HOST,
                            ImageInfoLookup.FAKE_IIIF_SERVER);

            myWebClient.post(FESTER_PORT, Constants.UNSPECIFIED_HOST, Constants.POST_CSV_ROUTE).sendMultipartForm(
                    form, aHandler);
        }
    }
}
