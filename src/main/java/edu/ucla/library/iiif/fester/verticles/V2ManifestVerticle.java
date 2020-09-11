
package edu.ucla.library.iiif.fester.verticles;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import info.freelibrary.iiif.presentation.v2.Canvas;
import info.freelibrary.iiif.presentation.v2.Collection;
import info.freelibrary.iiif.presentation.v2.ImageContent;
import info.freelibrary.iiif.presentation.v2.ImageResource;
import info.freelibrary.iiif.presentation.v2.Manifest;
import info.freelibrary.iiif.presentation.v2.Sequence;
import info.freelibrary.iiif.presentation.v2.properties.Attribution;
import info.freelibrary.iiif.presentation.v2.properties.Label;
import info.freelibrary.iiif.presentation.v2.properties.Metadata;
import info.freelibrary.iiif.presentation.v2.properties.ViewingDirection;
import info.freelibrary.iiif.presentation.v2.properties.ViewingHint;
import info.freelibrary.iiif.presentation.v2.services.ImageInfoService;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.CsvHeaders;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.ImageInfoLookup;
import edu.ucla.library.iiif.fester.ImageNotFoundException;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.MetadataLabels;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import edu.ucla.library.iiif.fester.utils.ItemSequenceComparator;
import edu.ucla.library.iiif.fester.utils.V2ManifestLabelComparator;

import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A verticle that updates pages on a version 2 presentation manifest.
 */
public class V2ManifestVerticle extends AbstractFesterVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(V2ManifestVerticle.class, MessageCodes.BUNDLE);

    private static final String SEQUENCE_URI = "{}/{}/manifest/sequence/normal";

    private static final String MANIFEST_URI = "{}/{}/manifest";

    private static final String CANVAS_URI = "{}/{}/manifest/canvas/{}";

    private static final String ANNOTATION_URI = "{}/{}/annotation/{}";

    private static final String SIMPLE_URI = "{}/{}";

    /**
     * Starts a verticle to update pages on a manifest.
     */
    @Override
    public void start(final Promise<Void> aPromise) {
        getJsonConsumer().handler(message -> {
            final String action = message.headers().get(Constants.ACTION);

            try {
                // These are the actions that a versioned manifest verticle needs to implement
                // This seems like something we could turn into a Vert.x service in the future
                switch (action) {
                    case ManifestVerticle.UPDATE_PAGES:
                        updatePages(message);
                        break;
                    case ManifestVerticle.UPDATE_COLLECTION:
                        updateCollection(message);
                        break;
                    case ManifestVerticle.CREATE_COLLECTION:
                        createCollection(message);
                        break;
                    case ManifestVerticle.CREATE_WORK:
                        createWork(message);
                        break;
                    default:
                        message.fail(HTTP.INTERNAL_SERVER_ERROR, LOGGER.getMessage(MessageCodes.MFS_153, action));
                }
            } catch (final JsonProcessingException details) {
                LOGGER.error(details, details.getMessage());
                message.fail(HTTP.INTERNAL_SERVER_ERROR, details.getMessage());
            }
        });

        aPromise.complete();
    }

    /**
     * Creates a new collection.
     *
     * @param aMessage An event queue message
     * @throws JsonProcessingException If there is trouble deserializing message components
     */
    private void createCollection(final Message<JsonObject> aMessage) throws JsonProcessingException {
        final JsonObject body = aMessage.body();
        final JsonObject message = new JsonObject();
        final ObjectMapper mapper = new ObjectMapper();
        final DeliveryOptions options = new DeliveryOptions();
        final String collectionName = body.getString(Constants.COLLECTION_NAME);
        final JsonArray collectionArray = body.getJsonArray(Constants.COLLECTION_CONTENT);
        final CsvHeaders csvHeaders = CsvHeaders.fromJSON(body.getJsonObject(Constants.CSV_HEADERS));
        final TypeReference<String[]> arrayTypeRef = new TypeReference<>() {};
        final String[] collectionData = mapper.readValue(collectionArray.encode(), arrayTypeRef);
        final String collectionID = collectionData[csvHeaders.getItemArkIndex()];
        final URI uri = IDUtils.getResourceURI(Constants.URL_PLACEHOLDER, IDUtils.getCollectionS3Key(collectionID));
        final Label label = new Label(collectionData[csvHeaders.getTitleIndex()]);
        final Optional<String> manifests = Optional.ofNullable(body.getString(Constants.MANIFEST_CONTENT));
        final Collection collection = new Collection(uri, label);
        final Metadata metadata = new Metadata();

        // Add optional properties below
        getMetadata(collectionData, csvHeaders.getRepositoryNameIndex()).ifPresent(repoName -> {
            metadata.add(MetadataLabels.REPOSITORY_NAME, repoName);
        });

        getMetadata(collectionData, csvHeaders.getLocalRightsStatementIndex()).ifPresent(rightsStatement -> {
            collection.setAttribution(new Attribution(rightsStatement));
        });

        getMetadata(collectionData, csvHeaders.getRightsContactIndex()).ifPresent(rightsContract -> {
            metadata.add(MetadataLabels.RIGHTS_CONTACT, rightsContract);
        });

        if (metadata.getEntries().size() > 0) {
            collection.setMetadata(metadata);
        }

        // If we have work manifests, add them to the collection
        if (manifests.isPresent()) {
            final SortedSet<Collection.Manifest> sortedSet = new TreeSet<>(new V2ManifestLabelComparator());
            final TypeReference<List<String[]>> listTypeRef = new TypeReference<>() {};

            for (final String[] workArray : mapper.readValue(manifests.get(), listTypeRef)) {
                sortedSet.add(new Collection.Manifest(URI.create(workArray[0]), new Label(workArray[1])));
            }

            collection.getManifests().addAll(sortedSet);
        }

        options.addHeader(Constants.ACTION, Op.PUT_COLLECTION);
        message.put(Constants.COLLECTION_NAME, collectionName);
        message.put(Constants.DATA, collection.toJSON());

        sendMessage(S3BucketVerticle.class.getName(), message, options, send -> {
            if (send.succeeded()) {
                aMessage.reply(new JsonObject());
            } else {
                error(aMessage, send.cause(), MessageCodes.MFS_125, send.cause().getMessage());
            }
        });
    }

    /**
     * Creates a single work from the data that sent as a message.
     *
     * @param aMessage Information needed to create a work manifest
     * @throws JsonProcessingException If there is trouble deserializing shared information
     */
    private void createWork(final Message<JsonObject> aMessage) throws JsonProcessingException {
        final JsonObject body = aMessage.body();
        final ObjectMapper mapper = new ObjectMapper();
        final CsvHeaders csvHeaders = CsvHeaders.fromJSON(body.getJsonObject(Constants.CSV_HEADERS));
        final JsonArray workArray = body.getJsonArray(Constants.MANIFEST_CONTENT);
        final String[] workRow = mapper.readValue(workArray.encode(), new TypeReference<String[]>() {});
        final JsonObject pagesJSON = body.getJsonObject(Constants.MANIFEST_PAGES);
        final TypeReference<Map<String, List<String[]>>> type = new TypeReference<>() {};
        final Map<String, List<String[]>> pagesMap = mapper.readValue(pagesJSON.encode(), type);
        final String placeholderImage = body.getString(Constants.PLACEHOLDER_IMAGE);
        final String imageHost = body.getString(Constants.IIIF_HOST);
        final String workID = workRow[csvHeaders.getItemArkIndex()];
        final String encodedWorkID = URLEncoder.encode(workID, StandardCharsets.UTF_8);
        final String manifestID = StringUtils.format(MANIFEST_URI, Constants.URL_PLACEHOLDER, encodedWorkID);
        final String sequenceID = StringUtils.format(SEQUENCE_URI, Constants.URL_PLACEHOLDER, encodedWorkID);
        final Manifest manifest = new Manifest(manifestID, workRow[csvHeaders.getTitleIndex()]);
        final Sequence sequence = new Sequence().setID(sequenceID);
        final DeliveryOptions options = new DeliveryOptions();
        final JsonObject message = new JsonObject();
        final Metadata metadata = new Metadata();
        final JsonObject jsonManifest;

        getMetadata(workRow, csvHeaders.getViewingDirectionIndex()).ifPresent(viewingDirection -> {
            manifest.setViewingDirection(ViewingDirection.fromString(viewingDirection));
        });

        getMetadata(workRow, csvHeaders.getViewingHintIndex()).ifPresent(viewingHint -> {
            manifest.setViewingHint(new ViewingHint(viewingHint));
        });

        getMetadata(workRow, csvHeaders.getRepositoryNameIndex()).ifPresent(repositoryName -> {
            metadata.add(MetadataLabels.REPOSITORY_NAME, repositoryName);
        });

        getMetadata(workRow, csvHeaders.getLocalRightsStatementIndex()).ifPresent(localRightsStatement -> {
            manifest.setAttribution(new Attribution(localRightsStatement));
        });

        getMetadata(workRow, csvHeaders.getRightsContactIndex()).ifPresent(rightsContract -> {
            metadata.add(MetadataLabels.RIGHTS_CONTACT, rightsContract);
        });

        if (metadata.getEntries().size() > 0) {
            manifest.setMetadata(metadata);
        }

        // Check first for pages, then if the work itself is an image
        if (pagesMap.containsKey(workID)) {
            final List<String[]> pageList = pagesMap.get(workID);

            manifest.addSequence(sequence);
            pageList.sort(new ItemSequenceComparator(csvHeaders.getItemSequenceIndex()));
            sequence.addCanvas(createCanvases(csvHeaders, pageList, imageHost, placeholderImage, encodedWorkID));
        } else {
            getMetadata(workRow, csvHeaders.getImageAccessUrlIndex()).ifPresent(accessURL -> {
                final List<String[]> pageList = new ArrayList<>(1);

                pageList.add(workRow);
                manifest.addSequence(sequence);
                sequence.addCanvas(createCanvases(csvHeaders, pageList, imageHost, placeholderImage, encodedWorkID));
            });
        }

        jsonManifest = manifest.toJSON();
        message.put(Constants.DATA, jsonManifest);
        message.put(Constants.MANIFEST_ID, workID);
        options.addHeader(Constants.ACTION, Op.PUT_MANIFEST);

        sendMessage(S3BucketVerticle.class.getName(), message, options, send -> {
            if (send.succeeded()) {
                aMessage.reply(jsonManifest);
            } else {
                error(aMessage, send.cause(), MessageCodes.MFS_151, send.cause().getMessage());
            }
        });
    }

    /**
     * Updates a collection with new works.
     *
     * @param aMessage A event queue message
     */
    private void updateCollection(final Message<JsonObject> aMessage) throws JsonProcessingException {
        final JsonObject body = aMessage.body();
        final String collectionName = body.getString(Constants.COLLECTION_NAME);
        final JsonObject worksJSON = body.getJsonObject(Constants.MANIFEST_CONTENT);
        final Collection collection = Collection.fromJSON(body.getJsonObject(Constants.COLLECTION_CONTENT));
        final TypeReference<Map<String, List<String[]>>> type = new TypeReference<>() {};
        final Map<String, List<String[]>> worksMap = new ObjectMapper().readValue(worksJSON.encode(), type);
        final SortedSet<Collection.Manifest> sortedManifestSet = new TreeSet<>(new V2ManifestLabelComparator());
        final Map<URI, Collection.Manifest> manifestMap = new HashMap<>(); // Using to eliminate duplicates
        final DeliveryOptions options = new DeliveryOptions();
        final JsonObject message = new JsonObject();

        // First, add the old manifests to the map
        final Stream<Collection.Manifest> stream = collection.getManifests().stream();
        manifestMap.putAll(stream.collect(Collectors.toMap(Collection.Manifest::getID, manifest -> manifest)));

        // Next, add the new manifests to the map, replacing any that already exist
        worksMap.get(IDUtils.getResourceID(collection.getID())).stream().forEach(workArray -> {
            final URI manifestURI = URI.create(workArray[0]);
            manifestMap.put(manifestURI, new Collection.Manifest(manifestURI, new Label(workArray[1])));
        });

        // Update the manifest list with the manifests in the map, ordered by their label
        sortedManifestSet.addAll(manifestMap.values());

        collection.getManifests().clear();
        collection.getManifests().addAll(sortedManifestSet);

        message.put(Constants.DATA, collection.toJSON());
        message.put(Constants.COLLECTION_NAME, collectionName);
        options.addHeader(Constants.ACTION, Op.PUT_COLLECTION);

        sendMessage(S3BucketVerticle.class.getName(), message, options, update -> {
            if (update.succeeded()) {
                aMessage.reply(collection.toJSON());
            } else {
                error(aMessage, update.cause(), MessageCodes.MFS_152, update.cause().getMessage());
            }
        });
    }

    /**
     * Updates pages in a work.
     *
     * @param aMessage A message with information about the page updates
     * @throws JsonProcessingException If there is trouble deserializing message components
     */
    private void updatePages(final Message<JsonObject> aMessage) throws JsonProcessingException {
        final JsonObject body = aMessage.body();
        final String workID = body.getString(Constants.MANIFEST_ID);
        final String imageHost = body.getString(Constants.IIIF_HOST);
        final String placeholderImage = body.getString(Constants.PLACEHOLDER_IMAGE);
        final String encodedWorkID = URLEncoder.encode(workID, StandardCharsets.UTF_8);
        final Manifest manifest = Manifest.fromJSON(body.getJsonObject(Constants.MANIFEST_CONTENT));
        final CsvHeaders csvHeaders = CsvHeaders.fromJSON(body.getJsonObject(Constants.CSV_HEADERS));
        final TypeReference<List<String[]>> typeRef = new TypeReference<>() {};
        final JsonArray pagesArray = body.getJsonArray(Constants.MANIFEST_PAGES);
        final List<String[]> pagesList = new ObjectMapper().readValue(pagesArray.encode(), typeRef);
        final List<Sequence> sequences = manifest.getSequences();
        final DeliveryOptions options = new DeliveryOptions();
        final JsonObject message = new JsonObject();
        final JsonObject jsonManifest;
        final Sequence sequence;

        // If the work doesn't already have any sequences, create one for it
        if (sequences.isEmpty()) {
            final String sequenceID = StringUtils.format(SEQUENCE_URI, Constants.URL_PLACEHOLDER, encodedWorkID);

            sequence = new Sequence().setID(sequenceID);
            manifest.addSequence(sequence);
        } else {
            sequence = sequences.get(0); // For now we're just dealing with single sequence works
        }

        sequence.getCanvases().clear(); // Overwrite whatever canvases are on the manifest
        pagesList.sort(new ItemSequenceComparator(csvHeaders.getItemSequenceIndex()));
        sequence.addCanvas(createCanvases(csvHeaders, pagesList, imageHost, placeholderImage, encodedWorkID));

        jsonManifest = manifest.toJSON();
        message.put(Constants.DATA, jsonManifest);
        message.put(Constants.MANIFEST_ID, workID);
        options.addHeader(Constants.ACTION, Op.PUT_MANIFEST);

        sendMessage(S3BucketVerticle.class.getName(), message, options, send -> {
            if (send.succeeded()) {
                aMessage.reply(jsonManifest);
            } else {
                error(aMessage, send.cause(), MessageCodes.MFS_054, workID, send.cause().getMessage());
            }
        });
    }

    /**
     * Adds pages to a sequence from a work manifest.
     *
     * @param aCsvHeaders A CSV headers
     * @param aPageList A list of pages to add
     * @param aSequence A sequence to add pages to
     * @param aImageHost An image host for image links
     * @param aWorkID A URL encoded work ID
     * @throws IOException If there is trouble adding a page
     */
    private Canvas[] createCanvases(final CsvHeaders aCsvHeaders, final List<String[]> aPageList,
            final String aImageHost, final String aPlaceholderImage, final String aWorkID) {
        final Iterator<String[]> iterator = aPageList.iterator();
        final List<Canvas> canvases = new ArrayList<>();

        while (iterator.hasNext()) {
            final String[] columns = iterator.next();
            final String pageID = columns[aCsvHeaders.getItemArkIndex()];
            final String idPart = IDUtils.getLastPart(pageID);
            final String pageLabel = columns[aCsvHeaders.getTitleIndex()];
            final String encodedPageID = URLEncoder.encode(pageID, StandardCharsets.UTF_8);
            final String canvasID = StringUtils.format(CANVAS_URI, Constants.URL_PLACEHOLDER, aWorkID, idPart);
            final String pageURI = StringUtils.format(SIMPLE_URI, aImageHost, encodedPageID);
            final String contentURI = StringUtils.format(ANNOTATION_URI, Constants.URL_PLACEHOLDER, aWorkID, idPart);

            String resourceURI = StringUtils.format(Constants.DEFAULT_THUMBNAIL_URI_TEMPLATE, pageURI,
                    Constants.DEFAULT_THUMBNAIL_SIZE);
            ImageResource imageResource = new ImageResource(resourceURI, new ImageInfoService(pageURI));
            ImageContent imageContent;
            Canvas canvas;

            try {
                final ImageInfoLookup infoLookup = new ImageInfoLookup(pageURI);
                final int width = infoLookup.getWidth();
                final int height = infoLookup.getHeight();

                // Create a canvas using the width and height of the related image
                canvas = new Canvas(canvasID, pageLabel, width, height);
                imageContent = new ImageContent(contentURI, canvas);
                imageContent.addResource(imageResource);
                canvas.addImageContent(imageContent);
            } catch (final ImageNotFoundException | IOException details) {
                LOGGER.info(MessageCodes.MFS_078, pageID);

                if (aPlaceholderImage != null) {
                    try {
                        final ImageInfoLookup placeholderLookup = new ImageInfoLookup(aPlaceholderImage);
                        final int width = placeholderLookup.getWidth();
                        final int height = placeholderLookup.getHeight();
                        final int size;

                        if (width >= Constants.DEFAULT_THUMBNAIL_SIZE) {
                            size = Constants.DEFAULT_THUMBNAIL_SIZE;
                        } else {
                            size = width;
                        }

                        // If placeholder image found, use its URL for image resource and service
                        resourceURI = StringUtils.format(Constants.DEFAULT_THUMBNAIL_URI_TEMPLATE, aPlaceholderImage,
                                size);
                        imageResource = new ImageResource(resourceURI, new ImageInfoService(aPlaceholderImage));

                        // Create a canvas using the width and height of the placeholder image
                        canvas = new Canvas(canvasID, pageLabel, width, height);
                        imageContent = new ImageContent(contentURI, canvas);
                        imageContent.addResource(imageResource);
                        canvas.addImageContent(imageContent);
                    } catch (final ImageNotFoundException | IOException lookupDetails) {
                        // We couldn't find the placeholder image so we create an empty canvas
                        canvas = new Canvas(canvasID, pageLabel, 0, 0);
                        LOGGER.error(lookupDetails, lookupDetails.getMessage());

                        // No image content added to canvas when we couldn't find any
                    }
                } else {
                    // We couldn't find the placeholder image so we create an empty canvas
                    canvas = new Canvas(canvasID, pageLabel, 0, 0);
                    LOGGER.info(MessageCodes.MFS_099, pageID);

                    // No image content added to canvas when we couldn't find any
                }
            }

            if (aCsvHeaders.hasViewingHintIndex()) {
                final String viewingHint = StringUtils.trimToNull(columns[aCsvHeaders.getViewingHintIndex()]);

                if (viewingHint != null) {
                    canvas.setViewingHint(new ViewingHint(viewingHint));
                }
            }

            canvases.add(canvas);
        }

        return canvases.toArray(new Canvas[] {});
    }
}
