
package edu.ucla.library.iiif.fester.verticles;

import static edu.ucla.library.iiif.fester.Constants.EMPTY;

import java.net.URI;

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

    private static final String ID = "@id";

    private S3Client myS3Client;

    private String myS3Bucket;

    /**
     * Starts the S3 Bucket Verticle.
     */
    @Override
    @SuppressWarnings("Indentation") // Checkstyle's indentation check doesn't work with multiple lambdas
    public void start() throws Exception {
        super.start(); // We do some stuff in the abstract class that we want here

        final JsonObject config = config();

        // Initialize the S3BucketVerticle by getting our s3 configs and setting up the S3 client
        if (myS3Client == null) {
            final String s3AccessKey = config.getString(Config.S3_ACCESS_KEY);
            final String s3SecretKey = config.getString(Config.S3_SECRET_KEY);
            final String s3RegionName = config.getString(Config.S3_REGION);
            final String s3Region = RegionUtils.getRegion(s3RegionName).getServiceEndpoint("s3");
            final HttpClientOptions httpOptions = new HttpClientOptions().setDefaultHost(s3Region);

            myS3Client = new S3Client(getVertx(), s3AccessKey, s3SecretKey, httpOptions);
            myS3Bucket = config.getString(Config.S3_BUCKET);

            // Trace is only for developer use; don't turn on when running on a server
            LOGGER.trace(MessageCodes.MFS_046, s3AccessKey, s3SecretKey);
            LOGGER.debug(MessageCodes.MFS_047, s3RegionName);
        }

        getJsonConsumer().handler(message -> {
            final JsonObject manifest = message.body();
            final String manifestID = getUniqueID(manifest.getString(ID));
            final String manifestKey = getS3Key(manifestID);
            final Buffer manifestContent = manifest.toBuffer();

            LOGGER.debug(MessageCodes.MFS_051, manifest, myS3Bucket);

            // Start with just ID for manifest metadata
            final UserMetadata metadata = new UserMetadata(Constants.MANIFEST_ID, manifestID);

            try {
                myS3Client.put(myS3Bucket, manifestKey, manifestContent, metadata, response -> {
                    final int statusCode = response.statusCode();

                    response.exceptionHandler(exception -> {
                        final String details = exception.getMessage();

                        LOGGER.error(exception, details);

                        sendReply(message, CodeUtils.getInt(MessageCodes.MFS_052), details);
                    });

                    // If we get a successful upload response code, send a reply to indicate so
                    if (statusCode == HTTP.OK) {
                        LOGGER.info(MessageCodes.MFS_053, manifestID);

                        // Send the success result and decrement the S3 request counter
                        sendReply(message, 0, Op.SUCCESS);
                    } else {
                        LOGGER.error(MessageCodes.MFS_054, statusCode, response.statusMessage());

                        // Log the detailed reason we failed so we can track down the issue
                        response.bodyHandler(body -> {
                            LOGGER.error(MessageCodes.MFS_052, body.getString(0, body.length()));
                        });

                        // If there is some internal S3 server error, let's try again
                        if (statusCode == HTTP.INTERNAL_SERVER_ERROR) {
                            sendReply(message, 0, Op.RETRY);
                        } else {
                            final String errorMessage = statusCode + " - " + response.statusMessage();

                            LOGGER.warn(MessageCodes.MFS_055, errorMessage);

                            retryUpload(manifestID, message);
                        }
                    }
                }, exception -> {
                    LOGGER.warn(MessageCodes.MFS_055, exception.getMessage());
                    retryUpload(manifestID, message);
                });
            } catch (final ConnectionPoolTooBusyException details) {
                LOGGER.debug(MessageCodes.MFS_056, manifestID);
                sendReply(message, 0, Op.RETRY);
            }
        });
    }

    /**
     * Gets the ID and checks whether it's a work or collection manifest and then returns the ID for that thing.
     *
     * @param aURIString A manifest URI
     * @return An S3 key for the manifest
     */
    private String getUniqueID(final String aURIString) {
        String uniqueID;

        if (aURIString.contains(Constants.COLLECTIONS_PATH)) {
            uniqueID = IDUtils.decode(URI.create(aURIString), Constants.COLLECTIONS_PATH);
            uniqueID = Constants.COLLECTIONS_PATH + Constants.SLASH + uniqueID;
        } else {
            // TODO: update this to put Work manifests in a works S3 "directory"
            uniqueID = IDUtils.decode(URI.create(aURIString));
        }

        LOGGER.debug(MessageCodes.MFS_128, uniqueID);

        return uniqueID;
    }

    /**
     * Gets the key that will be used when putting the manifest in S3.
     *
     * @param aID The unique part of a manifest ID
     * @return An S3 key
     */
    private String getS3Key(final String aID) {
        return !aID.endsWith(Constants.JSON_EXT) ? aID + Constants.JSON_EXT : aID;
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
