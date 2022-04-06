
package edu.ucla.library.iiif.fester.verticles;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import com.amazonaws.regions.RegionUtils;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import info.freelibrary.vertx.s3.LocalStackEndpoint;
import info.freelibrary.vertx.s3.S3Client;
import info.freelibrary.vertx.s3.S3ClientOptions;
import info.freelibrary.vertx.s3.S3Endpoint;
import info.freelibrary.vertx.s3.UnexpectedStatusException;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.CodeUtils;
import edu.ucla.library.iiif.fester.utils.IDUtils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.ConnectionPoolTooBusyException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Counter;

/**
 * Stores submitted manifests to an S3 bucket.
 */
public class S3BucketVerticle extends AbstractFesterVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3BucketVerticle.class, Constants.MESSAGES);

    private static final long MAX_RETRIES = 10;

    private S3Client myS3Client;

    private String myS3Bucket;

    private String myUrl;

    private final String myUrlPlaceholderPattern = Pattern.quote(Constants.URL_PLACEHOLDER);

    /**
     * Starts the S3 Bucket Verticle.
     */
    @Override
    public void start() throws Exception {
        super.start(); // We do some stuff in the abstract class that we want here

        final JsonObject config = config();

        // Initialize the S3BucketVerticle by getting our s3 configs and setting up the S3 client
        if (myS3Client == null) {
            final String s3AccessKey = config.getString(Config.S3_ACCESS_KEY);
            final String s3SecretKey = config.getString(Config.S3_SECRET_KEY);
            final String s3RegionName = config.getString(Config.S3_REGION, "us-east-1");
            final S3ClientOptions opts = new S3ClientOptions().setCredentials(s3AccessKey, s3SecretKey);

            if (RegionUtils.getRegion(s3RegionName) != null) {
                final String endpoint = config.getString(Config.S3_ENDPOINT);

                if (endpoint == null) {
                    opts.setEndpoint(S3Endpoint.fromRegion(s3RegionName));
                    LOGGER.debug(MessageCodes.MFS_034, opts.getEndpoint().getRegion(), "region");
                } else if (Constants.S3_ENDPOINT.equals(endpoint)) {
                    opts.setEndpoint(S3Endpoint.US_EAST_1);
                    LOGGER.debug(MessageCodes.MFS_034, S3Endpoint.US_EAST_1.getRegion(), "default");
                } else {
                    opts.setEndpoint(new LocalStackEndpoint(endpoint));
                    LOGGER.debug(MessageCodes.MFS_034, endpoint, "supplied");
                }
            }

            myS3Client = new S3Client(getVertx(), opts);
            myS3Bucket = config.getString(Config.S3_BUCKET);

            // Trace is only for developer use; don't turn on when running on a server
            LOGGER.trace(MessageCodes.MFS_046, s3AccessKey, s3SecretKey);
            LOGGER.debug(MessageCodes.MFS_047, s3RegionName);
            LOGGER.debug(MessageCodes.MFS_132, myS3Bucket);
        }

        getJsonConsumer().handler(message -> {
            final JsonObject messageBody = message.body();
            final String action = message.headers().get(Constants.ACTION);
            final JsonObject manifest;
            final String manifestID;

            switch (action) {
                case Op.GET_MANIFEST:
                    manifestID = messageBody.getString(Constants.MANIFEST_ID);
                    LOGGER.debug(MessageCodes.MFS_133, manifestID, myS3Bucket);
                    get(IDUtils.getWorkS3Key(manifestID), message);
                    break;
                case Op.PUT_MANIFEST:
                    manifestID = messageBody.getString(Constants.MANIFEST_ID);
                    manifest = messageBody.getJsonObject(Constants.DATA);
                    put(IDUtils.getWorkS3Key(manifestID), manifest, message);
                    break;
                case Op.GET_COLLECTION:
                    manifestID = messageBody.getString(Constants.COLLECTION_NAME);
                    LOGGER.debug(MessageCodes.MFS_133, manifestID, myS3Bucket);
                    get(IDUtils.getCollectionS3Key(manifestID), message);
                    break;
                case Op.PUT_COLLECTION:
                    manifestID = messageBody.getString(Constants.COLLECTION_NAME);
                    manifest = messageBody.getJsonObject(Constants.DATA);
                    put(IDUtils.getCollectionS3Key(manifestID), manifest, message);
                    break;
                default:
                    message.fail(CodeUtils.getInt(MessageCodes.MFS_139), StringUtils.format(MessageCodes.MFS_139,
                            getClass().toString(), message.toString(), action));
            }
        });

        myUrl = config.getString(Config.FESTER_URL);
    }

    /**
     * Gets a manifest from our S3 bucket.
     *
     * @param aS3Key The S3 key to use for the manifest
     * @param aMessage A event queue message
     */
    @SuppressWarnings("checkstyle:indentation")
    private void get(final String aS3Key, final Message<JsonObject> aMessage) {
        myS3Client.get(myS3Bucket, aS3Key, get -> {
            if (get.failed()) {
                final UnexpectedStatusException error = (UnexpectedStatusException) get.cause();
                final int statusCode = error.getStatusCode();
                final String statusMessage = error.getMessage();

                LOGGER.debug(MessageCodes.MFS_096, aS3Key, statusCode);

                if (statusCode == HTTP.NOT_FOUND) {
                    aMessage.fail(statusCode, statusMessage);
                } else {
                    get.result().body(body -> {
                        final Buffer buffer = body.result();

                        if (buffer != null) {
                            LOGGER.error(error, MessageCodes.MFS_097, aS3Key, buffer.toString(StandardCharsets.UTF_8));
                        }
                    });

                    aMessage.fail(HTTP.INTERNAL_SERVER_ERROR, statusMessage);
                }
            } else {
                LOGGER.debug(MessageCodes.MFS_096, aS3Key, HTTP.OK);

                get.result().body(body -> {
                    final String serializedJson = body.result().toString(StandardCharsets.UTF_8);
                    final String manifest;

                    if (aMessage.headers().get(Constants.NO_REWRITE_URLS) != null) {
                        manifest = serializedJson;
                    } else {
                        manifest = serializedJson.replaceAll(myUrlPlaceholderPattern, myUrl);
                    }

                    aMessage.reply(new JsonObject(manifest));
                });
            }
        });
    }

    /**
     * Puts a manifest into our S3 bucket.
     *
     * @param aS3Key The S3 key to use for the manifest
     * @param aManifest A work or collection manifest to store in the S3 bucket
     * @param aMessage A event queue message
     */
    @SuppressWarnings("Indentation") // Checkstyle's indentation check doesn't work with multiple lambdas
    private void put(final String aS3Key, final JsonObject aManifest, final Message<JsonObject> aMessage) {
        final Buffer manifestContent = aManifest.toBuffer();
        final String manifestID = IDUtils.getResourceID(aS3Key);
        final String context = aManifest.getString("@context");
        final String derivedManifestS3Key;
        final String idKey;

        if (context.equals(Constants.CONTEXT_V2)) {
            idKey = Constants.ID_V2;
        } else { // Constants.CONTEXT_V3
            idKey = Constants.ID_V3;
        }

        derivedManifestS3Key = IDUtils.getResourceS3Key(URI.create(aManifest.getString(idKey)));

        LOGGER.debug(MessageCodes.MFS_051, aManifest, myS3Bucket);
        LOGGER.debug(MessageCodes.MFS_128, manifestID);

        if (!aS3Key.equals(derivedManifestS3Key)) {
            LOGGER.warn(MessageCodes.MFS_138, aS3Key, derivedManifestS3Key);
        }

        try {
            myS3Client.put(myS3Bucket, aS3Key, manifestContent, put -> {
                if (put.failed()) {
                    final UnexpectedStatusException error = (UnexpectedStatusException) put.cause();
                    final int statusCode = error.getStatusCode();
                    final String message = error.getMessage();

                    // If there is some internal S3 server error, let's try again
                    if (statusCode == HTTP.INTERNAL_SERVER_ERROR) {
                        sendReply(aMessage, 0, Op.RETRY);
                    } else {
                        LOGGER.warn(MessageCodes.MFS_055, statusCode + " -> " + message);
                        retryUpload(manifestID, aMessage);
                    }
                } else {
                    LOGGER.info(MessageCodes.MFS_053, manifestID);
                    sendReply(aMessage, 0, Op.SUCCESS); // Decrements the request counter
                }
            });
        } catch (final ConnectionPoolTooBusyException details) {
            LOGGER.debug(MessageCodes.MFS_056, manifestID);
            sendReply(aMessage, 0, Op.RETRY);
        }
    }

    /**
     * A more tentative retry attempt. We count the number of times we've retried and give up after a certain point.
     *
     * @param aManifestID A Manifest ID to retry
     * @param aMessage A message to respond to with the retry request (or exception if we've failed)
     */
    private void retryUpload(final String aManifestID, final Message<JsonObject> aMessage) {
        shouldRetry(aManifestID, retryCheck -> {
            if (retryCheck.succeeded()) {
                if (retryCheck.result()) {
                    sendReply(aMessage, 0, Op.RETRY);
                } else {
                    sendReply(aMessage, CodeUtils.getInt(MessageCodes.MFS_058), Constants.EMPTY);
                }
            } else {
                final Throwable retryException = retryCheck.cause();
                final String details = retryException.getMessage();

                LOGGER.error(retryException, MessageCodes.MFS_059, details);

                // If we have an exception, don't retry... just log the issue
                sendReply(aMessage, CodeUtils.getInt(MessageCodes.MFS_059), details);
            }
        });
    }

    /**
     * A check to see whether an errored request should be retried.
     *
     * @param aManifestID A Manifest ID
     * @param aHandler A retry handler
     */
    private void shouldRetry(final String aManifestID, final Handler<AsyncResult<Boolean>> aHandler) {
        final Promise<Boolean> promise = Promise.promise();

        promise.future().onComplete(aHandler);

        vertx.sharedData().getLocalCounter(aManifestID, getCounter -> {
            if (getCounter.succeeded()) {
                final Counter counter = getCounter.result();

                counter.addAndGet(1L, get -> {
                    if (get.succeeded()) {
                        if (get.result() == MAX_RETRIES) {
                            promise.complete(Boolean.FALSE);

                            // Reset the counter in case we ever need to process this item again
                            counter.compareAndSet(MAX_RETRIES, 0L, reset -> {
                                if (reset.failed()) {
                                    LOGGER.error(MessageCodes.MFS_060, aManifestID);
                                }
                            });
                        } else {
                            promise.complete(Boolean.TRUE);
                        }
                    } else {
                        promise.fail(get.cause());
                    }
                });
            } else {
                promise.fail(getCounter.cause());
            }
        });
    }

    /**
     * Sends a message reply in the case that the S3 upload succeeded (or failed after the counter had been
     * incremented).
     *
     * @param aMessage A message requesting an S3 upload
     * @param aDetails The result of the S3 upload
     */
    private void sendReply(final Message<JsonObject> aMessage, final int aCode, final String aDetails) {
        getVertx().sharedData().getLocalCounter(Constants.S3_REQUEST_COUNT, getCounter -> {
            if (getCounter.succeeded()) {
                getCounter.result().decrementAndGet(decrement -> {
                    if (decrement.failed()) {
                        LOGGER.error(MessageCodes.MFS_062);
                    }

                    if (aCode == 0) {
                        aMessage.reply(aDetails);
                    } else {
                        aMessage.fail(aCode, aDetails);
                    }
                });
            } else {
                LOGGER.error(MessageCodes.MFS_063);

                if (aCode == 0) {
                    aMessage.reply(aDetails);
                } else {
                    aMessage.fail(aCode, aDetails);
                }
            }
        });
    }
}
