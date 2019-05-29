
package edu.ucla.library.iiif.manifeststore.verticles;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ucla.library.iiif.manifeststore.Config;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {

    private Vertx myVertx;

    /**
     * Test set up.
     *
     * @param aContext A testing context
     */
    @Before
    public void setUp(final TestContext aContext) throws IOException {
        final DeploymentOptions options = new DeploymentOptions();
        final ServerSocket socket = new ServerSocket(0);
        final int port = socket.getLocalPort();

        aContext.put(Config.HTTP_PORT, port);
        options.setConfig(new JsonObject().put(Config.HTTP_PORT, port));
        socket.close();

        // Initialize the Vert.x environment and start our main verticle
        myVertx = Vertx.vertx();
        myVertx.deployVerticle(MainVerticle.class.getName(), options, aContext.asyncAssertSuccess());
    }

    /**
     * Test tear down.
     *
     * @param aContext A testing context
     */
    @After
    public void tearDown(final TestContext aContext) {
        myVertx.close(aContext.asyncAssertSuccess());
    }

    /**
     * Our 'hello world' test.
     *
     * @param aContext A testing context
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testThatTheServerIsStarted(final TestContext aContext) {
        final Async async = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);

        // Testing the path defined in our OpenAPI YAML file
        myVertx.createHttpClient().getNow(port, "0.0.0.0", "/ping", response -> {
            final int statusCode = response.statusCode();

            if (statusCode == 200) {
                response.bodyHandler(body -> {
                    aContext.assertEquals("Hello", body.getString(0, body.length()));
                    async.complete();
                });
            } else {
                aContext.fail("Failed status code: " + statusCode);
                async.complete();
            }
        });
    }

}
