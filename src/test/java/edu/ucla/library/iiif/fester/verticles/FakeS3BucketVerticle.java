
package edu.ucla.library.iiif.fester.verticles;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import info.freelibrary.util.BufferedFileWriter;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

/**
 * A fake S3 bucket verticle that imitates the real S3BucketVerticle (without actually uploading anything).
 */
public class FakeS3BucketVerticle extends AbstractFesterVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(FakeS3BucketVerticle.class, Constants.MESSAGES);

    @Override
    public void start(final Promise<Void> aPromise) throws Exception {
        final String deploymentID = deploymentID();
        final File tmpDir = Files.createTempDirectory(deploymentID).toFile();

        LOGGER.debug(MessageCodes.MFS_110, getClass().getName(), deploymentID());

        vertx.eventBus().<JsonObject>consumer(S3BucketVerticle.class.getName()).handler(message -> {
            final JsonObject manifest = message.body();
            final URI uri = URI.create(manifest.getString(Constants.ID));
            final File tmpFile;
            final String id;

            // Sniff whether our ID is for a collection or a work manifest
            if (uri.toString().contains(Constants.COLLECTIONS_PATH)) {
                id = IDUtils.decode(uri, Constants.COLLECTIONS_PATH);
            } else {
                id = IDUtils.decode(uri);
            }

            // Encode our tmp file name because it may have non-safe characters in it
            tmpFile = new File(tmpDir, URLEncoder.encode(id, StandardCharsets.UTF_8) + Constants.JSON_EXT);

            try (BufferedFileWriter writer = new BufferedFileWriter(tmpFile)) {
                if (LOGGER.isDebugEnabled()) {
                    writer.write(manifest.encodePrettily());
                    LOGGER.debug(MessageCodes.MFS_124, manifest.encode());
                }

                message.reply(Op.SUCCESS);
            } catch (final IOException details) {
                message.fail(100, details.getMessage());
            }
        });

        aPromise.complete();
    }
}
