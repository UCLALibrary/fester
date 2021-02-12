
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

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * Functional tests that test the batch ingest feature flag while the feature is on.
 */
@RunWith(VertxUnitRunner.class)
public class BatchIngestFfOnT extends BaseFesterFfT {

    /* Our feature flag test logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchIngestFfOnT.class, Constants.MESSAGES);

    /**
     * Tests that the POST collections endpoint returns a 201 on success.
     *
     * @param aContext A testing context
     */
    @Test
    public final void testCsvPostEndpoint(final TestContext aContext) {
        final Async asyncTask = aContext.async();

        // Create our bucket so we have some place to put the manifests
        myS3Client.createBucket(BUCKET);

        myWebClient.post(FESTER_PORT, Constants.UNSPECIFIED_HOST, Constants.POST_CSV_ROUTE)
                .sendMultipartForm(CSV_UPLOAD_FORM, request -> {
                    if (request.succeeded()) {
                        // Check that we get a 201 response code
                        aContext.assertEquals(HTTP.CREATED, request.result().statusCode());
                        complete(asyncTask);
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

                // Check that we get the expected CSV upload page
                if (h1 != null) {
                    aContext.assertEquals("CSV Upload", h1.text());
                } else {
                    aContext.fail(LOGGER.getMessage(MessageCodes.MFS_087));
                }

                complete(asyncTask);
            } else {
                aContext.fail(request.cause());
            }
        });
    }

}
