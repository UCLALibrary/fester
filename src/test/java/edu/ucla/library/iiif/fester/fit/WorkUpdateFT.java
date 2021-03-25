
package edu.ucla.library.iiif.fester.fit;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amazonaws.services.s3.model.S3ObjectSummary;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.MetadataLabels;
import edu.ucla.library.iiif.fester.utils.ManifestTestUtils;
import edu.ucla.library.iiif.fester.utils.V2ManifestTestUtils;
import edu.ucla.library.iiif.fester.utils.V3ManifestTestUtils;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.multipart.MultipartForm;

/**
 * Tests to confirm that a work manifest's metadata can be updated by CSV upload.
 */
@RunWith(VertxUnitRunner.class)
public class WorkUpdateFT extends BaseFesterFT {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkUpdateFT.class, Constants.MESSAGES);

    private static final String MANIFEST_S3_KEY = "works/ark:/21198/zz000bjg0d.json";

    private static final File SRC_FILE = new File("src/test/resources/csv/hathaway.csv");

    private static final File TEST_FILE = new File("src/test/resources/csv/hathaway-updated.csv");

    private static final String METADATA_UPDATE = "metadata-update";

    private static final String IIIF_API_VERSION = "iiif-version";

    private static final String NEW_REPO_NAME = "UCLA";

    private static final String REPO_NAME =
            "University of California, Los Angeles. Library. Performing Arts Special Collections";

    private static final String NEW_TITLE = "Hathaway Manuscript 17";

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
     * Tests that a v2 work manifest can be updated by CSV upload.
     *
     * @param aContext A test context
     */
    @Test
    public void testV2WorkUpdate(final TestContext aContext) {
        final V2ManifestTestUtils v2ManifestTestUtils = new V2ManifestTestUtils();
        final Async asyncTask = aContext.async();

        setupManifest(v2ManifestTestUtils.getApiVersion()).compose(outcome -> checkManifest(v2ManifestTestUtils))
                .compose(outcome -> {
                    LOGGER.debug(MessageCodes.MFS_165, v2ManifestTestUtils.getApiVersion());
                    return Future.future(handle -> complete(asyncTask));
                });
    }

    /**
     * Tests that a v3 work manifest can be updated by CSV upload.
     *
     * @param aContext A test context
     */
    @Test
    public void testV3WorkUpdate(final TestContext aContext) {
        final V3ManifestTestUtils v3ManifestTestUtils = new V3ManifestTestUtils();
        final Async asyncTask = aContext.async();

        setupManifest(v3ManifestTestUtils.getApiVersion()).compose(outcome -> checkManifest(v3ManifestTestUtils))
                .compose(outcome -> {
                    LOGGER.debug(MessageCodes.MFS_165, v3ManifestTestUtils.getApiVersion());
                    return Future.future(handle -> complete(asyncTask));
                });

    }

    /**
     * Sets up a manifest update check.
     *
     * @param aApiVersion A version of the IIIF manifest specification
     * @return A future result of the setup
     */
    private Future<Void> setupManifest(final String aApiVersion) {
        final Promise<Void> promise = Promise.promise();

        // Update a source CSV to the S3 bucket to that we can update it
        uploadCSV(MultipartForm.create().attribute(IIIF_API_VERSION, aApiVersion).textFileUpload(Constants.CSV_FILE,
                SRC_FILE.getName(), SRC_FILE.getAbsolutePath(), Constants.CSV_MEDIA_TYPE), SRC_FILE, promise);

        return promise.future();
    }

    /**
     * Checks a manifest to make sure it's been updated.
     *
     * @param aContext A test context
     * @param aManifestUtils A utilities class for a particular version of IIIF manifests
     * @return A future result of the check
     */
    private Future<Void> checkManifest(final ManifestTestUtils aManifestUtils) {
        final Promise<Void> uploadPromise = Promise.promise();
        final Promise<Void> checkPromise = Promise.promise();
        final String manifestAsString = myS3Client.getObjectAsString(BUCKET, MANIFEST_S3_KEY);
        final Optional<String> repoName = aManifestUtils.getMetadata(manifestAsString, MetadataLabels.REPOSITORY_NAME);

        if (repoName.isPresent()) {
            final MultipartForm form = MultipartForm.create();

            assertEquals(REPO_NAME, repoName.get());

            form.attribute(METADATA_UPDATE, Boolean.TRUE.toString());
            form.attribute(IIIF_API_VERSION, aManifestUtils.getApiVersion());

            uploadPromise.future().onComplete(updateHandler -> {
                if (updateHandler.succeeded()) {
                    final String updatedManifestAsString = myS3Client.getObjectAsString(BUCKET, MANIFEST_S3_KEY);
                    final Optional<String> updatedRepoName =
                            aManifestUtils.getMetadata(updatedManifestAsString, MetadataLabels.REPOSITORY_NAME);
                    final Optional<String> updatedTitle = aManifestUtils.getLabel(updatedManifestAsString);
                    String errorMsg;

                    if (updatedRepoName.isPresent()) {
                        LOGGER.debug(MessageCodes.MFS_162, aManifestUtils.getApiVersion(), "repository name");
                        assertEquals(NEW_REPO_NAME, updatedRepoName.get());
                    } else {
                        errorMsg = LOGGER.getMessage(MessageCodes.MFS_161, "metadata entry",
                                MetadataLabels.REPOSITORY_NAME);
                        LOGGER.error(errorMsg);
                        checkPromise.fail(errorMsg);
                    }
                    if (updatedTitle.isPresent()) {
                        LOGGER.debug(MessageCodes.MFS_162, aManifestUtils.getApiVersion(), "title");
                        assertEquals(NEW_TITLE, updatedTitle.get());
                    } else {
                        errorMsg = LOGGER.getMessage(MessageCodes.MFS_161, "label", MANIFEST_S3_KEY);
                        LOGGER.error(errorMsg);
                        checkPromise.fail(errorMsg);
                    }
                    checkPromise.complete();
                } else {
                    LOGGER.error(updateHandler.cause().getMessage(), updateHandler.cause());
                    checkPromise.fail(updateHandler.cause());
                }
            });

            uploadCSV(form.textFileUpload(Constants.CSV_FILE, TEST_FILE.getName(), TEST_FILE.getAbsolutePath(),
                    Constants.CSV_MEDIA_TYPE), TEST_FILE, uploadPromise);
        } else {
            LOGGER.error(MessageCodes.MFS_161, MetadataLabels.REPOSITORY_NAME);
            checkPromise.fail(LOGGER.getMessage(MessageCodes.MFS_161, MetadataLabels.REPOSITORY_NAME));
        }

        return checkPromise.future();
    }

    /**
     * A method to upload a CSV file.
     *
     * @param aForm A form with the CSV file's information
     * @param aPromise A promise that the upload happens
     */
    private void uploadCSV(final MultipartForm aForm, final File aCsvFile, final Promise<Void> aPromise) {
        LOGGER.debug(MessageCodes.MFS_163, aCsvFile);

        myWebClient.post(FESTER_PORT, Constants.UNSPECIFIED_HOST, Constants.POST_CSV_ROUTE).sendMultipartForm(aForm,
                post -> {
                    if (post.succeeded()) {
                        final HttpResponse<Buffer> response = post.result();
                        final int statusCode = response.statusCode();

                        if (statusCode == HTTP.CREATED) {
                            LOGGER.debug(MessageCodes.MFS_164, aCsvFile);
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
}
