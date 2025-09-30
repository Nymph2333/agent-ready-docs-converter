package com.nymph.converter.springboot.analyzer;

import com.nymph.converter.core.model.ModelMetadata;
import com.nymph.converter.core.model.PropertyMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Analyzes Java classes to extract model metadata
 */
public class ModelAnalyzer {
    
    private static final Pattern CLASS_PATTERN = 
        Pattern.compile("(?:public\\s+)?class\\s+(\\w+)");
    
    private static final Pattern FIELD_PATTERN = 
        Pattern.compile("private\\s+(\\w+(?:<[^>]+>)?)\\s+(\\w+);");
    
    private static final Pattern GETTER_PATTERN = 
        Pattern.compile("public\\s+(\\w+(?:<[^>]+>)?)\\s+get(\\w+)\\s*\\(\\s*\\)");
    
    public List<ModelMetadata> analyzeModels(Path projectPath) {
        List<ModelMetadata> models = new ArrayList<>();
        
        Path srcPath = projectPath.resolve("src/main/java");
        if (!Files.exists(srcPath)) {
            return models;
        }
        
        try (Stream<Path> paths = Files.walk(srcPath)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                 .filter(this::isModelFile)
                 .forEach(modelPath -> {
                     ModelMetadata model = analyzeModel(modelPath);
                     if (model != null) {
                         models.add(model);
                     }
                 });
        } catch (IOException e) {
            System.err.println("Error analyzing models: " + e.getMessage());
        }
        
        return models;
    }
    
    private boolean isModelFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            String fileName = filePath.getFileName().toString();
            
            // Skip controller, service, config files
            if (fileName.contains("Controller") || 
                fileName.contains("Service") || 
                fileName.contains("Config") ||
                fileName.contains("Application")) {
                return false;
            }
            
            // Look for model indicators
            return content.contains("@Entity") || 
                   content.contains("@Document") ||
                   (content.contains("class") && 
                    (content.contains("private") || content.contains("public")) &&
                    !content.contains("@Controller") &&
                    !content.contains("@RestController") &&
                    !content.contains("@Service") &&
                    !content.contains("@Component"));
        } catch (IOException e) {
            return false;
        }
    }
    
    private ModelMetadata analyzeModel(Path modelPath) {
        try {
            String content = Files.readString(modelPath);
            String className = extractClassName(content);
            
            if (className == null) {
                return null;
            }
            
            ModelMetadata model = new ModelMetadata(className, "object");
            model.setDescription("Data model: " + className);
            
            // Extract properties
            Map<String, PropertyMetadata> properties = extractProperties(content);
            model.setProperties(properties);
            
            // Extract required fields (simple heuristic)
            List<String> required = extractRequiredFields(content, properties.keySet());
            model.setRequired(required);
            
            return model;
            
        } catch (IOException e) {
            System.err.println("Error reading model file: " + modelPath + " - " + e.getMessage());
            return null;
        }
    }
    
    private String extractClassName(String content) {
        Matcher matcher = CLASS_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private Map<String, PropertyMetadata> extractProperties(String content) {
        Map<String, PropertyMetadata> properties = new LinkedHashMap<>();
        
        // Extract from fields
        Matcher fieldMatcher = FIELD_PATTERN.matcher(content);
        while (fieldMatcher.find()) {
            String type = fieldMatcher.group(1);
            String name = fieldMatcher.group(2);
            
            PropertyMetadata property = new PropertyMetadata(mapJavaTypeToJsonType(type));
            property.setDescription("Property: " + name);
            properties.put(name, property);
        }
        
        // Extract from getters (in case fields are not private)
        Matcher getterMatcher = GETTER_PATTERN.matcher(content);
        while (getterMatcher.find()) {
            String type = getterMatcher.group(1);
            String methodName = getterMatcher.group(2);
            
            // Convert getter name to property name (e.g., getName -> name)
            String propertyName = Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
            
            if (!properties.containsKey(propertyName)) {
                PropertyMetadata property = new PropertyMetadata(mapJavaTypeToJsonType(type));
                property.setDescription("Property: " + propertyName);
                properties.put(propertyName, property);
            }
        }
        
        return properties;
    }
    
    private List<String> extractRequiredFields(String content, Set<String> propertyNames) {
        List<String> required = new ArrayList<>();
        
        // Simple heuristic: look for @NotNull, @NotEmpty annotations
        Pattern notNullPattern = Pattern.compile("@(?:NotNull|NotEmpty|NotBlank)\\s+[^;]*?\\s+(\\w+);");
        Matcher matcher = notNullPattern.matcher(content);
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            if (propertyNames.contains(fieldName)) {
                required.add(fieldName);
            }
        }
        
        // If no annotations found, consider id fields as required
        if (required.isEmpty()) {
            for (String propName : propertyNames) {
                if (propName.toLowerCase().equals("id") || propName.toLowerCase().contains("id")) {
                    required.add(propName);
                    break;
                }
            }
        }
        
        return required;
    }
    
    private String mapJavaTypeToJsonType(String javaType) {
        if (javaType == null) {
            return "string";
        }
        
        // Remove generics
        javaType = javaType.replaceAll("<.*>", "");
        
        return switch (javaType.toLowerCase()) {
            case "string", "char", "character" -> "string";
            case "int", "integer", "long", "short", "byte" -> "integer";
            case "float", "double", "bigdecimal", "number" -> "number";
            case "boolean", "bool" -> "boolean";
            case "list", "arraylist", "linkedlist", "set", "hashset" -> "array";
            case "map", "hashmap", "linkedhashmap" -> "object";
            case "date", "localdate", "localdatetime", "instant" -> "string";
            default -> "string";
        };
    }
}