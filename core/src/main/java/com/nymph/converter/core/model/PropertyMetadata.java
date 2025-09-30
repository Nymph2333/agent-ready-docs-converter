package com.nymph.converter.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents property metadata within models
 */
public class PropertyMetadata {
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("format")
    private String format;
    
    @JsonProperty("example")
    private Object example;
    
    @JsonProperty("enum")
    private Object[] enumValues;

    // Constructors
    public PropertyMetadata() {}

    public PropertyMetadata(String type) {
        this.type = type;
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Object getExample() { return example; }
    public void setExample(Object example) { this.example = example; }

    public Object[] getEnumValues() { return enumValues; }
    public void setEnumValues(Object[] enumValues) { this.enumValues = enumValues; }
}