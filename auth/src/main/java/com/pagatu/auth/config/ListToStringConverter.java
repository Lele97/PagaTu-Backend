package com.pagatu.auth.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

/**
 * JPA Attribute Converter that converts between List<String> and String
 * for database storage. Converts a list of strings to a comma-separated string
 * for database storage and vice versa for entity attribute retrieval.
 */
@Converter
public class ListToStringConverter implements AttributeConverter<List<String>, String> {

    /**
     * Converts a List of Strings to a comma-separated String for database storage.
     *
     * @param attribute the entity attribute value to be converted
     * @return a comma-separated string representation of the list,
     *         or empty string if input is null
     */
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return attribute == null ? "" : String.join(",", attribute);
    }

    /**
     * Converts a comma-separated String from the database back to a List of Strings.
     *
     * @param dbData the data from the database column to be converted
     * @return a List of Strings parsed from the comma-separated input,
     *         or empty list if input is null or empty
     */
    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        return dbData == null || dbData.isEmpty() ? List.of() : List.of(dbData.split(","));
    }
}