
package edu.ucla.library.iiif.fester.handlers;

import static edu.ucla.library.iiif.fester.Constants.COLLECTIONS_PATH;
import static edu.ucla.library.iiif.fester.Constants.UNSPECIFIED_HOST;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.multipart.MultipartForm;

/**
 * A test of the PostCollectionHandler.
 */
public class PostCsvHandlerTest extends AbstractManifestHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostCsvHandlerTest.class, Constants.MESSAGES);

    private static final File FULL_CSV_FILE = new File("src/test/resources/csv/hathaway.csv");

    private static final File WORKS_CSV_FILE = new File("src/test/resources/csv/hathaway/batch1/works.csv");

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
    public final void testFullCSV(final TestContext aContext) {
        final int port = aContext.get(Config.HTTP_PORT);
        final WebClient webClient = WebClient.create(myVertx);
        final HttpRequest<Buffer> postRequest = webClient.post(port, UNSPECIFIED_HOST, COLLECTIONS_PATH);
        final String filePath = FULL_CSV_FILE.getAbsolutePath();
        final String fileName = FULL_CSV_FILE.getName();
        final MultipartForm form = MultipartForm.create();
        final FileSystem fileSystem = myVertx.fileSystem();
        final Buffer expectedCSV = fileSystem.readFileBlocking(FULL_CSV_FILE.getAbsolutePath());
        final Async asyncTask = aContext.async();

        form.textFileUpload(Constants.CSV_FILE, fileName, filePath, Constants.CSV_MEDIA_TYPE);

        postRequest.sendMultipartForm(form, postHandler -> {
            if (postHandler.succeeded()) {
                final HttpResponse<Buffer> postResponse = postHandler.result();
                final String postStatusMessage = postResponse.statusMessage();
                final int postStatusCode = postResponse.statusCode();

                if (postStatusCode == HTTP.CREATED) {
                    final Buffer actualCSV = postResponse.body();
                    final String contentType = postResponse.getHeader(Constants.CONTENT_TYPE);

                    // Check that what we get back is the same as what we sent
                    aContext.assertEquals(expectedCSV, actualCSV);

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
    public final void testCollectionWorksCSV(final TestContext aContext) {
        final int port = aContext.get(Config.HTTP_PORT);
        final WebClient webClient = WebClient.create(myVertx);
        final HttpRequest<Buffer> postRequest = webClient.post(port, UNSPECIFIED_HOST, COLLECTIONS_PATH);
        final String filePath = WORKS_CSV_FILE.getAbsolutePath();
        final String fileName = WORKS_CSV_FILE.getName();
        final MultipartForm form = MultipartForm.create();
        final FileSystem fileSystem = myVertx.fileSystem();
        final Buffer expectedCSV = fileSystem.readFileBlocking(WORKS_CSV_FILE.getAbsolutePath());
        final Async asyncTask = aContext.async();

        form.textFileUpload(Constants.CSV_FILE, fileName, filePath, Constants.CSV_MEDIA_TYPE);

        postRequest.sendMultipartForm(form, postHandler -> {
            if (postHandler.succeeded()) {
                final HttpResponse<Buffer> postResponse = postHandler.result();
                final String postStatusMessage = postResponse.statusMessage();
                final int postStatusCode = postResponse.statusCode();

                if (postStatusCode == HTTP.CREATED) {
                    final Buffer actualCSV = postResponse.body();
                    final String contentType = postResponse.getHeader(Constants.CONTENT_TYPE);

                    // Check that what we get back is the same as what we sent
                    aContext.assertEquals(expectedCSV, actualCSV);

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
}
