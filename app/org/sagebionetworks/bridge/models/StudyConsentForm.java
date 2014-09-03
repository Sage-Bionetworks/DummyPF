package org.sagebionetworks.bridge.models;

import static org.apache.commons.httpclient.HttpStatus.SC_BAD_REQUEST;

import org.sagebionetworks.bridge.exceptions.BridgeServiceException;

import com.fasterxml.jackson.databind.JsonNode;

public class StudyConsentForm {

    private final String path;
    private final int minAge;

    private static final String PATH = "path";
    private static final String MIN_AGE = "minAge";

    public StudyConsentForm(String path, int minAge) {
        this.path = path;
        this.minAge = minAge;
    }

    public static final StudyConsentForm fromJson(JsonNode json) {
        assertFieldsValid(json);
        return new StudyConsentForm(json.get(PATH).asText(), json.get(MIN_AGE).asInt());
    }

    public String getPath() {
        return path;
    }

    public int getMinAge() {
        return minAge;
    }

    private static void assertFieldsValid(JsonNode json) {
        if (json == null) {
            throw new BridgeServiceException("StudyConsentForm JSON is null.", SC_BAD_REQUEST);
        } else if (json.get("path") == null) {
            throw new BridgeServiceException("Path field is null.", SC_BAD_REQUEST);
        } else if (json.get("path").asText().isEmpty()) {
            throw new BridgeServiceException("Path field is empty.", SC_BAD_REQUEST);
        } else if (json.get("minAge") == null) {
            throw new BridgeServiceException("minAge field is null.", SC_BAD_REQUEST);
        } else if (json.get("minAge").asText().isEmpty()) {
            throw new BridgeServiceException("minAge field is empty.", SC_BAD_REQUEST);
        } else if (json.get("minAge").asInt() < 0) {
            throw new BridgeServiceException("minAge field must be greater than 0.", SC_BAD_REQUEST);
        }
    }
}
