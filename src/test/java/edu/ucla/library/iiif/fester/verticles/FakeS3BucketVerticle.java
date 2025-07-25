
package edu.ucla.library.iiif.fester.verticles;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import info.freelibrary.util.BufferedFileWriter;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
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

    private static final Map<String, File> JSON_FILES = new HashMap<>();

    private File myTmpDir;

    private final String myUrl = System.getProperty(Config.FESTER_URL);

    private final String myUrlPlaceholderPattern = Pattern.quote(Constants.URL_PLACEHOLDER);

    private String myIiifApiVersion;

    @Override
    public void start() throws Exception {
        myIiifApiVersion = config().getString(Constants.IIIF_API_VERSION);

        super.start();

        LOGGER.debug(MessageCodes.MFS_110, getClass().getName(), deploymentID());

        if (myTmpDir == null) {
            // Creates a tmp dir from the deploymentID, adding additional random numbers after the underscore
            myTmpDir = Files.createTempDirectory(deploymentID() + "_").toFile();
        }

        JSON_FILES.put(IDUtils.getCollectionS3Key("ark:/21198/zz0009gsq9"), new File(
                StringUtils.format("src/test/resources/json/{}/ark%3A%2F21198%2Fzz0009gsq9.json", myIiifApiVersion)));
        JSON_FILES.put(IDUtils.getWorkS3Key("ark:/21198/zz0009gv8j"), new File(
                StringUtils.format("src/test/resources/json/{}/ark%3A%2F21198%2Fzz0009gv8j.json", myIiifApiVersion)));
        JSON_FILES.put(IDUtils.getWorkS3Key("ark:/21198/z12f8rtw"), new File(
                StringUtils.format("src/test/resources/json/{}/ark%3A%2F21198%2Fz12f8rtw.json", myIiifApiVersion)));

        vertx.eventBus().<JsonObject>consumer(S3BucketVerticle.class.getName()).handler(message -> {
            final JsonObject body = message.body();
            final String manifestID;
            final JsonObject manifest;
            final String action = message.headers().get(Constants.ACTION);

            switch (action) {
                case Op.GET_MANIFEST:
                    manifestID = body.getString(Constants.MANIFEST_ID);
                    LOGGER.debug(MessageCodes.MFS_127, manifestID);
                    get(IDUtils.getWorkS3Key(manifestID), message);
                    break;
                case Op.PUT_MANIFEST:
                    manifestID = body.getString(Constants.MANIFEST_ID);
                    manifest = body.getJsonObject(Constants.DATA);
                    put(IDUtils.getWorkS3Key(manifestID), manifest, message);
                    break;
                case Op.GET_COLLECTION:
                    manifestID = body.getString(Constants.COLLECTION_NAME);
                    LOGGER.debug(MessageCodes.MFS_127, manifestID);
                    get(IDUtils.getCollectionS3Key(manifestID), message);
                    break;
                case Op.PUT_COLLECTION:
                    manifestID = body.getString(Constants.COLLECTION_NAME);
                    manifest = body.getJsonObject(Constants.DATA);
                    put(IDUtils.getCollectionS3Key(manifestID), manifest, message);
                    break;
                default:
                    final String errorMessage = StringUtils.format(MessageCodes.MFS_139, this.getClass().toString(),
                            message.toString(), action);
                    message.fail(CodeUtils.getInt(MessageCodes.MFS_139), errorMessage);
                    break;
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
                final String serializedJson = StringUtils.read(JSON_FILES.get(aS3Key));
                final String manifest;
                if (aMessage.headers().get(Constants.NO_REWRITE_URLS) != null) {
                    manifest = serializedJson;
                } else {
                    manifest = serializedJson.replaceAll(myUrlPlaceholderPattern, myUrl);
                }
                aMessage.reply(new JsonObject(manifest));
            } else {
                aMessage.fail(HTTP.NOT_FOUND, aS3Key + " not found");
            }
        } catch (final IOException details) {
            aMessage.fail(HTTP.INTERNAL_SERVER_ERROR, details.getMessage());
        }
    }

    /**
     * Saves the JSON file locally with its name as a URL-encoded S3 key.
     *
     * @param aS3Key
     * @param aManifest
     * @param aMessage
     */
    private void put(final String aS3Key, final JsonObject aManifest, final Message<JsonObject> aMessage) {
        // URL-encoding makes it so we don't have to worry about creating subdirectories
        final String path = URLEncoder.encode(aS3Key, StandardCharsets.UTF_8);
        final File tmpFile = new File(myTmpDir, path);
        final String derivedManifestS3Key;
        final String idKey;
        final String id;

        switch (myIiifApiVersion) {
            case Constants.IIIF_API_V2:
                idKey = Constants.ID_V2;
                break;
            default: // Constants.IIIF_API_V3
                idKey = Constants.ID_V3;
                break;
        }

        if ((id = aManifest.getString(idKey)) == null) {
            aMessage.fail(400, "ID must be present");
        } else {
            derivedManifestS3Key = IDUtils.getResourceS3Key(URI.create(id));

            if (!aS3Key.equals(derivedManifestS3Key)) {
                LOGGER.warn(MessageCodes.MFS_138, aS3Key, derivedManifestS3Key);
            }

            try (BufferedFileWriter writer = new BufferedFileWriter(tmpFile)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(MessageCodes.MFS_124, aManifest.encode());
                }
                writer.write(aManifest.encodePrettily());
                JSON_FILES.put(aS3Key, tmpFile);

                aMessage.reply(Op.SUCCESS);
            } catch (final IOException details) {
                aMessage.fail(100, details.getMessage());
            }
        }
    }
}
