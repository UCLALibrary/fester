
package edu.ucla.library.iiif.fester.handlers;

import static edu.ucla.library.iiif.fester.Constants.COLLECTIONS_PATH;
import static edu.ucla.library.iiif.fester.Constants.UNSPECIFIED_HOST;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Test;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.ImageInfoLookup;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.LinkUtils;
import edu.ucla.library.iiif.fester.utils.LinkUtilsTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.multipart.MultipartForm;

/**
 * A test of the PostCollectionHandler.
 */
public class PostCsvHandlerTest extends AbstractFesterHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostCsvHandlerTest.class, Constants.MESSAGES);

    private static final File FULL_CSV_FILE = new File("src/test/resources/csv/hathaway.csv");

    private static final File COLL_WORKS_CSV_FILE = new File("src/test/resources/csv/hathaway/batch1/works.csv");

    private static final File WORKS_CSV_FILE = new File("src/test/resources/csv/hathaway/batch2/works.csv");

    private static final String HOST = "http://0.0.0.0:{}";

    /**
     * Test tear down.
     *
     * @param aContext A testing context
     */
    @Override
    @After
    public void tearDown(final TestContext aContext) {
        super.tearDown(aContext);
    }

    /**
     * Tests posting a CSV to the PostCollectionHandler.
     *
     * @param aContext A test context
     */
    @Test
    public final void testFullCSV(final TestContext aContext) throws CsvException, IOException {
        final int port = aContext.get(Config.HTTP_PORT);
        final WebClient webClient = WebClient.create(myVertx);
        final HttpRequest<Buffer> postRequest = webClient.post(port, UNSPECIFIED_HOST, COLLECTIONS_PATH);
        final String filePath = FULL_CSV_FILE.getAbsolutePath();
        final String fileName = FULL_CSV_FILE.getName();
        final MultipartForm form = MultipartForm.create();
        final List<String[]> expected = LinkUtilsTest.read(FULL_CSV_FILE.getAbsolutePath());
        final String host = StringUtils.format(HOST, port);
        final Async asyncTask = aContext.async();

        form.textFileUpload(Constants.CSV_FILE, fileName, filePath, Constants.CSV_MEDIA_TYPE);

        postRequest.sendMultipartForm(form, postHandler -> {
            if (postHandler.succeeded()) {
                final HttpResponse<Buffer> postResponse = postHandler.result();
                final String postStatusMessage = postResponse.statusMessage();
                final int postStatusCode = postResponse.statusCode();

                if (postStatusCode == HTTP.CREATED) {
                    final Buffer actual = postResponse.body();
                    final String contentType = postResponse.getHeader(Constants.CONTENT_TYPE);

                    // Check that what we get back is the same as what we sent
                    check(aContext, LinkUtils.addManifests(host, expected), actual);

                    // Check that what we get back has the correct media type
                    aContext.assertEquals(Constants.CSV_MEDIA_TYPE, contentType);

                    if (!asyncTask.isCompleted()) {
                        asyncTask.complete();
                    }
                } else {
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_039, postStatusCode, postStatusMessage));
                }
            } else {
                final Throwable exception = postHandler.cause();

                LOGGER.error(exception, exception.getMessage());
                aContext.fail(exception);
            }
        });
    }

    /**
     * Tests posting a CSV to the PostCollectionHandler with a supplied IIIF host.
     *
     * @param aContext A test context
     */
    @Test
    public final void testFullCsvWithIiifHost(final TestContext aContext) throws IOException, CsvException {
        final int port = aContext.get(Config.HTTP_PORT);
        final WebClient webClient = WebClient.create(myVertx);
        final HttpRequest<Buffer> postRequest = webClient.post(port, UNSPECIFIED_HOST, COLLECTIONS_PATH);
        final String filePath = FULL_CSV_FILE.getAbsolutePath();
        final String fileName = FULL_CSV_FILE.getName();
        final MultipartForm form = MultipartForm.create();
        final List<String[]> expected = LinkUtilsTest.read(FULL_CSV_FILE.getAbsolutePath());
        final String host = StringUtils.format(HOST, port);
        final Async asyncTask = aContext.async();

        form.textFileUpload(Constants.CSV_FILE, fileName, filePath, Constants.CSV_MEDIA_TYPE);
        form.attribute(Constants.IIIF_HOST, ImageInfoLookup.FAKE_IIIF_SERVER);

        postRequest.sendMultipartForm(form, postHandler -> {
            if (postHandler.succeeded()) {
                final HttpResponse<Buffer> postResponse = postHandler.result();
                final String postStatusMessage = postResponse.statusMessage();
                final int postStatusCode = postResponse.statusCode();

                if (postStatusCode == HTTP.CREATED) {
                    final Buffer actual = postResponse.body();
                    final String contentType = postResponse.getHeader(Constants.CONTENT_TYPE);

                    // Check that what we get back is the same as what we sent
                    check(aContext, LinkUtils.addManifests(host, expected), actual);

                    // Check that what we get back has the correct media type
                    aContext.assertEquals(Constants.CSV_MEDIA_TYPE, contentType);

                    if (!asyncTask.isCompleted()) {
                        asyncTask.complete();
                    }
                } else {
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_039, postStatusCode, postStatusMessage));
                }
            } else {
                final Throwable exception = postHandler.cause();

                LOGGER.error(exception, exception.getMessage());
                aContext.fail(exception);
            }
        });
    }

    /**
     * Tests posting a Collection + Works CSV to the PostCollectionHandler.
     *
     * @param aContext A test context
     */
    @Test
    public final void testCollectionWorksCSV(final TestContext aContext) throws IOException, CsvException {
        final int port = aContext.get(Config.HTTP_PORT);
        final WebClient webClient = WebClient.create(myVertx);
        final HttpRequest<Buffer> postRequest = webClient.post(port, UNSPECIFIED_HOST, COLLECTIONS_PATH);
        final String filePath = COLL_WORKS_CSV_FILE.getAbsolutePath();
        final String fileName = COLL_WORKS_CSV_FILE.getName();
        final MultipartForm form = MultipartForm.create();
        final List<String[]> expected = LinkUtilsTest.read(COLL_WORKS_CSV_FILE.getAbsolutePath());
        final String host = StringUtils.format(HOST, port);
        final Async asyncTask = aContext.async();

        form.textFileUpload(Constants.CSV_FILE, fileName, filePath, Constants.CSV_MEDIA_TYPE);

        postRequest.sendMultipartForm(form, postHandler -> {
            if (postHandler.succeeded()) {
                final HttpResponse<Buffer> postResponse = postHandler.result();
                final String postStatusMessage = postResponse.statusMessage();
                final int postStatusCode = postResponse.statusCode();

                if (postStatusCode == HTTP.CREATED) {
                    final Buffer actual = postResponse.body();
                    final String contentType = postResponse.getHeader(Constants.CONTENT_TYPE);

                    // Check that what we get back is the same as what we sent
                    check(aContext, LinkUtils.addManifests(host, expected), actual);

                    // Check that what we get back has the correct media type
                    aContext.assertEquals(Constants.CSV_MEDIA_TYPE, contentType);

                    if (!asyncTask.isCompleted()) {
                        asyncTask.complete();
                    }
                } else {
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_039, postStatusCode, postStatusMessage));
                }
            } else {
                final Throwable exception = postHandler.cause();

                LOGGER.error(exception, exception.getMessage());
                aContext.fail(exception);
            }
        });
    }

    /**
     * Tests posting a CSV to the PostCollectionHandler.
     *
     * @param aContext A test context
     */
    @Test
    public final void testWorksCSV(final TestContext aContext) throws IOException, CsvException {
        final int port = aContext.get(Config.HTTP_PORT);
        final WebClient webClient = WebClient.create(myVertx);
        final HttpRequest<Buffer> postRequest = webClient.post(port, UNSPECIFIED_HOST, COLLECTIONS_PATH);
        final String filePath = WORKS_CSV_FILE.getAbsolutePath();
        final String fileName = WORKS_CSV_FILE.getName();
        final MultipartForm form = MultipartForm.create();
        final List<String[]> expected = LinkUtilsTest.read(WORKS_CSV_FILE.getAbsolutePath());
        final String host = StringUtils.format(HOST, port);
        final Async asyncTask = aContext.async();

        form.textFileUpload(Constants.CSV_FILE, fileName, filePath, Constants.CSV_MEDIA_TYPE);

        postRequest.sendMultipartForm(form, postHandler -> {
            if (postHandler.succeeded()) {
                final HttpResponse<Buffer> postResponse = postHandler.result();
                final String postStatusMessage = postResponse.statusMessage();
                final int postStatusCode = postResponse.statusCode();

                if (postStatusCode == HTTP.CREATED) {
                    final Buffer actual = postResponse.body();
                    final String contentType = postResponse.getHeader(Constants.CONTENT_TYPE);

                    // Check that what we get back is the same as what we sent
                    check(aContext, LinkUtils.addManifests(host, expected), actual);

                    // Check that what we get back has the correct media type
                    aContext.assertEquals(Constants.CSV_MEDIA_TYPE, contentType);

                    if (!asyncTask.isCompleted()) {
                        asyncTask.complete();
                    }
                } else {
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_039, postStatusCode, postStatusMessage));
                }
            } else {
                final Throwable exception = postHandler.cause();

                LOGGER.error(exception, exception.getMessage());
                aContext.fail(exception);
            }
        });
    }

    /**
     * Ensures that uploaded files are deleted.
     *
     * @param aContext A test context
     */
    @Test
    public final void testDeleteUploadedFilesOnEnd(final TestContext aContext) throws IOException, CsvException {
        final int port = aContext.get(Config.HTTP_PORT);
        final WebClient webClient = WebClient.create(myVertx);
        final HttpRequest<Buffer> postRequest = webClient.post(port, UNSPECIFIED_HOST, COLLECTIONS_PATH);
        final String filePath = FULL_CSV_FILE.getAbsolutePath();
        final String fileName = FULL_CSV_FILE.getName();
        final MultipartForm form = MultipartForm.create();
        final List<String[]> expected = LinkUtilsTest.read(FULL_CSV_FILE.getAbsolutePath());
        final String host = StringUtils.format(HOST, port);
        final Async asyncTask = aContext.async();

        form.textFileUpload(Constants.CSV_FILE, fileName, filePath, Constants.CSV_MEDIA_TYPE);

        postRequest.sendMultipartForm(form, postHandler -> {
            if (postHandler.succeeded()) {
                final HttpResponse<Buffer> postResponse = postHandler.result();
                final String postStatusMessage = postResponse.statusMessage();
                final int postStatusCode = postResponse.statusCode();

                if (postStatusCode == HTTP.CREATED) {
                    final Buffer actual = postResponse.body();
                    final String contentType = postResponse.getHeader(Constants.CONTENT_TYPE);
                    // Uploaded file is named with a UUID
                    final String uploadedFilePathRegex = "(" + BodyHandler.DEFAULT_UPLOADS_DIRECTORY + "\\/"
                            + "[0-9a-f\\-]+" + ")";
                    final Matcher uploadedFilePathMatcher = Pattern.compile(uploadedFilePathRegex)
                            .matcher(postStatusMessage);

                    // Check that what we get back is the same as what we sent
                    check(aContext, LinkUtils.addManifests(host, expected), actual);

                    // Check that what we get back has the correct media type
                    aContext.assertEquals(Constants.CSV_MEDIA_TYPE, contentType);

                    if (uploadedFilePathMatcher.find()) {
                        final String uploadedFilePath = uploadedFilePathMatcher.group(1);
                        // Wait for the file to get deleted; 500 ms should be long enough, unless the file is huge
                        final Long timerDelay = 500L;

                        myVertx.setTimer(timerDelay, timerId -> {
                            final boolean isDeleted = !myVertx.fileSystem().existsBlocking(uploadedFilePath);
                            try {
                                aContext.assertTrue(isDeleted);
                            } catch (AssertionError details) {
                                aContext.fail(LOGGER.getMessage(MessageCodes.MFS_134, uploadedFilePath, timerDelay));
                            }

                            if (!asyncTask.isCompleted()) {
                                asyncTask.complete();
                            }
                        });
                    } else {
                        aContext.fail(
                                LOGGER.getMessage(MessageCodes.MFS_135, uploadedFilePathRegex, postStatusMessage));
                    }
                } else {
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_039, postStatusCode, postStatusMessage));
                }
            } else {
                final Throwable exception = postHandler.cause();

                LOGGER.error(exception, exception.getMessage());
                aContext.fail(exception);
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
}
