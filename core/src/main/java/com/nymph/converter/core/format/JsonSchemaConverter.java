package com.nymph.converter.core.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nymph.converter.core.model.ApplicationMetadata;
import com.nymph.converter.core.model.ModelMetadata;
import com.nymph.converter.core.converter.DocumentConverter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts application metadata to JSON Schema format
 */
public class JsonSchemaConverter implements DocumentConverter {
    
    private final ObjectMapper objectMapper;
    
    public JsonSchemaConverter() {
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String convert(ApplicationMetadata metadata) {
        try {
            Map<String, Object> schema = createJsonSchema(metadata);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert to JSON Schema format", e);
        }
    }
    
    private Map<String, Object> createJsonSchema(ApplicationMetadata metadata) {
        Map<String, Object> schema = new LinkedHashMap<>();
        
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("$id", "https://example.com/" + (metadata.getName() != null ? metadata.getName() : "app") + ".schema.json");
        schema.put("title", metadata.getName() != null ? metadata.getName() : "Application Schema");
        schema.put("description", metadata.getDescription() != null ? metadata.getDescription() : "Generated schema from Spring Boot application");
        
        schema.put("type", "object");
        schema.put("properties", createProperties(metadata));
        
        if (metadata.getModels() != null && !metadata.getModels().isEmpty()) {
            schema.put("$defs", createDefinitions(metadata));
        }
        
        return schema;
    }
    
    private Map<String, Object> createProperties(ApplicationMetadata metadata) {
        Map<String, Object> properties = new LinkedHashMap<>();
        
        // Add endpoints as a property
        if (metadata.getEndpoints() != null && !metadata.getEndpoints().isEmpty()) {
            Map<String, Object> endpointsSchema = new LinkedHashMap<>();
            endpointsSchema.put("type", "array");
            endpointsSchema.put("description", "Available API endpoints");
            
            Map<String, Object> endpointSchema = new LinkedHashMap<>();
            endpointSchema.put("type", "object");
            endpointSchema.put("properties", createEndpointProperties());
            endpointsSchema.put("items", endpointSchema);
            
            properties.put("endpoints", endpointsSchema);
        }
        
        // Add configurations as a property
        if (metadata.getConfigurations() != null && !metadata.getConfigurations().isEmpty()) {
            Map<String, Object> configSchema = new LinkedHashMap<>();
            configSchema.put("type", "object");
            configSchema.put("description", "Application configuration properties");
            properties.put("configurations", configSchema);
        }
        
        return properties;
    }
    
    private Map<String, Object> createEndpointProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        
        Map<String, Object> pathProp = new LinkedHashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "Endpoint path");
        properties.put("path", pathProp);
        
        Map<String, Object> methodProp = new LinkedHashMap<>();
        methodProp.put("type", "string");
        methodProp.put("enum", new String[]{"GET", "POST", "PUT", "DELETE", "PATCH"});
        methodProp.put("description", "HTTP method");
        properties.put("method", methodProp);
        
        Map<String, Object> descProp = new LinkedHashMap<>();
        descProp.put("type", "string");
        descProp.put("description", "Endpoint description");
        properties.put("description", descProp);
        
        return properties;
    }
    
    private Map<String, Object> createDefinitions(ApplicationMetadata metadata) {
        Map<String, Object> definitions = new LinkedHashMap<>();
        
        metadata.getModels().forEach(model -> {
            Map<String, Object> modelSchema = createModelSchema(model);
            definitions.put(model.getName(), modelSchema);
        });
        
        return definitions;
    }
    
    private Map<String, Object> createModelSchema(ModelMetadata model) {
        Map<String, Object> schema = new LinkedHashMap<>();
        
        schema.put("type", "object");
        
        if (model.getDescription() != null) {
            schema.put("description", model.getDescription());
        }
        
        if (model.getProperties() != null && !model.getProperties().isEmpty()) {
            Map<String, Object> properties = new LinkedHashMap<>();
            
            model.getProperties().forEach((name, prop) -> {
                Map<String, Object> propSchema = new LinkedHashMap<>();
                propSchema.put("type", mapJavaTypeToJsonType(prop.getType()));
                
                if (prop.getDescription() != null) {
                    propSchema.put("description", prop.getDescription());
                }
                
                if (prop.getFormat() != null) {
                    propSchema.put("format", prop.getFormat());
                }
                
                if (prop.getExample() != null) {
                    propSchema.put("example", prop.getExample());
                }
                
                if (prop.getEnumValues() != null && prop.getEnumValues().length > 0) {
                    propSchema.put("enum", prop.getEnumValues());
                }
                
                properties.put(name, propSchema);
            });
            
            schema.put("properties", properties);
        }
        
        if (model.getRequired() != null && !model.getRequired().isEmpty()) {
            schema.put("required", model.getRequired());
        }
        
        if (model.getExample() != null) {
            schema.put("example", model.getExample());
        }
        
        return schema;
    }
    
    private String mapJavaTypeToJsonType(String javaType) {
        if (javaType == null) {
            return "string";
        }
        
        return switch (javaType.toLowerCase()) {
            case "string", "char", "character" -> "string";
            case "int", "integer", "long", "short", "byte" -> "integer";
            case "float", "double", "bigdecimal", "number" -> "number";
            case "boolean", "bool" -> "boolean";
            case "array", "list", "set" -> "array";
            case "object", "map" -> "object";
            default -> "string";
        };
    }
    
    @Override
    public String getFormatName() {
        return "json-schema";
    }
    
    @Override
    public String getFileExtension() {
        return "json";
    }
}