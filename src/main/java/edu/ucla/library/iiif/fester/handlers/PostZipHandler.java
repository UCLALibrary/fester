
package edu.ucla.library.iiif.fester.handlers;

import static edu.ucla.library.iiif.fester.Constants.EMPTY;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import info.freelibrary.util.I18nRuntimeException;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import info.freelibrary.iiif.presentation.v3.ResourceTypes;
import info.freelibrary.iiif.presentation.v3.utils.JsonKeys;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.verticles.S3BucketVerticle;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handles ZIP file uploads.
 */
public class PostZipHandler extends AbstractFesterHandler {

    /** A logger for the handler. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PostZipHandler.class, Constants.MESSAGES);

    /**
     * Creates a handler to handle POSTs that upload ZIP files.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A application configuration
     * @throws IOException If there is trouble reading the HTML template files
     */
    public PostZipHandler(final Vertx aVertx, final JsonObject aConfig) throws IOException {
        super(aVertx, aConfig);
    }

    @Override
    @SuppressWarnings("rawtypes")
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
                    final ByteArrayOutputStream entryData = new ByteArrayOutputStream();
                    final byte[] buffer = new byte[4096];

                    int read;

                    while ((read = zipStream.read(buffer)) != -1) {
                        entryData.write(buffer, 0, read);
                    }

                    final byte[] entryBytes = entryData.toByteArray();
                    final String json = new String(entryBytes, UTF_8);

                    try {
                        final JsonObject jsonObject = new JsonObject(json);
                        final String type = jsonObject.getString(JsonKeys.TYPE);

                        if (type.equals(ResourceTypes.COLLECTION)) {
                            try {
                                validate(jsonObject, ResourceTypes.COLLECTION);
                            } catch (final ValidationException details) {
                                sendErrorResponse(response, details);
                                return;
                            }

                            upload(entry.getName(), ResourceTypes.COLLECTION, jsonObject);
                        } else if (type.equals(ResourceTypes.MANIFEST)) {
                            try {
                                validate(jsonObject, ResourceTypes.MANIFEST);
                            } catch (final ValidationException details) {
                                sendErrorResponse(response, details);
                                return;
                            }

                            upload(entry.getName(), ResourceTypes.MANIFEST, jsonObject);
                        } else {
                            response.setStatusCode(400).end(LOGGER.getMessage(MessageCodes.MFS_186));
                        }
                    } catch (final DecodeException details) {
                        response.setStatusCode(400).end(LOGGER.getMessage(MessageCodes.MFS_186));
                    }

                    zipStream.closeEntry();
                }
            } catch (final IOException details) {
                response.setStatusCode(500).end(details.getMessage());
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
     * Sends an error response.
     *
     * @param aResponse A HTTP response
     * @param aException An exception representing what went wrong
     */
    private void sendErrorResponse(final HttpServerResponse aResponse, final Exception aException) {
        LOGGER.error(aException.getMessage(), aException);
        aResponse.setStatusCode(HTTP.BAD_REQUEST);
        aResponse.setStatusMessage(aException.getMessage());
        aResponse.putHeader(Constants.CONTENT_TYPE, Constants.PLAIN_TEXT_TYPE);
        aResponse.end(aException.getMessage());
    }

    /**
     * Upload the supplied IIIF resource.
     *
     * @param aID The resource's ID
     * @param aType The type of resource (e.g., collection doc or manifest)
     * @param aResource The resource in {@code JsonObject} form
     * @return The result of the upload
     */
    private Future<Void> upload(final String aID, final String aType, final JsonObject aResource) {
        final String id = URLDecoder.decode(aID, UTF_8).replaceAll("\\.json$", EMPTY);
        final DeliveryOptions options = new DeliveryOptions();
        final Promise<Void> promise = Promise.promise();
        final JsonObject message = new JsonObject();

        message.put(Constants.DATA, aResource);

        if (ResourceTypes.COLLECTION.equals(aType)) {
            message.put(Constants.COLLECTION_NAME, id);
            options.addHeader(Constants.ACTION, Op.PUT_COLLECTION);
        } else if (ResourceTypes.MANIFEST.equals(aType)) {
            message.put(Constants.MANIFEST_ID, id);
            options.addHeader(Constants.ACTION, Op.PUT_MANIFEST);
        } else {
            promise.fail(new I18nRuntimeException(MessageCodes.BUNDLE, MessageCodes.MFS_188));
        }

        // Send the resource to the S3 verticle for it to do the upload
        sendMessage(S3BucketVerticle.class.getName(), message, options, send -> {
            if (send.succeeded()) {
                promise.complete();
            } else {
                promise.fail(send.cause());
            }
        });

        return promise.future();
    }
}
