
package edu.ucla.library.iiif.fester.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.MessageCodes;

import io.vertx.ext.unit.Async;

/**
 * A checker to confirm that our server has been started.
 */
public class ServerChecker implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerChecker.class, MessageCodes.BUNDLE);

    private static final String URL = "http://0.0.0.0:{}/fester/status";

    private final Async myAsyncTask;

    private final int myPort;

    /**
     * Creates a new server checker.
     *
     * @param aPort A port on which to check for the server
     * @param aAsyncTask A testing task to complete when the server is up
     */
    public ServerChecker(final int aPort, final Async aAsyncTask) {
        myAsyncTask = aAsyncTask;
        myPort = aPort;
    }

    @Override
    public void run() {
        while (!myAsyncTask.isCompleted()) { // Without completing the test will eventually time out
            try {
                final URL url = new URL(StringUtils.format(URL, myPort));
                final HttpURLConnection http = (HttpURLConnection) url.openConnection();
                final int responseCode;

                http.setRequestMethod("GET");
                http.connect();
                responseCode = http.getResponseCode();

                LOGGER.debug("Found the server: {}", responseCode);
                myAsyncTask.complete();
                http.disconnect();
            } catch (final IOException details) {
                LOGGER.error(details.getMessage());
            }
        }
    }

}
