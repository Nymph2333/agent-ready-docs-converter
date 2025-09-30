package com.nymph.converter.springboot.extractor;

import com.nymph.converter.core.model.*;
import com.nymph.converter.springboot.analyzer.ControllerAnalyzer;
import com.nymph.converter.springboot.analyzer.ModelAnalyzer;
import com.nymph.converter.springboot.analyzer.ConfigurationAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Default implementation of MetadataExtractor for Spring Boot projects
 */
public class SpringBootMetadataExtractor implements MetadataExtractor {
    
    private final ControllerAnalyzer controllerAnalyzer;
    private final ModelAnalyzer modelAnalyzer;
    private final ConfigurationAnalyzer configurationAnalyzer;
    
    public SpringBootMetadataExtractor() {
        this.controllerAnalyzer = new ControllerAnalyzer();
        this.modelAnalyzer = new ModelAnalyzer();
        this.configurationAnalyzer = new ConfigurationAnalyzer();
    }
    
    @Override
    public ApplicationMetadata extractMetadata(Path projectPath) {
        if (!isValidSpringBootProject(projectPath)) {
            throw new IllegalArgumentException("Not a valid Spring Boot project: " + projectPath);
        }
        
        ApplicationMetadata metadata = new ApplicationMetadata();
        
        // Extract basic application info
        extractBasicInfo(projectPath, metadata);
        
        // Extract endpoints from controllers
        List<EndpointMetadata> endpoints = controllerAnalyzer.analyzeControllers(projectPath);
        metadata.setEndpoints(endpoints);
        
        // Extract models
        List<ModelMetadata> models = modelAnalyzer.analyzeModels(projectPath);
        metadata.setModels(models);
        
        // Extract configuration
        List<ConfigurationMetadata> configurations = configurationAnalyzer.analyzeConfiguration(projectPath);
        metadata.setConfigurations(configurations);
        
        // Set additional properties
        metadata.setProperties(new HashMap<>());
        
        return metadata;
    }
    
    @Override
    public boolean isValidSpringBootProject(Path projectPath) {
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return false;
        }
        
        // Check for common Spring Boot project indicators
        return Files.exists(projectPath.resolve("pom.xml")) ||
               Files.exists(projectPath.resolve("build.gradle")) ||
               Files.exists(projectPath.resolve("gradle.properties"));
    }
    
    private void extractBasicInfo(Path projectPath, ApplicationMetadata metadata) {
        // Try to extract from pom.xml first
        Path pomPath = projectPath.resolve("pom.xml");
        if (Files.exists(pomPath)) {
            extractFromPom(pomPath, metadata);
            return;
        }
        
        // Try to extract from build.gradle
        Path gradlePath = projectPath.resolve("build.gradle");
        if (Files.exists(gradlePath)) {
            extractFromGradle(gradlePath, metadata);
            return;
        }
        
        // Try to extract from application.properties
        Path propsPath = projectPath.resolve("src/main/resources/application.properties");
        if (Files.exists(propsPath)) {
            extractFromProperties(propsPath, metadata);
            return;
        }
        
        // Default values
        metadata.setName("Spring Boot Application");
        metadata.setVersion("1.0.0");
        metadata.setDescription("Spring Boot API Documentation");
    }
    
    private void extractFromPom(Path pomPath, ApplicationMetadata metadata) {
        try {
            String content = Files.readString(pomPath);
            
            // Simple regex extraction (could be improved with XML parsing)
            String name = extractXmlValue(content, "artifactId");
            String version = extractXmlValue(content, "version");
            String description = extractXmlValue(content, "description");
            
            metadata.setName(name != null ? name : "Spring Boot Application");
            metadata.setVersion(version != null ? version : "1.0.0");
            metadata.setDescription(description != null ? description : "Spring Boot API Documentation");
            
        } catch (IOException e) {
            System.err.println("Failed to read pom.xml: " + e.getMessage());
            setDefaultInfo(metadata);
        }
    }
    
    private void extractFromGradle(Path gradlePath, ApplicationMetadata metadata) {
        try {
            String content = Files.readString(gradlePath);
            
            // Simple extraction from gradle files
            String version = extractGradleProperty(content, "version");
            
            metadata.setName("Spring Boot Application");
            metadata.setVersion(version != null ? version : "1.0.0");
            metadata.setDescription("Spring Boot API Documentation");
            
        } catch (IOException e) {
            System.err.println("Failed to read build.gradle: " + e.getMessage());
            setDefaultInfo(metadata);
        }
    }
    
    private void extractFromProperties(Path propsPath, ApplicationMetadata metadata) {
        try {
            Properties props = new Properties();
            props.load(Files.newInputStream(propsPath));
            
            String name = props.getProperty("spring.application.name", "Spring Boot Application");
            String version = props.getProperty("info.app.version", "1.0.0");
            String description = props.getProperty("info.app.description", "Spring Boot API Documentation");
            
            metadata.setName(name);
            metadata.setVersion(version);
            metadata.setDescription(description);
            
        } catch (IOException e) {
            System.err.println("Failed to read application.properties: " + e.getMessage());
            setDefaultInfo(metadata);
        }
    }
    
    private void setDefaultInfo(ApplicationMetadata metadata) {
        metadata.setName("Spring Boot Application");
        metadata.setVersion("1.0.0");
        metadata.setDescription("Spring Boot API Documentation");
    }
    
    private String extractXmlValue(String content, String tagName) {
        String pattern = "<" + tagName + ">(.*?)</" + tagName + ">";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(content);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }
    
    private String extractGradleProperty(String content, String propertyName) {
        String pattern = propertyName + "\\s*[=:]\\s*['\"]([^'\"]*)['\"]";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(content);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }
}