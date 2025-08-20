
package edu.ucla.library.iiif.fester.fit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Test;
import org.junit.runner.RunWith;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.utils.TestUtils;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;

/**
 * Functional tests that test the batch ingest feature flag while the feature is off.
 */
@RunWith(VertxUnitRunner.class)
public class BatchIngestFfOffT extends BaseFesterFfT {

    /* Our feature flag test logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchIngestFfOffT.class, Constants.MESSAGES);

    /**
     * Tests that the POST collections endpoint returns a 503.
     *
     * @param aContext A testing context
     */
    @Test
    @SuppressWarnings("checkstyle:indentation") // Checkstyle doesn't handle lambda indentations well
    public final void testCsvPostEndpoint(final TestContext aContext) {
        final Async asyncTask = aContext.async();

        myWebClient.post(FESTER_PORT, Constants.UNSPECIFIED_HOST, Constants.POST_CSV_ROUTE)
                .sendMultipartForm(CSV_UPLOAD_FORM, request -> {
                    if (request.succeeded()) {
                        final HttpResponse<Buffer> response = request.result();
                        final String expectedStatusMessage =
                                LOGGER.getMessage(MessageCodes.MFS_085, BATCH_INGEST_FEATURE);

                        aContext.assertEquals(HTTP.SERVICE_UNAVAILABLE, response.statusCode());
                        aContext.assertEquals(expectedStatusMessage, response.statusMessage());

                        TestUtils.complete(asyncTask);
                    } else {
                        aContext.fail(request.cause());
                    }
                });
    }

    /**
     * Tests that the upload form has been replaced by a feature off notification.
     *
     * @param aContext A testing context
     */
    @Test
    public final void testCsvUploadPage(final TestContext aContext) {
        final Async asyncTask = aContext.async();

        myWebClient.get(FESTER_PORT, Constants.UNSPECIFIED_HOST, UPLOAD_FORM_PATH).send(request -> {
            if (request.succeeded()) {
                final String html = request.result().bodyAsString();
                final Element h1 = Jsoup.parse(html).selectFirst("h1");

                // Our CSV upload page should be replaced by a "Content Unavailable" page
                if (h1 != null) {
                    aContext.assertEquals("Content Unavailable", h1.text());
                } else {
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_087));
                }

                TestUtils.complete(asyncTask);
            } else {
                aContext.fail(request.cause());
            }
        });
    }

}
