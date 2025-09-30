package com.nymph.converter.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents response metadata for endpoints
 */
public class ResponseMetadata {
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("content")
    private ModelMetadata content;
    
    @JsonProperty("statusCode")
    private int statusCode;

    // Constructors
    public ResponseMetadata() {}

    public ResponseMetadata(int statusCode, String description) {
        this.statusCode = statusCode;
        this.description = description;
    }

    // Getters and Setters
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ModelMetadata getContent() { return content; }
    public void setContent(ModelMetadata content) { this.content = content; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
}