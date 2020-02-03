
package edu.ucla.library.iiif.fester.verticles;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Map.Entry;

import info.freelibrary.util.BufferedFileWriter;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.ObjectType;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.CodeUtils;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * A fake S3 bucket verticle that imitates the real S3BucketVerticle (without actually uploading anything).
 */
public class FakeS3BucketVerticle extends AbstractFesterVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(FakeS3BucketVerticle.class, Constants.MESSAGES);

    private static final Map<String, File> JSON_FILES = Map.of(IDUtils.getCollectionS3Key("ark:/21198/zz0009gsq9"),
            new File("src/test/resources/json/ark%3A%2F21198%2Fzz0009gsq9.json"),
            IDUtils.getWorkS3Key("ark:/21198/zz0009gv8j"),
            new File("src/test/resources/json/ark%3A%2F21198%2Fzz0009gv8j.json"),
            IDUtils.getWorkS3Key("ark:/21198/z12f8rtw"),
            new File("src/test/resources/json/ark%3A%2F21198%2Fz12f8rtw.json"));

    private File myTmpDir;

    @Override
    public void start() throws Exception {
        super.start();

        LOGGER.debug(MessageCodes.MFS_110, getClass().getName(), deploymentID());

        if (myTmpDir == null) {
            // Creates a tmp dir from the deploymentID, adding additional random numbers after the underscore
            myTmpDir = Files.createTempDirectory(deploymentID() + "_").toFile();
        }

        vertx.eventBus().<JsonObject>consumer(S3BucketVerticle.class.getName()).handler(message -> {
            final JsonObject json = message.body();

            // Our JSON contains a single ID to get or a manifest to put
            if (json.size() == 1) {
                final Entry<String, Object> entry = json.iterator().next();
                final String manifestID = entry.getValue().toString();
                final String manifestType = entry.getKey();

                if (ObjectType.WORK.equals(manifestType)) {
                    LOGGER.debug(MessageCodes.MFS_127, manifestID);
                    get(IDUtils.getWorkS3Key(manifestID), message);
                } else if (ObjectType.COLLECTION.equals(manifestType)) {
                    LOGGER.debug(MessageCodes.MFS_127, manifestID);
                    get(IDUtils.getCollectionS3Key(manifestID), message);
                } else {
                    final String errorMessage = StringUtils.format(MessageCodes.MFS_136, entry.getValue(),
                            entry.getKey());
                    message.fail(CodeUtils.getInt(MessageCodes.MFS_136), errorMessage);
                }
            } else {
                put(json, message);
            }
        });
    }

    /**
     * Gets the object stored in the JSON_FILES map under aS3Key.
     *
     * @param aS3Key An S3 key
     * @param aMessage
     */
    private void get(final String aS3Key, final Message<JsonObject> aMessage) {
        try {
            LOGGER.debug(MessageCodes.MFS_127, aS3Key);

            if (JSON_FILES.containsKey(aS3Key)) {
                final String result = StringUtils.read(JSON_FILES.get(aS3Key));
                final JsonObject manifest = new JsonObject(result);

                aMessage.reply(manifest);
            } else {
                aMessage.fail(CodeUtils.getInt(MessageCodes.MFS_052), aS3Key + " not found");
            }
        } catch (final IOException details) {
            aMessage.fail(CodeUtils.getInt(MessageCodes.MFS_052), details.getMessage());
        }
    }

    /**
     * Saves the JSON file locally with its name as a URL-encoded S3 key.
     *
     * @param aManifest
     * @param aMessage
     */
    private void put(final JsonObject aManifest, final Message<JsonObject> aMessage) {
        final URI uri = URI.create(aManifest.getString(Constants.ID));

        // URL-encoding makes it so we don't have to worry about creating subdirectories
        final String path = URLEncoder.encode(IDUtils.getResourceS3Key(uri), StandardCharsets.UTF_8);
        final File tmpFile = new File(myTmpDir, path);

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
