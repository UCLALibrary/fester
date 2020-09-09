
package edu.ucla.library.iiif.fester;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

/**
 * The CSV file's headers.
 */
public class CsvHeaders {

    /**
     * The index position for the item ARK column.
     */
    private int myItemArkIndex = -1;

    /**
     * The index position for the parent ARK column.
     */
    private int myParentArkIndex = -1;

    /**
     * The index position for the title column.
     */
    private int myTitleIndex = -1;

    /**
     * The index position for the object type column.
     */
    private int myObjectTypeIndex = -1;

    /**
     * The index position for the file name column.
     */
    private int myFileNameIndex = -1;

    /**
     * The index position for the item sequence column.
     */
    private int myItemSequenceIndex = -1;

    /**
     * The index position for the viewingHint column.
     */
    private int myViewingHintIndex = -1;

    /**
     * The index position for the viewingDirection column.
     */
    private int myViewingDirectionIndex = -1;

    /**
     * The index position for the image access URL column.
     */
    private int myImageAccessUrlIndex = -1;

    /**
     * The index position for the repository name column.
     */
    private int myRepositoryNameIndex = -1;

    /**
     * The index position for the local rights statement column.
     */
    private int myLocalRightsStatementIndex = -1;

    /**
     * The index position for the rights contact column.
     */
    private int myRightsContactIndex = -1;

    /**
     * Create a new CSV headers object.
     *
     * @param aRow CSV header data
     */
    public CsvHeaders(final String[] aRow) {
        for (int index = 0; index < aRow.length; index++) {

            // Trim whitespace before attempting to match
            switch (aRow[index].trim()) {
                case CSV.TITLE:
                    setTitleIndex(index);
                    break;
                case CSV.ITEM_ARK:
                    setItemArkIndex(index);
                    break;
                case CSV.PARENT_ARK:
                    setParentArkIndex(index);
                    break;
                case CSV.OBJECT_TYPE:
                    setObjectTypeIndex(index);
                    break;
                case CSV.FILE_NAME:
                    setFileNameIndex(index);
                    break;
                case CSV.ITEM_SEQ:
                    setItemSequenceIndex(index);
                    break;
                case CSV.VIEWING_DIRECTION:
                    setViewingDirectionIndex(index);
                    break;
                case CSV.VIEWING_HINT:
                    setViewingHintIndex(index);
                    break;
                case CSV.IIIF_ACCESS_URL:
                    setImageAccessUrlIndex(index);
                    break;
                case CSV.REPOSITORY_NAME:
                    setRepositoryNameIndex(index);
                    break;
                case CSV.LOCAL_RIGHTS_STATEMENT:
                    setLocalRightsStatementIndex(index);
                    break;
                case CSV.RIGHTS_CONTACT:
                    setRightsContactIndex(index);
                    break;
                default:
                    // Our default is to ignore things we don't care about
            }
        }
    }

    /**
     * A constructor used by Jackson for deserialization.
     */
    @SuppressWarnings("unused")
    private CsvHeaders() {
    }

    /**
     * Gets the Item ARK index position.
     *
     * @return The Item ARK index position
     */
    @JsonGetter
    public int getItemArkIndex() {
        return myItemArkIndex;
    }

    /**
     * Sets the Item ARK index position.
     *
     * @param aItemArkIndex The position of the Item ARK header
     * @return This CSV headers
     */
    @JsonSetter
    public CsvHeaders setItemArkIndex(final int aItemArkIndex) {
        myItemArkIndex = aItemArkIndex;
        return this;
    }

    /**
     * Checks whether there is an Item ARK index position
     *
     * @return True if there is an Item ARK index position; else, false
     */
    public boolean hasItemArkIndex() {
        return myItemArkIndex != -1;
    }

    /**
     * Gets the image access URL index position.
     *
     * @return The image access URL index position
     */
    @JsonGetter
    public int getImageAccessUrlIndex() {
        return myImageAccessUrlIndex;
    }

    /**
     * Sets the image access URL index position.
     *
     * @param aImageAccessUrlIndex The position of the image access URL header
     * @return This CSV headers
     */
    @JsonSetter
    public CsvHeaders setImageAccessUrlIndex(final int aImageAccessUrlIndex) {
        myImageAccessUrlIndex = aImageAccessUrlIndex;
        return this;
    }

    /**
     * Checks whether there is an image access URL index position
     *
     * @return True if there is an image access URL index position; else, false
     */
    public boolean hasImageAccessUrlIndex() {
        return myImageAccessUrlIndex != -1;
    }

    /**
     * Gets the Parent ARK index position.
     *
     * @return The Parent ARK index position
     */
    @JsonGetter
    public int getParentArkIndex() {
        return myParentArkIndex;
    }

    /**
     * Sets the Parent ARK index position.
     *
     * @param aParentArkIndex The index position of the Parent ARK
     * @return This CSV headers
     */
    @JsonSetter
    public CsvHeaders setParentArkIndex(final int aParentArkIndex) {
        myParentArkIndex = aParentArkIndex;
        return this;
    }

    /**
     * Checks whether there is an Parent ARK index position
     *
     * @return True if there is an Parent ARK index position; else, false
     */
    public boolean hasParentArkIndex() {
        return myParentArkIndex != -1;
    }

    /**
     * Gets the Title index position.
     *
     * @return The Title index position
     */
    @JsonGetter
    public int getTitleIndex() {
        return myTitleIndex;
    }

    /**
     * Sets the Title index position.
     *
     * @param aTitleIndex The index position of the Title
     * @return This CSV headers
     */
    @JsonSetter
    public CsvHeaders setTitleIndex(final int aTitleIndex) {
        myTitleIndex = aTitleIndex;
        return this;
    }

    /**
     * Checks whether there is a Title index position
     *
     * @return True if there is an Title index position; else, false
     */
    public boolean hasTitleIndex() {
        return myTitleIndex != -1;
    }

    /**
     * Gets the Object Type index position.
     *
     * @return The index position of the Object Type
     */
    @JsonGetter
    public int getObjectTypeIndex() {
        return myObjectTypeIndex;
    }

    /**
     * Sets the Object Type index position.
     *
     * @param aObjectTypeIndex The index position of the Object Type
     * @return This CSV headers
     */
    @JsonSetter
    public CsvHeaders setObjectTypeIndex(final int aObjectTypeIndex) {
        myObjectTypeIndex = aObjectTypeIndex;
        return this;
    }

    /**
     * Checks whether there is an Object Type index position
     *
     * @return True if there is an Object Type index position; else, false
     */
    public boolean hasObjectTypeIndex() {
        return myObjectTypeIndex != -1;
    }

    /**
     * Gets the File Name index position.
     *
     * @return The index position of the File Name
     */
    @JsonGetter
    public int getFileNameIndex() {
        return myFileNameIndex;
    }

    /**
     * Sets the File Name index position.
     *
     * @param aFileNameIndex The index position of the File Name
     * @return This CSV headers
     */
    @JsonSetter
    public CsvHeaders setFileNameIndex(final int aFileNameIndex) {
        myFileNameIndex = aFileNameIndex;
        return this;
    }

    /**
     * Checks whether there is a File Name index position
     *
     * @return True if there is a File Name index position; else, false
     */
    public boolean hasFileNameIndex() {
        return myFileNameIndex != -1;
    }

    /**
     * Gets the Item Sequence header position.
     *
     * @return The item sequence header position
     */
    @JsonGetter
    public int getItemSequenceIndex() {
        return myItemSequenceIndex;
    }

    /**
     * Sets the Item Sequence index position.
     *
     * @param aItemSequenceIndex
     * @return This CSV headers
     */
    @JsonSetter
    public CsvHeaders setItemSequenceIndex(final int aItemSequenceIndex) {
        myItemSequenceIndex = aItemSequenceIndex;
        return this;
    }

    /**
     * Checks whether the CSV headers have a item sequence position registered.
     *
     * @return True if the headers have an item sequence position; else, false
     */
    public boolean hasItemSequenceIndex() {
        return myItemSequenceIndex != -1;
    }

    /**
     * Gets the viewingHint index position.
     *
     * @return The viewingHint index position
     */
    @JsonGetter
    public int getViewingHintIndex() {
        return myViewingHintIndex;
    }

    /**
     * Sets the viewingHint index position.
     *
     * @param aViewingHintIndex The position of the viewingHint header
     * @return This CSV headers
     */
    @JsonSetter
    public CsvHeaders setViewingHintIndex(final int aViewingHintIndex) {
        myViewingHintIndex = aViewingHintIndex;
        return this;
    }

    /**
     * Checks whether there is a viewingHint index position
     *
     * @return True if there is a viewingHint index position; else, false
     */
    public boolean hasViewingHintIndex() {
        return myViewingHintIndex != -1;
    }

    /**
     * Gets the viewingDirection index position.
     *
     * @return The viewingDirection index position
     */
    @JsonGetter
    public int getViewingDirectionIndex() {
        return myViewingDirectionIndex;
    }

    /**
     * Sets the viewingDirection index position.
     *
     * @param aViewingDirectionIndex The position of the viewingDirection header
     * @return This CSV headers
     */
    @JsonSetter
    public CsvHeaders setViewingDirectionIndex(final int aViewingDirectionIndex) {
        myViewingDirectionIndex = aViewingDirectionIndex;
        return this;
    }

    /**
     * Checks whether there is a viewingDirection index position
     *
     * @return True if there is a viewingDirection index position; else, false
     */
    public boolean hasViewingDirectionIndex() {
        return myViewingDirectionIndex != -1;
    }

    /**
     * Gets the repository name index position.
     *
     * @return The repository name index position
     */
    @JsonGetter
    public int getRepositoryNameIndex() {
        return myRepositoryNameIndex;
    }

    /**
     * Sets the repository name index position.
     *
     * @param aRepositoryNameIndex The position of the repository name header
     * @return This CSV headers
     */
    @JsonSetter
    public CsvHeaders setRepositoryNameIndex(final int aRepositoryNameIndex) {
        myRepositoryNameIndex = aRepositoryNameIndex;
        return this;
    }

    /**
     * Checks whether there is a repository name index position
     *
     * @return True if there is a repository name index position; else, false
     */
    public boolean hasRepositoryNameIndex() {
        return myRepositoryNameIndex != -1;
    }

    /**
     * Gets the local rights statement index position.
     *
     * @return The local rights statement index position
     */
    @JsonGetter
    public int getLocalRightsStatementIndex() {
        return myLocalRightsStatementIndex;
    }

    /**
     * Sets the local rights statement index position.
     *
     * @param aLocalRightsStatementIndex The position of the local rights statement header
     * @return This CSV headers
     */
    @JsonSetter
    public CsvHeaders setLocalRightsStatementIndex(final int aLocalRightsStatementIndex) {
        myLocalRightsStatementIndex = aLocalRightsStatementIndex;
        return this;
    }

    /**
     * Checks whether there is a local rights statement index position
     *
     * @return True if there is a local rights statement index position; else, false
     */
    public boolean hasLocalRightsStatementIndex() {
        return myLocalRightsStatementIndex != -1;
    }

    /**
     * Gets the rights contact index position.
     *
     * @return The rights contact index position
     */
    @JsonGetter
    public int getRightsContactIndex() {
        return myRightsContactIndex;
    }

    /**
     * Sets the rights contact index position.
     *
     * @param aRightsContactIndex The position of the rights contact header
     * @return This CSV headers
     */
    @JsonSetter
    public CsvHeaders setRightsContactIndex(final int aRightsContactIndex) {
        myRightsContactIndex = aRightsContactIndex;
        return this;
    }

    /**
     * Checks whether there is a rights contact index position
     *
     * @return True if there is a rights contact index position; else, false
     */
    public boolean hasRightsContactIndex() {
        return myRightsContactIndex != -1;
    }

    /**
     * Returns a JsonObject of the CsvHeaders.
     *
     * @return A JsonObject of the CsvHeaders
     */
    public JsonObject toJSON() {
        return JsonObject.mapFrom(this);
    }

    @Override
    public String toString() {
        return toJSON().encodePrettily();
    }

    /**
     * Returns a CsvHeaders from its JSON representation.
     *
     * @param aJsonObject A CSV headers object in JSON form
     * @return The CSV headers
     */
    @JsonIgnore
    public static CsvHeaders fromJSON(final JsonObject aJsonObject) {
        return Json.decodeValue(aJsonObject.toString(), CsvHeaders.class);
    }

    /**
     * Returns a CsvHeaders from its JSON representation.
     *
     * @param aJsonString A CSV headers object in string form
     * @return The CSV headers
     */
    @JsonIgnore
    public static CsvHeaders fromString(final String aJsonString) {
        return fromJSON(new JsonObject(aJsonString));
    }
}
