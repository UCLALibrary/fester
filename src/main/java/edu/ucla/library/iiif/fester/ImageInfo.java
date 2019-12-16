
package edu.ucla.library.iiif.fester;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import io.vertx.core.json.JsonObject;

/**
 * Information about a IIIF image.
 */
public class ImageInfo {

    private final int myWidth;

    private final int myHeight;

    /**
     * Create a new image info object from the information at the supplied URL.
     *
     * @param aURL A URL for an image's info.json file
     */
    public ImageInfo(final String aURL) throws MalformedURLException, IOException {
        // If our images are using an unspecified host, we're running in test mode
        if (aURL.contains(Constants.UNSPECIFIED_HOST)) {
            myHeight = 1000;
            myWidth = 1000;
        } else {
            final HttpURLConnection urlConnection = (HttpURLConnection) new URL(aURL).openConnection();
            final InputStream inStream = new BufferedInputStream(urlConnection.getInputStream());
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

                // Find our image's width and height
                myHeight = jsonObject.getInteger("height", 0);
                myWidth = jsonObject.getInteger("width", 0);
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
