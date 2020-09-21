
package edu.ucla.library.iiif.fester.fit;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.opencsv.exceptions.CsvException;

import info.freelibrary.iiif.presentation.v2.Canvas;
import info.freelibrary.iiif.presentation.v2.ImageResource;
import info.freelibrary.iiif.presentation.v2.Manifest;
import info.freelibrary.iiif.presentation.v2.Sequence;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.multipart.MultipartForm;

@RunWith(VertxUnitRunner.class)
public class MissingImageFT extends BaseFesterFT {

    private static final Logger LOGGER = LoggerFactory.getLogger(MissingImageFT.class, Constants.MESSAGES);

    private static final File MISSING_IMG_FIXTURES = new File("src/test/resources/csv/missing-image");

    private static final File MINASIAN_COLLECTION = new File(MISSING_IMG_FIXTURES, "minasianmss_collection_row.csv");

    private static final File MINASIAN_WORKS = new File(MISSING_IMG_FIXTURES, "minasian_batch5_works.csv");

    private static final File MINASIAN_PAGES = new File(MISSING_IMG_FIXTURES, "minasian_batch5_pages.csv");

    private static final String MANIFEST_S3_KEY = "works/ark:/21198/zz000sw0gr.json";

    private static final String PLACEHOLDER_URL = "https://iiif.library.ucla.edu/iiif/2/blank";

    private static final String PLACEHOLDER_SAMPLE_URL = PLACEHOLDER_URL + "/full/293,/0/default.jpg";

    /**
     * Sets up testing environment.
     */
    @Override
    @Before
    public void setUpTest() {
        super.setUpTest();
        myS3Client.createBucket(BUCKET);
    }

    /**
     * Cleans up testing environment.
     */
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
     * Test that pages CSV with missing images get the missing image placeholder image.
     *
     * @param aContext A testing context
     * @throws CsvException If there is trouble parsing the CSV test fixtures
     * @throws IOException If there is trouble reading the CSV test fixtures
     */
    @Test
    @SuppressWarnings("checkstyle:indentation")
    public final void testPagesCsv(final TestContext aContext) throws CsvException, IOException {
        final Async asyncTask = aContext.async();
        final Promise<Void> collectionPromise = Promise.promise();
        final Promise<Void> worksPromise = Promise.promise();
        final Promise<Void> pagesPromise = Promise.promise();

        // Check the result of the collection manifest ingest
        collectionPromise.future().onComplete(collectionHandler -> {
            if (collectionHandler.succeeded()) {
                // Check the result of the works manifest ingest
                worksPromise.future().onComplete(worksHandler -> {
                    if (worksHandler.succeeded()) {
                        // Check the result of the pages manifest ingest
                        pagesPromise.future().onComplete(pagesHandler -> {
                            if (pagesHandler.succeeded()) {
                                // Check that missing image page manifest has the placeholder URL
                                checkResults(asyncTask, aContext);
                            } else {
                                aContext.fail(pagesHandler.cause());
                            }
                        });

                        // Ingest works' pages, updating works manifests
                        ingestCSV(MultipartForm.create().textFileUpload(Constants.CSV_FILE, MINASIAN_PAGES.getName(),
                                MINASIAN_PAGES.getAbsolutePath(), Constants.CSV_MEDIA_TYPE), pagesPromise);
                    } else {
                        aContext.fail(worksHandler.cause());
                    }
                });

                // Ingest works manifests
                ingestCSV(MultipartForm.create().textFileUpload(Constants.CSV_FILE, MINASIAN_WORKS.getName(),
                        MINASIAN_WORKS.getAbsolutePath(), Constants.CSV_MEDIA_TYPE), worksPromise);
            } else {
                aContext.fail(collectionHandler.cause());
            }
        });

        // Ingest collection manifest
        ingestCSV(MultipartForm.create().textFileUpload(Constants.CSV_FILE, MINASIAN_COLLECTION.getName(),
                MINASIAN_COLLECTION.getAbsolutePath(), Constants.CSV_MEDIA_TYPE), collectionPromise);
    }

    /**
     * Ingest the supplied CSV into Fester.
     *
     * @param aForm A multipart form submission
     * @param aPromise A promise that the ingest happens
     */
    @SuppressWarnings("checkstyle:indentation")
    private void ingestCSV(final MultipartForm aForm, final Promise<Void> aPromise) {
        myWebClient.post(FESTER_PORT, Constants.UNSPECIFIED_HOST, Constants.POST_CSV_ROUTE).sendMultipartForm(aForm,
                post -> {
                    if (post.succeeded()) {
                        final HttpResponse<Buffer> response = post.result();
                        final int statusCode = response.statusCode();

                        if (statusCode == HTTP.CREATED) {
                            aPromise.complete();
                        } else {
                            final String statusMessage = response.statusMessage();
                            aPromise.fail(LOGGER.getMessage(MessageCodes.MFS_039, statusCode, statusMessage));
                        }
                    } else {
                        aPromise.fail(post.cause());
                    }
                });
    }

    /**
     * Check the results of our test.
     *
     * @param aAsyncTask An asynchronous task
     */
    private void checkResults(final Async aAsyncTask, final TestContext aContext) {
        final Manifest manifest = Manifest.fromString(myS3Client.getObjectAsString(BUCKET, MANIFEST_S3_KEY));
        final List<Sequence> sequences = manifest.getSequences();
        final Canvas coverPage = sequences.get(0).getCanvases().get(0);
        final ImageResource image = coverPage.getImageContent().get(0).getResources().get(0);

        // Check that we have the right page and that it's image resource URL is set to our placeholder
        aContext.assertEquals("cover page", coverPage.getLabel().getString());
        aContext.assertEquals(PLACEHOLDER_SAMPLE_URL, image.getID().toString());
        aContext.assertEquals(PLACEHOLDER_URL, image.getService().get().getID().toString());

        if (!aAsyncTask.isCompleted()) {
            aAsyncTask.complete();
        }
    }
}
