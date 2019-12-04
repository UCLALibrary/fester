
package edu.ucla.library.iiif.fester.verticles;

import static edu.ucla.library.iiif.fester.Constants.EMPTY;

import com.amazonaws.regions.RegionUtils;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.vertx.s3.S3Client;
import info.freelibrary.vertx.s3.UserMetadata;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.CodeUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
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

    private static final int DEFAULT_S3_MAX_REQUESTS = 10;

    private static final long MAX_RETRIES = 10;

    private S3Client myS3Client;

    /**
     * Starts the S3 Bucket Verticle.
     */
    @Override
    public void start(final Future<Void> aFuture) {
        getJsonConsumer().handler(message -> {
            final JsonObject messageBody = message.body();

            // retrieve our parameters from the message we have been sent on the bus
            // param: manifest-content:jsonObject - holds the content of the Manifest object
            // param: manifestID:String - the identifier we will use when storing the Manifest in S3
            final String manifestID = messageBody.getString(Constants.MANIFEST_ID);
            final JsonObject jsonObject = messageBody.getJsonObject(Constants.MANIFEST_CONTENT);

            // grab the Fester configuration
            final JsonObject config = config();
            final int s3MaxRequests = config.getInteger(Config.S3_MAX_REQUESTS, DEFAULT_S3_MAX_REQUESTS);

            // Initialize the S3BucketVerticle by getting our s3 configs and setting up the S3 client
            if (myS3Client == null) {
                final String s3AccessKey = config.getString(Config.S3_ACCESS_KEY);
                final String s3SecretKey = config.getString(Config.S3_SECRET_KEY);
                final String s3Bucket = config.getString(Config.S3_BUCKET);
                final String s3RegionName = config.getString(Config.S3_REGION);
                final String s3Region = RegionUtils.getRegion(s3RegionName).getServiceEndpoint("s3");

                final HttpClientOptions options = new HttpClientOptions();

                // Set the S3 client options
                options.setDefaultHost(s3Region);

                myS3Client = new S3Client(getVertx(), s3AccessKey, s3SecretKey, options);

                // Trace is only for developer use; don't turn on when running on a server
                LOGGER.trace(MessageCodes.MFS_046, s3AccessKey, s3SecretKey); // AWS S3 access / secret keys: {} / {}

                LOGGER.debug(MessageCodes.MFS_047, s3RegionName); // S3 Client configured for region: {}
            }

            // handle S3 upload requests

            getVertx().sharedData().getLocalCounter(Constants.S3_REQUEST_COUNT, getCounter -> {
                if (getCounter.succeeded()) {
                    getCounter.result().incrementAndGet(increment -> {
                        if (increment.succeeded()) {
                            // Check that we haven't reached our maximum number of S3 uploads
                            if (increment.result() <= s3MaxRequests) {
                                upload(message, config);
                            } else {
                                // If we have reached our maximum request count, re-queue the request
                                sendReply(message, 0, Op.RETRY);
                            }
                        } else {
                            message.fail(CodeUtils.getInt(MessageCodes.MFS_048), EMPTY);
                        }
                    });
                } else {
                    message.fail(CodeUtils.getInt(MessageCodes.MFS_049), EMPTY);
                }
            });

        });

        aFuture.complete();
    }

    /**
     * Upload the provided MANIFEST_CONTENT to a file in S3.
     *
     * @param aMessage The message containing the S3 upload request
     * @param aConfig The verticle's configuration
     */
    @SuppressWarnings("Indentation") // Checkstyle's indentation check doesn't work with multiple lambdas
    private void upload(final Message<JsonObject> aMessage, final JsonObject aConfig) {
        final JsonObject storageRequest = aMessage.body();

        // If an S3 bucket isn't being supplied to us, use the one in our application configuration
        if (!storageRequest.containsKey(Config.S3_BUCKET)) {
            storageRequest.mergeIn(aConfig);
        }

        final String s3Bucket = storageRequest.getString(Config.S3_BUCKET);
        final String manifestID = storageRequest.getString(Constants.MANIFEST_ID);
        final String manifestIDwithExt = manifestID + Constants.JSON_EXT;

        LOGGER.debug(MessageCodes.MFS_050, manifestID, s3Bucket);

        // If we have MANIFEST_CONTENT, try to upload it to S3
        if (storageRequest.containsKey(Constants.MANIFEST_CONTENT)) {
            LOGGER.debug(MessageCodes.MFS_051, manifestID, s3Bucket);

            // convert MANIFEST_CONTENT to a buffer
            final Buffer manifestContent = storageRequest.getJsonObject(Constants.MANIFEST_CONTENT).toBuffer();

            // This is pretty rudimentary, we will likely want to add more metadata, but we'll start with an ID
            final UserMetadata metadata = new UserMetadata(Constants.MANIFEST_ID, manifestID);

            // If our connection pool is full, drop back and try resubmitting the request
            try {
                myS3Client.put(s3Bucket, manifestIDwithExt, manifestContent, metadata, response -> {
                    final int statusCode = response.statusCode();

                    response.exceptionHandler(exception -> {
                        final String details = exception.getMessage();

                        LOGGER.error(exception, details);

                        sendReply(aMessage, CodeUtils.getInt(MessageCodes.MFS_052), details);
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
                            final String errorMessage = statusCode + " - " + response.statusMessage();

                            LOGGER.warn(MessageCodes.MFS_055, errorMessage);
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

        } else {
            // log an error, because we ought to be provided MANIFEST_CONTENT, AND we ought to continue
            // regardless
            LOGGER.warn(MessageCodes.MFS_055, manifestID);
            sendReply(aMessage, CodeUtils.getInt(MessageCodes.MFS_055), manifestID);
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
                    sendReply(aMessage, CodeUtils.getInt(MessageCodes.MFS_058), EMPTY);
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

        promise.future().setHandler(aHandler);

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
