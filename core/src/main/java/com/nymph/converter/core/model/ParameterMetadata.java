package com.nymph.converter.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents parameter metadata for endpoints
 */
public class ParameterMetadata {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("required")
    private boolean required;
    
    @JsonProperty("in")
    private String in; // query, path, header, body
    
    @JsonProperty("defaultValue")
    private String defaultValue;

    // Constructors
    public ParameterMetadata() {}

    public ParameterMetadata(String name, String type, String in, boolean required) {
        this.name = name;
        this.type = type;
        this.in = in;
        this.required = required;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public String getIn() { return in; }
    public void setIn(String in) { this.in = in; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
}