
package edu.ucla.library.iiif.fester.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import info.freelibrary.iiif.presentation.v3.MediaType;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.utils.TestUtils;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;

/**
 * Tests the PatchZipHandler.
 */
public class PatchZipHandlerTest extends AbstractFesterHandlerTest {

    /** The collection test artifact. */
    private static final String COLLECTION_ZIP = "src/test/resources/zip/layers-choice.zip";

    /** The test endpoint. */
    private static final String ENDPOINT = "/package";

    /**
     * Test the PatchZipHandler.
     *
     * @param aContext A testing context
     */
    @Test
    public void testCheckEndpointsHandler(final TestContext aContext) throws IOException {
        final Buffer zipBuffer = Buffer.buffer(Files.readAllBytes(Paths.get(COLLECTION_ZIP)));
        final Async asyncTask = aContext.async();
        final WebClient webClient = WebClient.create(myVertx);
        final int port = aContext.get(Config.HTTP_PORT);
        final BodyCodec<String> codec = BodyCodec.string();

        webClient.patch(port, Constants.UNSPECIFIED_HOST, ENDPOINT).expect(ResponsePredicate.SC_SUCCESS).as(codec)
                .putHeader(Constants.CONTENT_TYPE, MediaType.APPLICATION_ZIP.toString())
                .sendBuffer(zipBuffer, request -> {
                    if (request.succeeded()) {
                        final HttpResponse<String> response = request.result();

                        aContext.assertEquals(HTTP.OK, response.statusCode());
                        TestUtils.complete(asyncTask);
                    } else {
                        aContext.fail(request.cause());
                    }
                });
    }
}
