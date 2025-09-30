package com.nymph.converter.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Represents metadata extracted from Spring Boot applications
 */
public class ApplicationMetadata {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("endpoints")
    private List<EndpointMetadata> endpoints;
    
    @JsonProperty("configurations")
    private List<ConfigurationMetadata> configurations;
    
    @JsonProperty("models")
    private List<ModelMetadata> models;
    
    @JsonProperty("properties")
    private Map<String, Object> properties;

    // Constructors
    public ApplicationMetadata() {}

    public ApplicationMetadata(String name, String version, String description) {
        this.name = name;
        this.version = version;
        this.description = description;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<EndpointMetadata> getEndpoints() { return endpoints; }
    public void setEndpoints(List<EndpointMetadata> endpoints) { this.endpoints = endpoints; }

    public List<ConfigurationMetadata> getConfigurations() { return configurations; }
    public void setConfigurations(List<ConfigurationMetadata> configurations) { this.configurations = configurations; }

    public List<ModelMetadata> getModels() { return models; }
    public void setModels(List<ModelMetadata> models) { this.models = models; }

    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
}