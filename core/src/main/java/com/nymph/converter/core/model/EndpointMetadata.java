package com.nymph.converter.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Represents REST endpoint metadata
 */
public class EndpointMetadata {
    
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("parameters")
    private List<ParameterMetadata> parameters;
    
    @JsonProperty("requestBody")
    private ModelMetadata requestBody;
    
    @JsonProperty("responses")
    private Map<String, ResponseMetadata> responses;
    
    @JsonProperty("tags")
    private List<String> tags;

    // Constructors
    public EndpointMetadata() {}

    public EndpointMetadata(String path, String method, String description) {
        this.path = path;
        this.method = method;
        this.description = description;
    }

    // Getters and Setters
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<ParameterMetadata> getParameters() { return parameters; }
    public void setParameters(List<ParameterMetadata> parameters) { this.parameters = parameters; }

    public ModelMetadata getRequestBody() { return requestBody; }
    public void setRequestBody(ModelMetadata requestBody) { this.requestBody = requestBody; }

    public Map<String, ResponseMetadata> getResponses() { return responses; }
    public void setResponses(Map<String, ResponseMetadata> responses) { this.responses = responses; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}