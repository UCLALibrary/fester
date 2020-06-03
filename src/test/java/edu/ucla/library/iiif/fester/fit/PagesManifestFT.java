
package edu.ucla.library.iiif.fester.fit;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.opencsv.exceptions.CsvException;

import info.freelibrary.iiif.presentation.Manifest;
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
public class PagesManifestFT extends BaseFesterFT {

    private static final Logger LOGGER = LoggerFactory.getLogger(PagesManifestFT.class, Constants.MESSAGES);

    private static final File MINASIAN_FIXTURES = new File("src/test/resources/csv/minasian");

    private static final File MINASIAN_COLLECTION = new File(MINASIAN_FIXTURES, "minasianmss_collection_row.csv");

    private static final File MINASIAN_WORKS = new File(MINASIAN_FIXTURES, "minasian_batch5_works.csv");

    private static final File MINASIAN_PAGES = new File(MINASIAN_FIXTURES, "minasian_batch5_pages.csv");

    private static final String[] MANIFESTS = new String[] { "works/ark:/21198/zz000srx2s.json",
        "works/ark:/21198/zz000sw0gr.json", "works/ark:/21198/zz000wrbz8.json", "works/ark:/21198/zz000wrgm1.json",
        "works/ark:/21198/zz000wrh7t.json" };

    private static final int[] CANVAS_COUNTS = new int[] { 42, 26, 46, 39, 36 };

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
     * Tests that pages have been added to works manifests. Several things have to happen in sequential order before
     * we can check the results of the test: the collection CSV must be loaded, the works CSV must be loaded, and
     * finally the pages CSV must be loaded. After this has all happened, we can check the canvas counts in each of
     * the work manifests to confirm all the pages were loaded.
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
                                // Check that pages have been added to the works manifests
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
        for (int index = 0; index < MANIFESTS.length; index++) {
            final Manifest manifest = Manifest.fromString(myS3Client.getObjectAsString(BUCKET, MANIFESTS[index]));
            aContext.assertEquals(CANVAS_COUNTS[index], manifest.getSequences().get(0).getCanvases().size());
        }

        if (!aAsyncTask.isCompleted()) {
            aAsyncTask.complete();
        }
    }
}
