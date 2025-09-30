package com.nymph.converter.springboot.analyzer;

import com.nymph.converter.core.model.ConfigurationMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Analyzes Spring Boot configuration files to extract configuration metadata
 */
public class ConfigurationAnalyzer {
    
    public List<ConfigurationMetadata> analyzeConfiguration(Path projectPath) {
        List<ConfigurationMetadata> configurations = new ArrayList<>();
        
        // Analyze application.properties
        Path propsPath = projectPath.resolve("src/main/resources/application.properties");
        if (Files.exists(propsPath)) {
            configurations.addAll(analyzePropertiesFile(propsPath));
        }
        
        // Analyze application.yml
        Path ymlPath = projectPath.resolve("src/main/resources/application.yml");
        if (Files.exists(ymlPath)) {
            configurations.addAll(analyzeYamlFile(ymlPath));
        }
        
        // Analyze application.yaml
        Path yamlPath = projectPath.resolve("src/main/resources/application.yaml");
        if (Files.exists(yamlPath)) {
            configurations.addAll(analyzeYamlFile(yamlPath));
        }
        
        return configurations;
    }
    
    private List<ConfigurationMetadata> analyzePropertiesFile(Path propsPath) {
        List<ConfigurationMetadata> configurations = new ArrayList<>();
        
        try {
            Properties props = new Properties();
            props.load(Files.newInputStream(propsPath));
            
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                
                ConfigurationMetadata config = new ConfigurationMetadata(key, inferType(value));
                config.setDescription("Configuration property: " + key);
                config.setDefaultValue(value);
                config.setRequired(false);
                
                configurations.add(config);
            }
            
        } catch (IOException e) {
            System.err.println("Error reading properties file: " + propsPath + " - " + e.getMessage());
        }
        
        return configurations;
    }
    
    private List<ConfigurationMetadata> analyzeYamlFile(Path yamlPath) {
        List<ConfigurationMetadata> configurations = new ArrayList<>();
        
        try {
            List<String> lines = Files.readAllLines(yamlPath);
            Map<String, Object> flattenedProps = parseYamlToFlatMap(lines);
            
            for (Map.Entry<String, Object> entry : flattenedProps.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                ConfigurationMetadata config = new ConfigurationMetadata(key, inferType(value));
                config.setDescription("Configuration property: " + key);
                config.setDefaultValue(value);
                config.setRequired(false);
                
                configurations.add(config);
            }
            
        } catch (IOException e) {
            System.err.println("Error reading YAML file: " + yamlPath + " - " + e.getMessage());
        }
        
        return configurations;
    }
    
    private Map<String, Object> parseYamlToFlatMap(List<String> lines) {
        Map<String, Object> result = new LinkedHashMap<>();
        Stack<String> keyStack = new Stack<>();
        
        for (String line : lines) {
            line = line.trim();
            
            // Skip comments and empty lines
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // Calculate indentation level
            int indent = getIndentLevel(line);
            
            // Adjust key stack based on indentation
            while (keyStack.size() > indent / 2) {
                keyStack.pop();
            }
            
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                String key = parts[0].trim();
                String value = parts.length > 1 ? parts[1].trim() : "";
                
                // Build full key path
                String fullKey = keyStack.isEmpty() ? key : String.join(".", keyStack) + "." + key;
                
                if (!value.isEmpty()) {
                    // Leaf value
                    result.put(fullKey, parseValue(value));
                } else {
                    // Parent key
                    keyStack.push(key);
                }
            }
        }
        
        return result;
    }
    
    private int getIndentLevel(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }
    
    private Object parseValue(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }
        
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            // Return as string
            return value.replaceAll("^[\"']|[\"']$", ""); // Remove quotes
        }
    }
    
    private String inferType(Object value) {
        if (value == null) {
            return "string";
        }
        
        if (value instanceof Boolean) {
            return "boolean";
        }
        
        if (value instanceof Integer) {
            return "integer";
        }
        
        if (value instanceof Double || value instanceof Float) {
            return "number";
        }
        
        String stringValue = value.toString();
        
        // Try to infer from string content
        if (stringValue.equalsIgnoreCase("true") || stringValue.equalsIgnoreCase("false")) {
            return "boolean";
        }
        
        try {
            Integer.parseInt(stringValue);
            return "integer";
        } catch (NumberFormatException e) {
            // Not an integer
        }
        
        try {
            Double.parseDouble(stringValue);
            return "number";
        } catch (NumberFormatException e) {
            // Not a number
        }
        
        return "string";
    }
}