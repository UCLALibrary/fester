
package edu.ucla.library.iiif.fester.fit;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import info.freelibrary.iiif.presentation.v2.Collection;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.ImageInfoLookup;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import edu.ucla.library.iiif.fester.utils.LinkUtils;
import edu.ucla.library.iiif.fester.utils.LinkUtilsTest;
import edu.ucla.library.iiif.fester.utils.V2ManifestLabelComparator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.multipart.MultipartForm;

/**
 * Tests related to uploading data through a CSV file.
 */
public class PostCsvFIT {

    /* A logger for the tests */
    private static final Logger LOGGER = LoggerFactory.getLogger(PostCsvFIT.class, Constants.MESSAGES);

    private static final File DIR = new File("src/test/resources");

    private static final File ALL_IN_ONE_CSV = new File(DIR, "csv/hathaway.csv");

    private static final File WORKS_CSV_COLLECTION = new File(DIR, "csv/hathaway/batch1/works.csv");

    private static final File WORKS_CSV_NO_COLLECTION = new File(DIR, "csv/hathaway/batch2/works.csv");

    private static final String HATHAWAY_COLLECTION_ARK = "ark:/21198/zz0009gsq9";

    private static final File HATHAWAY_COLLECTION_MANIFEST = new File(DIR, "json/v2/ark%3A%2F21198%2Fzz0009gsq9.json");

    private static final File BLANK_LINE_CSV = new File(DIR, "csv/blankline.csv");

    private static final File EOL_CHECK_CSV = new File(DIR, "csv/eolcheck.csv");

    private static final File WORKS_CSV_PROTESTA_1 = new File(DIR, "csv/lat_newspapers/protesta/protesta_works_1.csv");

    private static final File PROTESTA_COLLECTION_MANIFEST = new File(DIR, "json/v2/ark%3A%2F21198%2Fzz0025hqmb.json");

    /* Uploaded CSV files are named with a UUID */
    private static final String FILE_PATH_REGEX =
            "(" + BodyHandler.DEFAULT_UPLOADS_DIRECTORY + "\\/" + "[0-9a-f\\-]+" + ")";

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

            postCSV(ALL_IN_ONE_CSV, Constants.IIIF_API_V2, post -> {
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

        /**
         * Tests submitting a CSV file with an end of line character. This should result in the spreadsheet being
         * rejected with a 400 error.
         *
         * @param aContext A testing context
         */
        @Test
        public final void testCsvWithEolCharacter(final TestContext aContext) {
            final Async asyncTask = aContext.async();

            postCSV(EOL_CHECK_CSV, Constants.IIIF_API_V2, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> response = post.result();

                    aContext.assertEquals(HTTP.BAD_REQUEST, response.statusCode());

                    // The body should have a break inserted to replace the EOL
                    aContext.assertTrue(response.body().toString().contains("<br>"));

                    // The line break would be between "of" and "Persian"
                    aContext.assertTrue(response.statusMessage().startsWith(
                            "CSV data contains a forbidden hard return: Minasian (Caro) Collection of Persian"));

                    complete(asyncTask);
                } else {
                    aContext.fail(post.cause());
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

            postCSV(BLANK_LINE_CSV, Constants.IIIF_API_V2, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> response = post.result();

                    aContext.assertEquals(HTTP.CREATED, response.statusCode(), response.statusMessage());
                    complete(asyncTask);
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

            postCSV(WORKS_CSV_COLLECTION, Constants.IIIF_API_V2, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> response = post.result();
                    final int statusCode = response.statusCode();
                    final String statusMessage = response.statusMessage();

                    if (statusCode == HTTP.CREATED) {
                        final Buffer actual = response.body();
                        final String contentType = response.getHeader(Constants.CONTENT_TYPE);
                        final Optional<JsonObject> optJsonObject;
                        final List<String[]> expected;

                        // Check that what we get back is the same as what we sent
                        try {
                            expected = LinkUtilsTest.read(WORKS_CSV_COLLECTION.getAbsolutePath());
                            check(aContext, LinkUtils.addManifests(FESTER_URL, expected), actual);
                            optJsonObject = checkS3(HATHAWAY_COLLECTION_ARK, true);

                            if (optJsonObject.isPresent()) {
                                final JsonObject expectedJSON = readJsonFile(HATHAWAY_COLLECTION_MANIFEST);
                                aContext.assertEquals(expectedJSON, optJsonObject.get());
                            } else {
                                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_154, WORKS_CSV_COLLECTION));
                            }
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
            myS3Client.putObject(BUCKET, IDUtils.getCollectionS3Key(HATHAWAY_COLLECTION_ARK),
                    HATHAWAY_COLLECTION_MANIFEST);

            postCSV(WORKS_CSV_NO_COLLECTION, Constants.IIIF_API_V2, post -> {
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

            postCSV(WORKS_CSV_NO_COLLECTION, Constants.IIIF_API_V2, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> response = post.result();
                    final String expectedErrorMessage = LOGGER.getMessage(MessageCodes.MFS_103,
                            LOGGER.getMessage(MessageCodes.MFS_146, Constants.COLLECTION, HATHAWAY_COLLECTION_ARK));

                    aContext.assertEquals(response.statusCode(), HTTP.INTERNAL_SERVER_ERROR);
                    aContext.assertEquals(response.getHeader(Constants.CONTENT_TYPE), Constants.HTML_MEDIA_TYPE);
                    aContext.assertTrue(response.bodyAsString().contains(expectedErrorMessage));

                    complete(asyncTask);
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

            postCSV(ALL_IN_ONE_CSV, Constants.IIIF_API_V2, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> response = post.result();
                    final int statusCode = response.statusCode();
                    final String statusMessage = response.statusMessage();

                    if (statusCode == HTTP.CREATED) {
                        final Buffer actual = response.body();
                        final String contentType = response.getHeader(Constants.CONTENT_TYPE);
                        final Matcher matcher = Pattern.compile(FILE_PATH_REGEX).matcher(statusMessage);

                        // Check that what we get back is the same as what we sent
                        check(aContext, LinkUtils.addManifests(FESTER_URL, expected), actual);

                        // Check that what we get back has the correct media type
                        aContext.assertEquals(Constants.CSV_MEDIA_TYPE, contentType);

                        if (matcher.find()) {
                            final String filePath = matcher.group(1);
                            final Long timerDelay = 500L; // Wait 500msfor the file to get deleted

                            VERTX_INSTANCE.setTimer(timerDelay, timerId -> {
                                try {
                                    aContext.assertTrue(!VERTX_INSTANCE.fileSystem().existsBlocking(filePath));
                                } catch (final AssertionError details) {
                                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_134, filePath, timerDelay));
                                }

                                complete(asyncTask);
                            });
                        } else {
                            aContext.fail(LOGGER.getMessage(MessageCodes.MFS_135, FILE_PATH_REGEX, statusMessage));
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

            // POST a work CSV with randomly sorted rows; all work manifests generated from these rows should be
            // ordered before all those already existing on the collection manifest
            postCSV(WORKS_CSV_PROTESTA_1, Constants.IIIF_API_V2, post -> {
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
                                    final V2ManifestLabelComparator comparator = new V2ManifestLabelComparator();

                                    for (int index = 0; index < works.size() - 1; index++) {
                                        final Collection.Manifest work1 = works.get(index);
                                        final Collection.Manifest work2 = works.get(index + 1);

                                        aContext.assertTrue(comparator.compare(work1, work2) < 0);
                                    }

                                    complete(asyncTask);
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
         * Tests submitting a CSV using an outdated version of Festerize.
         *
         * @param aContext A testing context
         */
        @Test
        public final void testOutdatedFesterizeVersion(final TestContext aContext) {
            final Async asyncTask = aContext.async();

            // Create a fake Festerize client for this test only
            myWebClient.close();
            myWebClient = WebClient.create(VERTX_INSTANCE, new WebClientOptions().setUserAgent("Festerize/0.0.0"));

            postCSV(WORKS_CSV_COLLECTION, Constants.IIIF_API_V2, post -> {
                if (post.succeeded()) {
                    final HttpResponse<Buffer> response = post.result();

                    aContext.assertEquals(HTTP.BAD_REQUEST, response.statusCode());
                    aContext.assertTrue(response.body().toString().contains("Festerize is outdated, please upgrade"));

                    complete(asyncTask);
                } else {
                    aContext.fail(post.cause());
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
         * @param aIiifApiVersion A IIIF Presentation API version identifier (either Constants.IIIF_API_V2 or
         *        Constants.IIIF_API_V3)
         * @param aHandler A handler to handle the result of the post
         */
        private void postCSV(final File aTestFile, final String aIiifApiVersion,
                final Handler<AsyncResult<HttpResponse<Buffer>>> aHandler) {
            final MultipartForm form = MultipartForm.create()
                    .textFileUpload(Constants.CSV_FILE, aTestFile.getName(), aTestFile.getAbsolutePath(),
                            Constants.CSV_MEDIA_TYPE)
                    .attribute(Constants.IIIF_HOST, ImageInfoLookup.FAKE_IIIF_SERVER)
                    .attribute(Constants.IIIF_API_VERSION, aIiifApiVersion);

            myWebClient.post(FESTER_PORT, Constants.UNSPECIFIED_HOST, Constants.POST_CSV_ROUTE).sendMultipartForm(form,
                    aHandler);
        }

        /**
         * Reads a test file into a JSON object.
         *
         * @param aTestFile A test file
         * @return A JSON object from the contents of the supplied test file
         * @throws IOException If there is trouble reading the file
         */
        private JsonObject readJsonFile(final File aTestFile) throws IOException {
            return new JsonObject(StringUtils.read(aTestFile, StandardCharsets.UTF_8));
        }

        /**
         * Checks out S3 to see if the expected file is there, and if it is the method returns it.
         *
         * @param aKey An S3 key
         * @param aCollection Whether the key belongs to a collection; else, it's a work manifest's key
         * @return An optional JSON object
         */
        private Optional<JsonObject> checkS3(final String aKey, final boolean aCollection) {
            final String key;

            if (aCollection) {
                key = IDUtils.getCollectionS3Key(aKey);
            } else {
                key = IDUtils.getWorkS3Key(aKey);
            }

            if (myS3Client.doesObjectExist(BUCKET, key)) {
                try {
                    final S3Object s3Object = myS3Client.getObject(BUCKET, key);
                    final byte[] bytes = s3Object.getObjectContent().readAllBytes();

                    return Optional.of(new JsonObject(new String(bytes, StandardCharsets.UTF_8)));
                } catch (final IOException details) {
                    LOGGER.error(details, details.getMessage());
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        }
    }
}
