package com.nymph.converter.core.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nymph.converter.core.model.ApplicationMetadata;
import com.nymph.converter.core.converter.DocumentConverter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts application metadata to AgentCard format (A2A/MCP compatible)
 */
public class AgentCardConverter implements DocumentConverter {
    
    private final ObjectMapper objectMapper;
    
    public AgentCardConverter() {
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String convert(ApplicationMetadata metadata) {
        try {
            Map<String, Object> agentCard = createAgentCard(metadata);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(agentCard);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert to AgentCard format", e);
        }
    }
    
    private Map<String, Object> createAgentCard(ApplicationMetadata metadata) {
        Map<String, Object> card = new LinkedHashMap<>();
        
        // AgentCard header
        card.put("version", "1.0");
        card.put("type", "agent-card");
        
        // Application info
        Map<String, Object> application = new LinkedHashMap<>();
        application.put("name", metadata.getName() != null ? metadata.getName() : "Spring Boot Application");
        application.put("version", metadata.getVersion() != null ? metadata.getVersion() : "1.0.0");
        application.put("description", metadata.getDescription() != null ? metadata.getDescription() : "Spring Boot API");
        application.put("type", "rest-api");
        card.put("application", application);
        
        // Capabilities
        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("protocols", new String[]{"http", "https"});
        capabilities.put("formats", new String[]{"json", "xml"});
        
        if (metadata.getEndpoints() != null && !metadata.getEndpoints().isEmpty()) {
            capabilities.put("endpoints", metadata.getEndpoints().size());
            capabilities.put("methods", extractUniqueMethods(metadata));
        }
        
        card.put("capabilities", capabilities);
        
        // Tools/Functions (mapped from endpoints)
        if (metadata.getEndpoints() != null && !metadata.getEndpoints().isEmpty()) {
            card.put("tools", createTools(metadata));
        }
        
        // Resources (mapped from models)
        if (metadata.getModels() != null && !metadata.getModels().isEmpty()) {
            card.put("resources", createResources(metadata));
        }
        
        // Configuration
        if (metadata.getConfigurations() != null && !metadata.getConfigurations().isEmpty()) {
            card.put("configuration", createConfiguration(metadata));
        }
        
        // Metadata
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("generated", java.time.Instant.now().toString());
        meta.put("generator", "agent-ready-docs-converter");
        card.put("metadata", meta);
        
        return card;
    }
    
    private String[] extractUniqueMethods(ApplicationMetadata metadata) {
        return metadata.getEndpoints().stream()
            .map(endpoint -> endpoint.getMethod().toUpperCase())
            .distinct()
            .toArray(String[]::new);
    }
    
    private java.util.List<Map<String, Object>> createTools(ApplicationMetadata metadata) {
        java.util.List<Map<String, Object>> tools = new ArrayList<>();
        
        metadata.getEndpoints().forEach(endpoint -> {
            Map<String, Object> tool = new LinkedHashMap<>();
            
            // Generate tool name from path and method
            String toolName = generateToolName(endpoint.getPath(), endpoint.getMethod());
            tool.put("name", toolName);
            tool.put("description", endpoint.getDescription() != null ? 
                endpoint.getDescription() : "API endpoint " + endpoint.getMethod() + " " + endpoint.getPath());
            
            // Input schema
            Map<String, Object> inputSchema = new LinkedHashMap<>();
            inputSchema.put("type", "object");
            
            Map<String, Object> properties = new LinkedHashMap<>();
            
            if (endpoint.getParameters() != null && !endpoint.getParameters().isEmpty()) {
                endpoint.getParameters().forEach(param -> {
                    Map<String, Object> paramSchema = new LinkedHashMap<>();
                    paramSchema.put("type", mapToJsonType(param.getType()));
                    paramSchema.put("description", param.getDescription() != null ? 
                        param.getDescription() : "Parameter: " + param.getName());
                    
                    if (param.getDefaultValue() != null) {
                        paramSchema.put("default", param.getDefaultValue());
                    }
                    
                    properties.put(param.getName(), paramSchema);
                });
            }
            
            // Add path parameters
            if (endpoint.getPath().contains("{")) {
                extractPathParameters(endpoint.getPath()).forEach(pathParam -> {
                    if (!properties.containsKey(pathParam)) {
                        Map<String, Object> paramSchema = new LinkedHashMap<>();
                        paramSchema.put("type", "string");
                        paramSchema.put("description", "Path parameter: " + pathParam);
                        properties.put(pathParam, paramSchema);
                    }
                });
            }
            
            inputSchema.put("properties", properties);
            tool.put("inputSchema", inputSchema);
            
            // Endpoint metadata
            Map<String, Object> endpointMeta = new LinkedHashMap<>();
            endpointMeta.put("path", endpoint.getPath());
            endpointMeta.put("method", endpoint.getMethod().toUpperCase());
            
            if (endpoint.getTags() != null && !endpoint.getTags().isEmpty()) {
                endpointMeta.put("tags", endpoint.getTags());
            }
            
            tool.put("endpoint", endpointMeta);
            
            tools.add(tool);
        });
        
        return tools;
    }
    
    private String generateToolName(String path, String method) {
        // Convert REST path to camelCase tool name
        // e.g., GET /api/users/{id} -> getUserById
        
        String cleanPath = path.replaceAll("^/+", "").replaceAll("/+$", "");
        String[] parts = cleanPath.split("/");
        
        StringBuilder toolName = new StringBuilder(method.toLowerCase());
        
        for (String part : parts) {
            if (part.startsWith("{") && part.endsWith("}")) {
                // Path parameter
                String paramName = part.substring(1, part.length() - 1);
                toolName.append("By").append(capitalize(paramName));
            } else if (!part.equals("api")) {
                toolName.append(capitalize(part));
            }
        }
        
        return toolName.toString();
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private java.util.List<String> extractPathParameters(String path) {
        java.util.List<String> params = new ArrayList<>();
        int start = path.indexOf('{');
        while (start != -1) {
            int end = path.indexOf('}', start);
            if (end != -1) {
                params.add(path.substring(start + 1, end));
                start = path.indexOf('{', end);
            } else {
                break;
            }
        }
        return params;
    }
    
    private java.util.List<Map<String, Object>> createResources(ApplicationMetadata metadata) {
        java.util.List<Map<String, Object>> resources = new ArrayList<>();
        
        metadata.getModels().forEach(model -> {
            Map<String, Object> resource = new LinkedHashMap<>();
            resource.put("name", model.getName());
            resource.put("type", "data-model");
            resource.put("description", model.getDescription() != null ? 
                model.getDescription() : "Data model: " + model.getName());
            
            // Schema
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "object");
            
            if (model.getProperties() != null && !model.getProperties().isEmpty()) {
                Map<String, Object> properties = new LinkedHashMap<>();
                model.getProperties().forEach((name, prop) -> {
                    Map<String, Object> propSchema = new LinkedHashMap<>();
                    propSchema.put("type", mapToJsonType(prop.getType()));
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
            
            resource.put("schema", schema);
            resources.add(resource);
        });
        
        return resources;
    }
    
    private Map<String, Object> createConfiguration(ApplicationMetadata metadata) {
        Map<String, Object> config = new LinkedHashMap<>();
        
        metadata.getConfigurations().forEach(configItem -> {
            Map<String, Object> configSchema = new LinkedHashMap<>();
            configSchema.put("type", mapToJsonType(configItem.getType()));
            configSchema.put("description", configItem.getDescription() != null ? 
                configItem.getDescription() : "Configuration: " + configItem.getKey());
            configSchema.put("required", configItem.isRequired());
            
            if (configItem.getDefaultValue() != null) {
                configSchema.put("default", configItem.getDefaultValue());
            }
            
            config.put(configItem.getKey(), configSchema);
        });
        
        return config;
    }
    
    private String mapToJsonType(String javaType) {
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
        return "agentcard";
    }
    
    @Override
    public String getFileExtension() {
        return "json";
    }
}