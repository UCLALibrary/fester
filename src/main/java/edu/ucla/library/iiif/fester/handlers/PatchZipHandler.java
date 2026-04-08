
package edu.ucla.library.iiif.fester.handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import info.freelibrary.iiif.presentation.v3.ResourceTypes;
import info.freelibrary.iiif.presentation.v3.utils.JsonKeys;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.MergeUtils;
import edu.ucla.library.iiif.fester.verticles.S3BucketVerticle;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handles Zip file patches.
 */
public class PatchZipHandler extends AbstractFesterHandler {

    /** A logger for the handler. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PatchZipHandler.class, Constants.MESSAGES);

    /**
     * Creates a handler to handle PATCHes that upload ZIP files.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A application configuration
     * @throws IOException If there is trouble reading the HTML template files
     */
    public PatchZipHandler(final Vertx aVertx, final JsonObject aConfig) throws IOException {
        super(aVertx, aConfig);
    }

    /**
     * Handles a PATCH request.
     *
     * @param aContext A routing context
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void handle(RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final List<Future> uploads = new ArrayList<>();
        final Buffer body = aContext.getBody();

        if (body == null || body.length() == 0) {
            response.setStatusCode(HTTP.BAD_REQUEST).end(LOGGER.getMessage(MessageCodes.MFS_185));
        } else {
            final byte[] bytes = body.getBytes();

            try (ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                ZipEntry entry;

                while ((entry = zipStream.getNextEntry()) != null) {
                    try {
                        update(decodeID(entry.getName()), readEntry(zipStream));
                    } finally {
                        zipStream.closeEntry();
                    }
                }
            } catch (final ReplyException details) {
                response.setStatusCode(details.failureCode()).end(details.getMessage());
            } catch (final IOException details) {
                response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR).end(details.getMessage());
            }

            // Confirm we haven't written a response already because of a failed validation or other error
            if (!response.headWritten()) {
                CompositeFuture.all(uploads).onComplete(upload -> {
                    if (upload.succeeded()) {
                        response.setStatusCode(HTTP.OK).end();
                    } else {
                        final Throwable aThrowable = upload.cause();
                        final String failMessage = aThrowable.getMessage();
                        final String errorMessage = LOGGER.getMessage(MessageCodes.MFS_015, failMessage);

                        LOGGER.error(aThrowable, errorMessage);

                        response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                        response.setStatusMessage(failMessage);
                        response.putHeader(Constants.CONTENT_TYPE, Constants.PLAIN_TEXT_TYPE);
                        response.end(errorMessage);
                    }
                });
            }
        }
    }

    /**
     * Reads an entry from a Zip stream.
     *
     * @param aZipStream A stream from a Zip file
     * @return The entry as a JSON document
     * @throws IOException if there is trouble reading from the Zip stream
     */
    private JsonObject readEntry(final ZipInputStream aZipStream) throws IOException {
        final ByteArrayOutputStream entryData = new ByteArrayOutputStream();
        final byte[] buffer = new byte[4096];

        int read;

        while ((read = aZipStream.read(buffer)) != -1) {
            entryData.write(buffer, 0, read);
        }

        return new JsonObject(new String(entryData.toByteArray(), StandardCharsets.UTF_8));
    }

    /**
     * Updates a IIIF resource in the S3 manifest store.
     *
     * @param aID The ID of the resource to update
     * @param aResource The IIIF resource
     * @return A promise that the work will be completed in the future
     */
    private Future<Void> update(final String aID, final JsonObject aResource) {
        final String type = aResource.getString(JsonKeys.TYPE);
        final Promise<Void> promise = Promise.promise();

        try {
            validate(aResource, type);
        } catch (final ValidationException details) {
            promise.fail(details);
            return promise.future();
        }

        // We can just overwrite manifest updates
        if (ResourceTypes.MANIFEST.equals(type)) {
            final JsonObject message = getMessage(type, aID, aResource);
            final DeliveryOptions config = getDeliveryOpts(type, aID, Op.PUT_MANIFEST);

            sendMessage(S3BucketVerticle.class.getName(), message, config, send -> {
                if (send.succeeded()) {
                    LOGGER.debug(MessageCodes.MFS_189, aID);
                    promise.complete();
                } else {
                    promise.fail(send.cause());
                }
            });
        } else if (ResourceTypes.COLLECTION.equals(type)) {
            final JsonObject message = getMessage(type, aID, aResource);
            final DeliveryOptions get = getDeliveryOpts(type, aID, Op.GET_COLLECTION);

            sendMessage(S3BucketVerticle.class.getName(), message, get, sendGet -> {
                if (sendGet.succeeded()) {
                    final Message<JsonObject> result = sendGet.result();
                    final DeliveryOptions put = getDeliveryOpts(type, aID, Op.PUT_COLLECTION);
                    final JsonObject collection = MergeUtils.update(result.body(), aResource);
                    final JsonObject update = getMessage(type, aID, collection);

                    sendMessage(S3BucketVerticle.class.getName(), update, put, sendPut -> {
                        if (sendPut.succeeded()) {
                            LOGGER.debug(MessageCodes.MFS_189, aID);
                            promise.complete();
                        } else {
                            promise.fail(sendPut.cause());
                        }
                    });
                } else {
                    final ReplyException getReply = (ReplyException) sendGet.cause();
                    final int getCode = getReply.failureCode();

                    if (getCode == HTTP.NOT_FOUND) {
                        final DeliveryOptions put = getDeliveryOpts(type, aID, Op.PUT_COLLECTION);

                        sendMessage(S3BucketVerticle.class.getName(), message, put, sendPut -> {
                            if (sendPut.succeeded()) {
                                LOGGER.debug(MessageCodes.MFS_189, aID);
                                promise.complete();
                            } else {
                                promise.fail(sendPut.cause());
                            }
                        });
                    } else {
                        promise.fail(getReply);
                    }
                }
            });
        }

        return promise.future();
    }

    /**
     * Gets an S3 message.
     *
     * @param aType The type of IIIF resource
     * @param aID The ID of the IIIF resource
     * @return The message as a JSON object
     */
    private JsonObject getMessage(final String aType, final String aID, final JsonObject aResource) {
        final JsonObject message = new JsonObject();

        if (ResourceTypes.MANIFEST.equals(aType)) {
            message.put(Constants.MANIFEST_ID, aID);
        } else if (ResourceTypes.COLLECTION.equals(aType)) {
            message.put(Constants.COLLECTION_NAME, aID);
        } else {
            LOGGER.error(MessageCodes.MFS_188, aID);
        }

        return message.put(Constants.DATA, aResource);
    }

    /**
     * Gets the delivery options from the supplied arguments.
     *
     * @param aType A type of IIIF resource
     * @param aID The ID of the IIIF resource
     * @param aOp An S3 operation
     * @return A delivery configuration
     */
    private DeliveryOptions getDeliveryOpts(final String aType, final String aID, final String aOp) {
        final DeliveryOptions options = new DeliveryOptions();

        if (ResourceTypes.MANIFEST.equals(aType)) {
            options.addHeader(Constants.ACTION, aOp);
        } else if (ResourceTypes.COLLECTION.equals(aType)) {
            options.addHeader(Constants.ACTION, aOp);
        } else {
            LOGGER.error(MessageCodes.MFS_188, aID);
        }

        return options;
    }

    /**
     * Decodes an encoded item ID.
     *
     * @param aID An encoded ID
     * @return A decoded ID
     */
    private String decodeID(final String aID) {
        return aID.endsWith(".json") ? URLDecoder.decode(aID.substring(0, aID.length() - 5), StandardCharsets.UTF_8)
                : URLDecoder.decode(aID, StandardCharsets.UTF_8);
    }
}
