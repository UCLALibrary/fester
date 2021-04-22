
package edu.ucla.library.iiif.fester.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.MessageCodes;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.Async;

/**
 * A test utility that confirms our server has been started before completing the set up process.
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

    /**
     * Checks that the server is up and responsive.
     */
    @Override
    public void run() {
        int counter = 0;

        while (!myAsyncTask.isCompleted()) { // Without completing, the test will eventually time out
            if (++counter % 10 == 0) {
                // Every 10 checks, inform that we're still looking for the port
                LOGGER.debug(MessageCodes.MFS_178, myPort);
            }

            try {
                final URL url = new URL(StringUtils.format(URL, myPort));
                final HttpURLConnection http = (HttpURLConnection) url.openConnection();

                http.setRequestMethod(HttpMethod.GET.name());
                http.connect();

                LOGGER.debug(MessageCodes.MFS_177, http.getResponseCode());

                http.disconnect();
                myAsyncTask.complete();
            } catch (final IOException details) {
                // I don't think we care how many times we tried(?)
            }
        }
    }

}
