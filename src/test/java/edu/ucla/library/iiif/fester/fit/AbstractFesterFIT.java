
package edu.ucla.library.iiif.fester.fit;

import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;

import com.amazonaws.services.s3.AmazonS3;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * A base class for functional or integration tests.
 */
abstract class AbstractFesterFIT {

    /* The bucket that's being used for testing */
    protected static final String BUCKET = System.getProperty(Config.S3_BUCKET);

    /* The Vertx instance that our tests use */
    protected static final Vertx VERTX_INSTANCE = Vertx.vertx();

    /* The base URL of the Fester instance */
    protected static final String FESTER_URL = System.getProperty(Config.FESTER_URL);

    /* Regexp of the placeholder base URL of the Fester instance */
    protected static final String FESTER_URL_PLACEHOLDER = Pattern.quote(Constants.URL_PLACEHOLDER);

    /* This is the port that tests can be run against */
    protected static int FESTER_PORT;

    /* An S3 client used to confirm tests have worked */
    protected AmazonS3 myS3Client;

    /* A WebClient that can be used in tests */
    protected WebClient myWebClient;

    /* A test ID */
    protected String myID;

    /**
     * Sets up the test.
     */
    @Before
    public void setUpTest() {
        myID = UUID.randomUUID().toString();
        myWebClient = WebClient.create(VERTX_INSTANCE, new WebClientOptions().setKeepAlive(false));
    }

    /**
     * Cleans up the test.
     */
    @After
    public void cleanUpTest() {
        myWebClient.close();
    }

    /**
     * Completes an asynchronous task.
     *
     * @param aAsyncTask An asynchronous task
     */
    protected void complete(final Async aAsyncTask) {
        if (!aAsyncTask.isCompleted()) {
            aAsyncTask.complete();
        }
    }

}
