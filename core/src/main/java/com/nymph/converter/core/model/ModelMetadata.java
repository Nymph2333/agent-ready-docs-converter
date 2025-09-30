package com.nymph.converter.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Represents data model metadata
 */
public class ModelMetadata {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("properties")
    private Map<String, PropertyMetadata> properties;
    
    @JsonProperty("required")
    private List<String> required;
    
    @JsonProperty("example")
    private Object example;

    // Constructors
    public ModelMetadata() {}

    public ModelMetadata(String name, String type) {
        this.name = name;
        this.type = type;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, PropertyMetadata> getProperties() { return properties; }
    public void setProperties(Map<String, PropertyMetadata> properties) { this.properties = properties; }

    public List<String> getRequired() { return required; }
    public void setRequired(List<String> required) { this.required = required; }

    public Object getExample() { return example; }
    public void setExample(Object example) { this.example = example; }
}