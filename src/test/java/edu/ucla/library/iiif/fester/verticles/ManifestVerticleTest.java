
package edu.ucla.library.iiif.fester.verticles;

import java.io.IOException;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * Tests run against the ManifestVerticle.
 */
@RunWith(VertxUnitRunner.class)
public class ManifestVerticleTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestVerticleTest.class, Constants.MESSAGES);

    private static final String MANIFEST_HOST = "https://iiif.library.ucla.edu";

    private static final String IMAGE_HOST = "https://iiif.library.ucla.edu/iiif/2";

    private static final String CSV_FILE_PATH = "src/test/resources/csv/{}.csv";

    private static final String HATHAWAY = "hathaway";

    private static final String POSTCARDS = "capostcards";

    private Vertx myVertx;

    private String myRunID;

    /**
     * Initialization of the testing environment before the tests are run.
     *
     * @param aContext A testing context
     * @throws IOException If there is trouble starting the Vert.x instance
     */
    @Before
    public void before(final TestContext aContext) throws IOException {
        final DeploymentOptions options = new DeploymentOptions();
        final JsonObject config = new JsonObject();
        final Async asyncTask = aContext.async();

        options.setConfig(config.put(Config.IIIF_BASE_URL, IMAGE_HOST));

        myVertx = Vertx.vertx();
        myVertx.deployVerticle(ManifestVerticle.class.getName(), options, deployment1 -> {
            if (deployment1.succeeded()) {
                myVertx.deployVerticle(FakeS3BucketVerticle.class.getName(), options, deployment2 -> {
                    if (deployment2.succeeded()) {
                        asyncTask.complete();
                    } else {
                        aContext.fail(deployment2.cause());
                    }
                });
            } else {
                aContext.fail(deployment1.cause());
            }
        });

        myRunID = UUID.randomUUID().toString();
    }

    /**
     * Cleanup the testing environment after the tests have been run.
     *
     * @param aContext A testing context
     */
    @After
    public void after(final TestContext aContext) {
        myVertx.close(aContext.asyncAssertSuccess());
    }

    /**
     * Test against the Hathaway collection.
     *
     * @param aContext A testing context
     */
    @Test
    public final void testHathawayManifest(final TestContext aContext) {
        final String filePath = StringUtils.format(CSV_FILE_PATH, HATHAWAY);
        final JsonObject message = new JsonObject();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID).put(Constants.CSV_FILE_PATH, filePath);
        message.put(Constants.FESTER_HOST, MANIFEST_HOST);

        LOGGER.debug(MessageCodes.MFS_120, HATHAWAY, ManifestVerticle.class.getName());

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, request -> {
            if (request.succeeded()) {
                asyncTask.complete();
            } else {
                aContext.fail(request.cause());
            }
        });
    }

    /**
     * Test against the CA Postcards collection.
     *
     * @param aContext A testing context
     */
    @Test
    public final void testPostcardsManifest(final TestContext aContext) {
        final String filePath = StringUtils.format(CSV_FILE_PATH, POSTCARDS);
        final JsonObject message = new JsonObject();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID).put(Constants.CSV_FILE_PATH, filePath);
        message.put(Constants.FESTER_HOST, MANIFEST_HOST);

        LOGGER.debug(MessageCodes.MFS_120, POSTCARDS, ManifestVerticle.class.getName());

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, request -> {
            if (request.succeeded()) {
                asyncTask.complete();
            } else {
                aContext.fail(request.cause());
            }
        });
    }

    /**
     * Test against the Hathaway (sample Sinai) collection.
     *
     * @param aContext A testing context
     */
    @Test
    public final void testHathawaySinaiManifests(final TestContext aContext) {
        final String hathawayWorks = HATHAWAY + "/works";
        final String filePath = StringUtils.format(CSV_FILE_PATH, hathawayWorks);
        final JsonObject message = new JsonObject();
        final Async asyncTask = aContext.async();

        message.put(Constants.CSV_FILE_NAME, myRunID).put(Constants.CSV_FILE_PATH, filePath);
        message.put(Constants.FESTER_HOST, MANIFEST_HOST);

        LOGGER.debug(MessageCodes.MFS_120, hathawayWorks, ManifestVerticle.class.getName());

        myVertx.eventBus().request(ManifestVerticle.class.getName(), message, request -> {
            if (request.succeeded()) {
                asyncTask.complete();
            } else {
                aContext.fail(request.cause());
            }
        });
    }

}
