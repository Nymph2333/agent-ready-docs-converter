package com.nymph.converter.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents configuration metadata
 */
public class ConfigurationMetadata {
    
    @JsonProperty("key")
    private String key;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("defaultValue")
    private Object defaultValue;
    
    @JsonProperty("required")
    private boolean required;

    // Constructors
    public ConfigurationMetadata() {}

    public ConfigurationMetadata(String key, String type) {
        this.key = key;
        this.type = type;
    }

    // Getters and Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Object getDefaultValue() { return defaultValue; }
    public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
}