
package edu.ucla.library.iiif.fester;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.utils.IDUtils;
import io.vertx.core.json.JsonObject;

/**
 * Information about a IIIF image.
 */
public class ImageInfoLookup {

    public static final String FAKE_IIIF_SERVER = "https://test.example.com/iiif";

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageInfoLookup.class, Constants.MESSAGES);

    private static final int CANTALOUPE_TIMEOUT = 300000; // Five minutes

    private final int myWidth;

    private final int myHeight;

    /**
     * Create a new image info object from the information at the supplied URL.
     *
     * @param aURL A URL for an image's info.json file
     */
    public ImageInfoLookup(final String aURL) throws MalformedURLException, IOException, ManifestNotFoundException {
        LOGGER.debug(MessageCodes.MFS_072, aURL);

        // If our images are using an unspecified host, we're running in test mode and will use fake values
        if (aURL.contains(Constants.UNSPECIFIED_HOST) || aURL.startsWith(FAKE_IIIF_SERVER)) {
            myHeight = 1000;
            myWidth = 1000;
        } else {
            final HttpURLConnection connection = (HttpURLConnection) new URL(aURL).openConnection();
            final int responseCode;

            connection.setReadTimeout(CANTALOUPE_TIMEOUT);
            responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                final InputStream inStream = new BufferedInputStream(connection.getInputStream());
                final StringBuilder result = new StringBuilder();
                final JsonObject jsonObject;

                // Use try-with-resources so the reader is closed automatically
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inStream))) {
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    // Get the info.json contents
                    jsonObject = new JsonObject(result.toString());

                    // Find our image's width and height or use one if they're missing in the manifest
                    myHeight = jsonObject.getInteger("height", 1);
                    myWidth = jsonObject.getInteger("width", 1);

                    if (myHeight == 1 || myWidth == 1) {
                        LOGGER.warn(MessageCodes.MFS_073, aURL);
                    }
                }
            } else if (responseCode == 404) {
                final String id = IDUtils.decode(URI.create(aURL));
                throw new ManifestNotFoundException(MessageCodes.MFS_070, id);
            } else {
                final String responseMessage = connection.getResponseMessage();
                throw new IOException(LOGGER.getMessage(MessageCodes.MFS_071, responseCode, responseMessage));
            }
        }
    }

    /**
     * Gets the width of the image.
     *
     * @return The width of the image
     */
    public int getWidth() {
        return myWidth;
    }

    /**
     * Returns the height of the image.
     *
     * @return The height of the image
     */
    public int getHeight() {
        return myHeight;
    }
}
