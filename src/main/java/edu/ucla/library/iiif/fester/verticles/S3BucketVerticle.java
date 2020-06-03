
package edu.ucla.library.iiif.fester.verticles;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import com.amazonaws.regions.RegionUtils;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;
import info.freelibrary.vertx.s3.S3Client;

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
import io.vertx.core.http.HttpClientOptions;
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
            final String s3RegionName = config.getString(Config.S3_REGION);
            final String endpoint = config.getString(Config.S3_ENDPOINT);
            final HttpClientOptions httpOptions = new HttpClientOptions();

            // Check to see that we're not overriding the default S3 endpoint
            if (endpoint == null || Constants.S3_ENDPOINT.equals(endpoint)) {
                httpOptions.setDefaultHost(RegionUtils.getRegion(s3RegionName).getServiceEndpoint("s3"));

                LOGGER.debug(MessageCodes.MFS_034, httpOptions.getDefaultHost());
            } else {
                final URI s3URI = URI.create(endpoint);

                // TODO: make sure host has no http(s) but that the right setSSL is set
                httpOptions.setDefaultHost(s3URI.getHost());
                httpOptions.setDefaultPort(s3URI.getPort());

                LOGGER.debug(MessageCodes.MFS_034, httpOptions.getDefaultHost() + ':' + httpOptions.getDefaultPort());
            }

            myS3Client = new S3Client(getVertx(), s3AccessKey, s3SecretKey, httpOptions);
            myS3Bucket = config.getString(Config.S3_BUCKET);

            // Trace is only for developer use; don't turn on when running on a server
            LOGGER.trace(MessageCodes.MFS_046, s3AccessKey, s3SecretKey);
            LOGGER.debug(MessageCodes.MFS_047, s3RegionName);
        }

        getJsonConsumer().handler(message -> {
            final JsonObject msg = message.body();
            final String manifestID;
            final JsonObject manifest;
            final String action = message.headers().get(Constants.ACTION);

            switch (action) {
                case Op.GET_MANIFEST:
                    manifestID = msg.getString(Constants.MANIFEST_ID);
                    LOGGER.debug(MessageCodes.MFS_133, manifestID, myS3Bucket);
                    get(IDUtils.getWorkS3Key(manifestID), message);
                    break;
                case Op.PUT_MANIFEST:
                    manifestID = msg.getString(Constants.MANIFEST_ID);
                    manifest = msg.getJsonObject(Constants.DATA);
                    put(IDUtils.getWorkS3Key(manifestID), manifest, message);
                    break;
                case Op.GET_COLLECTION:
                    manifestID = msg.getString(Constants.COLLECTION_NAME);
                    LOGGER.debug(MessageCodes.MFS_133, manifestID, myS3Bucket);
                    get(IDUtils.getCollectionS3Key(manifestID), message);
                    break;
                case Op.PUT_COLLECTION:
                    manifestID = msg.getString(Constants.COLLECTION_NAME);
                    manifest = msg.getJsonObject(Constants.DATA);
                    put(IDUtils.getCollectionS3Key(manifestID), manifest, message);
                    break;
                default:
                    final String errorMessage = StringUtils.format(MessageCodes.MFS_139, getClass().toString(),
                            message.toString(), action);
                    message.fail(CodeUtils.getInt(MessageCodes.MFS_139), errorMessage);
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
            final int statusCode = get.statusCode();
            final String statusMessage = get.statusMessage();

            // This lets us know at what time we received a response, in addition to the code
            LOGGER.debug(MessageCodes.MFS_096, aS3Key, statusCode);

            if (statusCode == HTTP.OK) {
                get.bodyHandler(body -> {
                    final String serializedJson = body.toString(StandardCharsets.UTF_8);
                    final String manifest;

                    if (aMessage.headers().get(Constants.NO_REWRITE_URLS) != null) {
                        manifest = serializedJson;
                    } else {
                        manifest = serializedJson.replaceAll(myUrlPlaceholderPattern, myUrl);
                    }

                    aMessage.reply(new JsonObject(manifest));
                });
            } else if (statusCode == HTTP.NOT_FOUND) {
                aMessage.fail(HTTP.NOT_FOUND, statusMessage);
            } else {
                aMessage.fail(HTTP.INTERNAL_SERVER_ERROR, statusMessage);
            }
        }, exception -> {
            LOGGER.error(exception, MessageCodes.MFS_097, aS3Key, exception.getMessage());
            aMessage.fail(HTTP.INTERNAL_SERVER_ERROR, exception.getMessage());
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
        final String derivedManifestS3Key = IDUtils.getResourceS3Key(URI.create(aManifest.getString(Constants.ID)));

        LOGGER.debug(MessageCodes.MFS_051, aManifest, myS3Bucket);
        LOGGER.debug(MessageCodes.MFS_128, manifestID);

        if (!aS3Key.equals(derivedManifestS3Key)) {
            LOGGER.warn(MessageCodes.MFS_138, aS3Key, derivedManifestS3Key);
        }

        try {
            myS3Client.put(myS3Bucket, aS3Key, manifestContent, response -> {
                final int statusCode = response.statusCode();

                response.exceptionHandler(exception -> {
                    LOGGER.error(exception, exception.getMessage());
                    sendReply(aMessage, CodeUtils.getInt(MessageCodes.MFS_052), exception.getMessage());
                });

                // If we get a successful upload response code, send a reply to indicate so
                if (statusCode == HTTP.OK) {
                    LOGGER.info(MessageCodes.MFS_053, manifestID);

                    // Send the success result and decrement the S3 request counter
                    sendReply(aMessage, 0, Op.SUCCESS);
                } else {
                    LOGGER.error(MessageCodes.MFS_054, statusCode, response.statusMessage());

                    // Log the detailed reason we failed so we can track down the issue
                    response.bodyHandler(body -> {
                        LOGGER.error(MessageCodes.MFS_052, body.getString(0, body.length()));
                    });

                    // If there is some internal S3 server error, let's try again
                    if (statusCode == HTTP.INTERNAL_SERVER_ERROR) {
                        sendReply(aMessage, 0, Op.RETRY);
                    } else {
                        LOGGER.warn(MessageCodes.MFS_055, statusCode + " - " + response.statusMessage());
                        retryUpload(manifestID, aMessage);
                    }
                }
            }, exception -> {
                LOGGER.warn(MessageCodes.MFS_055, exception.getMessage());
                retryUpload(manifestID, aMessage);
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
