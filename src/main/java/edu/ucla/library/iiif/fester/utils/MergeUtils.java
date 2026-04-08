
package edu.ucla.library.iiif.fester.utils;

import java.util.HashSet;
import java.util.Set;

import info.freelibrary.iiif.presentation.v3.utils.JsonKeys;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Utilities related to the merging of resource records.
 */
public final class MergeUtils {

    /**
     * Merges two collections together based on child IDs.
     *
     * @param aExisting A pre-existing JSON collection
     * @param aModified A JSON collection with modifications
     * @return A merged JSON object
     */
    public static final JsonObject update(final JsonObject aExisting, final JsonObject aModification) {
        final JsonArray modItems = aModification.getJsonArray(JsonKeys.ITEMS, new JsonArray());
        final JsonObject merged = aExisting.copy();
        final Set<String> existingIDs = new HashSet<>();

        // Build a set of the known IDs
        merged.getJsonArray(JsonKeys.ITEMS, new JsonArray()).forEach(object -> {
            if (object instanceof JsonObject) {
                final JsonObject item = (JsonObject) object;
                final String id = item.getString(JsonKeys.ID);

                if (id != null) {
                    existingIDs.add(id);
                } // else, be forgiving and skip
            } // else, be forgiving and skip
        });

        for (int modIndex = 0; modIndex < modItems.size(); modIndex++) {
            final JsonObject modItem = modItems.getJsonObject(modIndex);
            final String modID = modItem.getString(JsonKeys.ID);

            if (modID != null) {
                final JsonArray items = merged.getJsonArray(JsonKeys.ITEMS, new JsonArray());

                if (existingIDs.contains(modID)) {
                    for (int index = 0; index < items.size(); index++) {
                        final Object object = items.getValue(index);

                        if (object instanceof JsonObject) {
                            final JsonObject item = (JsonObject) object;
                            final String id = item.getString(JsonKeys.ID);

                            if (id != null && id.equals(modID)) {
                                items.set(index, modItem);
                            } // else, be forgiving and skip
                        } // else, be forgiving and skip
                    }
                } else {
                    if (!items.isEmpty()) {
                        items.add(modItem);
                    } else {
                        merged.put(JsonKeys.ITEMS, new JsonArray().add(modItem));
                    }
                }
            }
        }

        return merged;
    }
}
