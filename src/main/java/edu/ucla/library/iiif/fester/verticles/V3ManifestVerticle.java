
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

import info.freelibrary.iiif.presentation.v3.Collection;
import info.freelibrary.iiif.presentation.v3.properties.Label;
import info.freelibrary.iiif.presentation.v3.properties.Metadata;
import info.freelibrary.iiif.presentation.v3.properties.RequiredStatement;
import info.freelibrary.iiif.presentation.v3.Manifest;
import info.freelibrary.iiif.presentation.v3.id.Minter;
import info.freelibrary.iiif.presentation.v3.id.MinterFactory;
import info.freelibrary.iiif.presentation.v3.properties.ViewingDirection;
import info.freelibrary.iiif.presentation.v3.properties.behaviors.CanvasBehavior;
import info.freelibrary.iiif.presentation.v3.properties.behaviors.ManifestBehavior;
import info.freelibrary.iiif.presentation.v3.services.ImageService2;
import info.freelibrary.iiif.presentation.v3.ImageContent;
import info.freelibrary.iiif.presentation.v3.Canvas;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;
import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.CsvHeaders;
import edu.ucla.library.iiif.fester.CsvParser;
import edu.ucla.library.iiif.fester.CsvParsingException;
import edu.ucla.library.iiif.fester.HTTP;
import edu.ucla.library.iiif.fester.ImageInfoLookup;
import edu.ucla.library.iiif.fester.ImageNotFoundException;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.MetadataLabels;
import edu.ucla.library.iiif.fester.ObjectType;
import edu.ucla.library.iiif.fester.Op;
import edu.ucla.library.iiif.fester.utils.IDUtils;
import edu.ucla.library.iiif.fester.utils.ItemSequenceComparator;
import edu.ucla.library.iiif.fester.utils.V3CollectionItemLabelComparator;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A verticle that updates pages on a version 3 presentation manifest.
 */
public class V3ManifestVerticle extends AbstractFesterVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(V3ManifestVerticle.class, MessageCodes.BUNDLE);

    private static final String MANIFEST_URI = "{}/{}/manifest";

    private static final String SIMPLE_URI = "{}/{}";

    /**
     * Starts a verticle to update pages on a manifest.
     */
    @Override
    public void start(final Promise<Void> aPromise) {
        getJsonConsumer().handler(message -> {
            final String action = message.headers().get(Constants.ACTION);

            try {
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

        // Add optional properties below
        getMetadata(collectionData, csvHeaders.getRepositoryNameIndex()).ifPresent(repoName -> {
            collection.getMetadata().add(new Metadata(MetadataLabels.REPOSITORY_NAME, repoName));
        });

        getMetadata(collectionData, csvHeaders.getLocalRightsStatementIndex()).ifPresent(rightsStatement -> {
            collection.setRequiredStatement(new RequiredStatement(MetadataLabels.ATTRIBUTION, rightsStatement));
        });

        getMetadata(collectionData, csvHeaders.getRightsContactIndex()).ifPresent(rightsContract -> {
            collection.getMetadata().add(new Metadata(MetadataLabels.RIGHTS_CONTACT, rightsContract));
        });

        // If we have work manifests, add them to the collection
        if (manifests.isPresent()) {
            final SortedSet<Collection.Item> sortedSet = new TreeSet<>(new V3CollectionItemLabelComparator());
            final TypeReference<List<String[]>> listTypeRef = new TypeReference<>() {};

            for (final String[] workArray : mapper.readValue(manifests.get(), listTypeRef)) {
                final Collection.Item item = new Collection.Item(Collection.Item.Type.Manifest,
                        URI.create(workArray[0]), new Label(workArray[1]));
                sortedSet.add(item);
            }

            collection.setItems(new ArrayList<>(sortedSet));
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
        final Manifest manifest = new Manifest(manifestID, workRow[csvHeaders.getTitleIndex()]);
        final Minter minter = MinterFactory.getMinter(manifest);
        final DeliveryOptions options = new DeliveryOptions();
        final JsonObject message = new JsonObject();
        final JsonObject jsonManifest;

        getMetadata(workRow, csvHeaders.getViewingDirectionIndex()).ifPresent(viewingDirection -> {
            manifest.setViewingDirection(ViewingDirection.fromString(viewingDirection));
        });

        getMetadata(workRow, csvHeaders.getViewingHintIndex()).ifPresent(behavior -> {
            manifest.setBehaviors(ManifestBehavior.fromString(behavior));
        });

        getMetadata(workRow, csvHeaders.getRepositoryNameIndex()).ifPresent(repositoryName -> {
            manifest.getMetadata().add(new Metadata(MetadataLabels.REPOSITORY_NAME, repositoryName));
        });

        getMetadata(workRow, csvHeaders.getLocalRightsStatementIndex()).ifPresent(localRightsStatement -> {
            manifest.setRequiredStatement(new RequiredStatement(MetadataLabels.ATTRIBUTION, localRightsStatement));
        });

        getMetadata(workRow, csvHeaders.getRightsContactIndex()).ifPresent(rightsContract -> {
            manifest.getMetadata().add(new Metadata(MetadataLabels.RIGHTS_CONTACT, rightsContract));
        });

        // Check first for pages, then if the work itself is an image
        if (pagesMap.containsKey(workID)) {
            final List<String[]> pageList = pagesMap.get(workID);
            final Canvas[] canvases;

            pageList.sort(new ItemSequenceComparator(csvHeaders.getItemSequenceIndex()));
            canvases = createCanvases(csvHeaders, pageList, imageHost, placeholderImage, encodedWorkID, minter);
            manifest.addCanvas(canvases);
        } else {
            getMetadata(workRow, csvHeaders.getImageAccessUrlIndex()).ifPresent(accessURL -> {
                final List<String[]> pageList = new ArrayList<>(1);
                final Canvas[] canvases;

                pageList.add(workRow);
                canvases = createCanvases(csvHeaders, pageList, imageHost, placeholderImage, encodedWorkID, minter);
                manifest.addCanvas(canvases);
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
        final SortedSet<Collection.Item> sortedCollectionItemSet = new TreeSet<>(new V3CollectionItemLabelComparator());
        final Map<URI, Collection.Item> collectionItemMap = new HashMap<>(); // Using to eliminate duplicates
        final DeliveryOptions options = new DeliveryOptions();
        final JsonObject message = new JsonObject();

        // First, add the old manifests to the map
        final Stream<Collection.Item> stream = collection.getItems().stream();
        collectionItemMap.putAll(stream.collect(Collectors.toMap(Collection.Item::getID, manifest -> manifest)));

        // Next, add the new manifests to the map, replacing any that already exist
        worksMap.get(IDUtils.getResourceID(collection.getID())).stream().forEach(workArray -> {
            final URI manifestURI = URI.create(workArray[0]);
            final Label label = new Label(workArray[1]);
            collectionItemMap.put(manifestURI, new Collection.Item(Collection.Item.Type.Manifest, manifestURI, label));
        });

        // Update the item list with the manifests in the map, ordered by their label
        sortedCollectionItemSet.addAll(collectionItemMap.values());

        collection.getItems().clear();
        collection.getItems().addAll(sortedCollectionItemSet);

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
        final Minter minter = MinterFactory.getMinter(manifest);
        final CsvHeaders csvHeaders = CsvHeaders.fromJSON(body.getJsonObject(Constants.CSV_HEADERS));
        final TypeReference<List<String[]>> typeRef = new TypeReference<>() {};
        final JsonArray pagesArray = body.getJsonArray(Constants.MANIFEST_PAGES);
        final List<String[]> pagesList = new ObjectMapper().readValue(pagesArray.encode(), typeRef);
        final DeliveryOptions options = new DeliveryOptions();
        final JsonObject message = new JsonObject();
        final JsonObject jsonManifest;

        manifest.getCanvases().clear(); // Overwrite whatever canvases are on the manifest
        pagesList.sort(new ItemSequenceComparator(csvHeaders.getItemSequenceIndex()));
        manifest.addCanvas(createCanvases(csvHeaders, pagesList, imageHost, placeholderImage, encodedWorkID, minter));

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
     * Creates canvases to add to a manifest.
     *
     * @param aCsvHeaders A CSV headers
     * @param aPageList A list of pages to add
     * @param aSequence A sequence to add pages to
     * @param aImageHost An image host for image links
     * @param aWorkID A URL encoded work ID
     * @param aMinter An ID minter
     */
    private Canvas[] createCanvases(final CsvHeaders aCsvHeaders, final List<String[]> aPageList,
            final String aImageHost, final String aPlaceholderImage, final String aWorkID, final Minter aMinter) {
        final Iterator<String[]> iterator = aPageList.iterator();
        final List<Canvas> canvases = new ArrayList<>();

        while (iterator.hasNext()) {
            final String[] columns = iterator.next();
            final String pageID = columns[aCsvHeaders.getItemArkIndex()];
            final String pageLabel = columns[aCsvHeaders.getTitleIndex()];
            final String encodedPageID = URLEncoder.encode(pageID, StandardCharsets.UTF_8);
            final String pageURI = StringUtils.format(SIMPLE_URI, aImageHost, encodedPageID);

            String resourceURI = StringUtils.format(Constants.DEFAULT_THUMBNAIL_URI_TEMPLATE, pageURI,
                    Constants.DEFAULT_THUMBNAIL_SIZE);
            Canvas canvas;
            ImageContent image;

            try {
                final ImageInfoLookup infoLookup = new ImageInfoLookup(pageURI);
                final int width = infoLookup.getWidth();
                final int height = infoLookup.getHeight();

                // Create a canvas using the width and height of the related image
                canvas = new Canvas(aMinter, pageLabel);
                image = new ImageContent(resourceURI)
                        .setServices(new ImageService2(ImageService2.Profile.TWO, pageURI));
                canvas.setWidthHeight(width, height).paintWith(aMinter, image);
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

                        // Create a canvas using the width and height of the placeholder image
                        canvas = new Canvas(aMinter, pageLabel);
                        image = new ImageContent(resourceURI)
                                .setServices(new ImageService2(ImageService2.Profile.TWO, aPlaceholderImage));
                        canvas.setWidthHeight(width, height).paintWith(aMinter, image);
                    } catch (final ImageNotFoundException | IOException lookupDetails) {
                        // We couldn't find the placeholder image so we create an empty canvas
                        canvas = new Canvas(aMinter, pageLabel);
                        LOGGER.error(lookupDetails, lookupDetails.getMessage());

                        // No image content added to canvas when we couldn't find any
                    }
                } else {
                    // We couldn't find the placeholder image so we create an empty canvas
                    canvas = new Canvas(aMinter, pageLabel);
                    LOGGER.info(MessageCodes.MFS_099, pageID);

                    // No image content added to canvas when we couldn't find any
                }
            }

            if (aCsvHeaders.hasViewingHintIndex()) {
                final ObjectType objectType;
                final String behavior = StringUtils.trimToNull(columns[aCsvHeaders.getViewingHintIndex()]);

                try {
                    objectType = CsvParser.getObjectType(columns, aCsvHeaders);
                    if (objectType == ObjectType.PAGE && behavior != null) {
                        canvas.setBehaviors(CanvasBehavior.fromString(behavior));
                    }
                } catch (final CsvParsingException details) {
                    LOGGER.error(details.getMessage());
                }
            }

            canvases.add(canvas);
        }

        return canvases.toArray(new Canvas[] {});
    }

}
