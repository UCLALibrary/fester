
package edu.ucla.library.iiif.manifeststore.handlers;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.manifeststore.Config;
import edu.ucla.library.iiif.manifeststore.Constants;
import edu.ucla.library.iiif.manifeststore.HTTP;
import edu.ucla.library.iiif.manifeststore.MessageCodes;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.multipart.MultipartForm;

/**
 * A test of the PostCollectionHandler.
 */
public class PostCollectionHandlerTest extends AbstractManifestHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostCollectionHandlerTest.class, Constants.MESSAGES);

    private static final File CSV_FILE = new File("src/test/resources/csv/capostcards.csv");

    private static final String ENDPOINT = "/collections";

    /**
     * Test set up.
     *
     * @param aContext A testing context
     */
    @Override
    @Before
    public void setUp(final TestContext aContext) throws IOException {
        super.setUp(aContext);
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
    }

    /**
     * Tests posting a CSV to the PostCollectionHandler.
     *
     * @param aContext A test context
     */
    @Test
    public final void testHandle(final TestContext aContext) {
        final int port = aContext.get(Config.HTTP_PORT);
        final WebClient webClient = WebClient.create(myVertx);
        final HttpRequest<Buffer> postRequest = webClient.post(port, Constants.UNSPECIFIED_HOST, ENDPOINT);
        final MultipartForm form = MultipartForm.create();
        final Async asyncTask = aContext.async();

        form.textFileUpload("csv-file", CSV_FILE.getName(), CSV_FILE.getAbsolutePath(), Constants.CSV_MEDIA_TYPE);

        postRequest.sendMultipartForm(form, sendMultipartForm -> {
            if (sendMultipartForm.succeeded()) {
                final HttpResponse<Buffer> postResponse = sendMultipartForm.result();
                final String postStatusMessage = postResponse.statusMessage();
                final int postStatusCode = postResponse.statusCode();

                // If batch job submission successful, submit a batch job update and check the response code
                if (postStatusCode == HTTP.CREATED) {
                    asyncTask.complete();
                } else {
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_039, postStatusCode, postStatusMessage));
                }
            } else {
                final Throwable exception = sendMultipartForm.cause();

                LOGGER.error(exception, exception.getMessage());
                aContext.fail(exception);
            }
        });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
