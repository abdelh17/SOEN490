package com.quolance.quolance_api.entities.enums;

import java.util.Arrays;

public enum ProjectSortFields {

    ID("id"),
    TITLE("title"),
    EXPIRATION_DATE("expirationDate"),
    PROJECT_STATUS("projectStatus");

    private final String fieldName;

    ProjectSortFields(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public static boolean isValidField(String field) {
        return Arrays.stream(values())
                .map(ProjectSortFields::getFieldName)
                .anyMatch(name -> name.equals(field));
    }
}
