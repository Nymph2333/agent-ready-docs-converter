package com.nymph.converter.core.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.nymph.converter.core.model.ApplicationMetadata;
import com.nymph.converter.core.converter.DocumentConverter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts application metadata to OpenAPI 3.0 specification
 */
public class OpenApiConverter implements DocumentConverter {
    
    private final ObjectMapper yamlMapper;
    private final ObjectMapper jsonMapper;
    private final boolean useYaml;
    
    public OpenApiConverter() {
        this(true);
    }
    
    public OpenApiConverter(boolean useYaml) {
        this.useYaml = useYaml;
        this.yamlMapper = new YAMLMapper();
        this.jsonMapper = new ObjectMapper();
    }
    
    @Override
    public String convert(ApplicationMetadata metadata) {
        try {
            Map<String, Object> openApiSpec = createOpenApiSpec(metadata);
            
            if (useYaml) {
                return yamlMapper.writeValueAsString(openApiSpec);
            } else {
                return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(openApiSpec);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert to OpenAPI format", e);
        }
    }
    
    private Map<String, Object> createOpenApiSpec(ApplicationMetadata metadata) {
        Map<String, Object> spec = new LinkedHashMap<>();
        
        spec.put("openapi", "3.0.3");
        spec.put("info", createInfo(metadata));
        spec.put("paths", createPaths(metadata));
        spec.put("components", createComponents(metadata));
        
        return spec;
    }
    
    private Map<String, Object> createInfo(ApplicationMetadata metadata) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", metadata.getName() != null ? metadata.getName() : "Spring Boot API");
        info.put("description", metadata.getDescription() != null ? metadata.getDescription() : "API Documentation");
        info.put("version", metadata.getVersion() != null ? metadata.getVersion() : "1.0.0");
        return info;
    }
    
    private Map<String, Object> createPaths(ApplicationMetadata metadata) {
        Map<String, Object> paths = new LinkedHashMap<>();
        
        if (metadata.getEndpoints() != null) {
            metadata.getEndpoints().forEach(endpoint -> {
                String path = endpoint.getPath();
                if (!paths.containsKey(path)) {
                    paths.put(path, new LinkedHashMap<>());
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> pathItem = (Map<String, Object>) paths.get(path);
                pathItem.put(endpoint.getMethod().toLowerCase(), createOperation(endpoint));
            });
        }
        
        return paths;
    }
    
    private Map<String, Object> createOperation(com.nymph.converter.core.model.EndpointMetadata endpoint) {
        Map<String, Object> operation = new LinkedHashMap<>();
        
        if (endpoint.getDescription() != null) {
            operation.put("description", endpoint.getDescription());
        }
        
        if (endpoint.getTags() != null && !endpoint.getTags().isEmpty()) {
            operation.put("tags", endpoint.getTags());
        }
        
        if (endpoint.getParameters() != null && !endpoint.getParameters().isEmpty()) {
            operation.put("parameters", endpoint.getParameters().stream()
                .map(this::createParameter)
                .toList());
        }
        
        if (endpoint.getResponses() != null && !endpoint.getResponses().isEmpty()) {
            operation.put("responses", createResponses(endpoint));
        } else {
            // Default response
            Map<String, Object> responses = new LinkedHashMap<>();
            Map<String, Object> defaultResponse = new LinkedHashMap<>();
            defaultResponse.put("description", "Successful response");
            responses.put("200", defaultResponse);
            operation.put("responses", responses);
        }
        
        return operation;
    }
    
    private Map<String, Object> createParameter(com.nymph.converter.core.model.ParameterMetadata param) {
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put("name", param.getName());
        parameter.put("in", param.getIn());
        parameter.put("required", param.isRequired());
        
        if (param.getDescription() != null) {
            parameter.put("description", param.getDescription());
        }
        
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", param.getType());
        parameter.put("schema", schema);
        
        return parameter;
    }
    
    private Map<String, Object> createResponses(com.nymph.converter.core.model.EndpointMetadata endpoint) {
        Map<String, Object> responses = new LinkedHashMap<>();
        
        endpoint.getResponses().forEach((code, response) -> {
            Map<String, Object> responseObj = new LinkedHashMap<>();
            responseObj.put("description", response.getDescription() != null ? response.getDescription() : "Response");
            responses.put(code, responseObj);
        });
        
        return responses;
    }
    
    private Map<String, Object> createComponents(ApplicationMetadata metadata) {
        Map<String, Object> components = new LinkedHashMap<>();
        
        if (metadata.getModels() != null && !metadata.getModels().isEmpty()) {
            Map<String, Object> schemas = new LinkedHashMap<>();
            
            metadata.getModels().forEach(model -> {
                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "object");
                
                if (model.getDescription() != null) {
                    schema.put("description", model.getDescription());
                }
                
                if (model.getProperties() != null && !model.getProperties().isEmpty()) {
                    Map<String, Object> properties = new LinkedHashMap<>();
                    model.getProperties().forEach((name, prop) -> {
                        Map<String, Object> propSchema = new LinkedHashMap<>();
                        propSchema.put("type", prop.getType());
                        if (prop.getDescription() != null) {
                            propSchema.put("description", prop.getDescription());
                        }
                        properties.put(name, propSchema);
                    });
                    schema.put("properties", properties);
                }
                
                if (model.getRequired() != null && !model.getRequired().isEmpty()) {
                    schema.put("required", model.getRequired());
                }
                
                schemas.put(model.getName(), schema);
            });
            
            components.put("schemas", schemas);
        }
        
        return components;
    }
    
    @Override
    public String getFormatName() {
        return "openapi";
    }
    
    @Override
    public String getFileExtension() {
        return useYaml ? "yaml" : "json";
    }
}