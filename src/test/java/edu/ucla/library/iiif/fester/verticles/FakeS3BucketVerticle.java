
package edu.ucla.library.iiif.fester.verticles;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import info.freelibrary.util.BufferedFileWriter;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * A fake S3 bucket verticle that imitates the real S3BucketVerticle (without actually uploading anything).
 */
public class FakeS3BucketVerticle extends AbstractFesterVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(FakeS3BucketVerticle.class, Constants.MESSAGES);

    private static final Map<String, File> JSON_FILES = Map.of("ark%3A%2F21198%2Fzz0009gsq9", new File(
            "src/test/resources/json/ark%3A%2F21198%2Fzz0009gsq9.json"), "ark%3A%2F21198%2Fzz0009gv8j", new File(
                    "src/test/resources/json/ark%3A%2F21198%2Fzz0009gv8j.json"));

    private File myTmpDir;

    @Override
    public void start(final Promise<Void> aPromise) throws Exception {
        final String deploymentID = deploymentID();

        LOGGER.debug(MessageCodes.MFS_110, getClass().getName(), deploymentID());

        if (myTmpDir == null) {
            myTmpDir = Files.createTempDirectory(deploymentID).toFile();
        }

        vertx.eventBus().<JsonObject>consumer(S3BucketVerticle.class.getName()).handler(message -> {
            final JsonObject json = message.body();

            // Our JSON contains a single ID to get or a manifest to put
            if (json.size() == 1) {
                get(json.iterator().next().getValue().toString(), message);
            } else {
                put(json, message);
            }
        });

        aPromise.complete();
    }

    private void get(final String aID, final Message<JsonObject> aMessage) {
        try {
            // In the fake S3BucketVerticle, we don't care about the works or collections distinction
            final int start = aID.lastIndexOf('/');
            final String id = start == -1 ? aID : aID.substring(start + 1, aID.length());

            LOGGER.debug(MessageCodes.MFS_127, id);

            if (JSON_FILES.containsKey(id)) {
                final String result = StringUtils.read(JSON_FILES.get(id));
                final JsonObject manifest = new JsonObject(result);

                aMessage.reply(manifest);
            } else {
                aMessage.fail(0, id + " not found");
            }
        } catch (final IOException details) {
            aMessage.fail(0, details.getMessage());
        }
    }

    private void put(final JsonObject aManifest, final Message<JsonObject> aMessage) {
        final URI uri = URI.create(aManifest.getString(Constants.ID));
        final File tmpFile;
        final String id;

        // Sniff whether our ID is for a collection or a work manifest
        if (uri.toString().contains(Constants.COLLECTIONS_PATH)) {
            id = IDUtils.decode(uri, Constants.COLLECTIONS_PATH);
        } else {
            id = IDUtils.decode(uri);
        }

        // Encode our tmp file name because it may have non-safe characters in it
        tmpFile = new File(myTmpDir, URLEncoder.encode(id, StandardCharsets.UTF_8) + Constants.JSON_EXT);

        try (BufferedFileWriter writer = new BufferedFileWriter(tmpFile)) {
            if (LOGGER.isDebugEnabled()) {
                writer.write(aManifest.encodePrettily());
                LOGGER.debug(MessageCodes.MFS_124, aManifest.encode());
            }

            aMessage.reply(Op.SUCCESS);
        } catch (final IOException details) {
            aMessage.fail(100, details.getMessage());
        }
    }
}
