package com.nymph.converter.springboot.cli;

import com.nymph.converter.core.converter.DocumentConverter;
import com.nymph.converter.core.format.AgentCardConverter;
import com.nymph.converter.core.format.JsonSchemaConverter;
import com.nymph.converter.core.format.OpenApiConverter;
import com.nymph.converter.core.model.ApplicationMetadata;
import com.nymph.converter.springboot.extractor.MetadataExtractor;
import com.nymph.converter.springboot.extractor.SpringBootMetadataExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Command Line Interface for the Spring Boot Documentation Converter
 */
public class DocumentConverterCLI {
    
    private final MetadataExtractor metadataExtractor;
    private final Map<String, DocumentConverter> converters;
    
    public DocumentConverterCLI() {
        this.metadataExtractor = new SpringBootMetadataExtractor();
        this.converters = new HashMap<>();
        
        // Register converters
        converters.put("openapi-yaml", new OpenApiConverter(true));
        converters.put("openapi-json", new OpenApiConverter(false));
        converters.put("json-schema", new JsonSchemaConverter());
        converters.put("agentcard", new AgentCardConverter());
    }
    
    public static void main(String[] args) {
        DocumentConverterCLI cli = new DocumentConverterCLI();
        cli.run(args);
    }
    
    public void run(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }
        
        String projectPath = args[0];
        String format = args[1];
        String outputPath = args.length > 2 ? args[2] : null;
        
        try {
            // Validate project path
            Path project = Paths.get(projectPath);
            if (!metadataExtractor.isValidSpringBootProject(project)) {
                System.err.println("Error: Not a valid Spring Boot project: " + projectPath);
                System.exit(1);
            }
            
            // Validate format
            DocumentConverter converter = converters.get(format.toLowerCase());
            if (converter == null) {
                System.err.println("Error: Unsupported format: " + format);
                System.err.println("Supported formats: " + String.join(", ", converters.keySet()));
                System.exit(1);
            }
            
            // Extract metadata
            System.out.println("Extracting metadata from: " + projectPath);
            ApplicationMetadata metadata = metadataExtractor.extractMetadata(project);
            
            // Convert to target format
            System.out.println("Converting to format: " + format);
            String output = converter.convert(metadata);
            
            // Write output
            if (outputPath != null) {
                Path outputFilePath = Paths.get(outputPath);
                Files.writeString(outputFilePath, output);
                System.out.println("Output written to: " + outputPath);
            } else {
                System.out.println("\n" + "=".repeat(50));
                System.out.println("Generated " + format.toUpperCase() + " Documentation:");
                System.out.println("=".repeat(50));
                System.out.println(output);
            }
            
            // Print summary
            printSummary(metadata);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private void printUsage() {
        System.out.println("Spring Boot Documentation Converter");
        System.out.println();
        System.out.println("Usage: java -jar spring-boot-converter.jar <project-path> <format> [output-file]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  project-path  Path to the Spring Boot project directory");
        System.out.println("  format        Output format (openapi-yaml, openapi-json, json-schema, agentcard)");
        System.out.println("  output-file   Optional output file path (prints to console if not provided)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar spring-boot-converter.jar ./my-spring-app openapi-yaml");
        System.out.println("  java -jar spring-boot-converter.jar ./my-spring-app agentcard output.json");
        System.out.println("  java -jar spring-boot-converter.jar ./my-spring-app json-schema schema.json");
        System.out.println();
        System.out.println("Supported formats:");
        System.out.println("  openapi-yaml  OpenAPI 3.0 specification in YAML format");
        System.out.println("  openapi-json  OpenAPI 3.0 specification in JSON format");
        System.out.println("  json-schema   JSON Schema specification");
        System.out.println("  agentcard     AgentCard format (A2A/MCP compatible)");
    }
    
    private void printSummary(ApplicationMetadata metadata) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Extraction Summary:");
        System.out.println("=".repeat(50));
        System.out.println("Application: " + metadata.getName());
        System.out.println("Version: " + metadata.getVersion());
        System.out.println("Description: " + metadata.getDescription());
        
        if (metadata.getEndpoints() != null) {
            System.out.println("Endpoints found: " + metadata.getEndpoints().size());
            metadata.getEndpoints().forEach(endpoint -> 
                System.out.println("  " + endpoint.getMethod() + " " + endpoint.getPath())
            );
        }
        
        if (metadata.getModels() != null) {
            System.out.println("Models found: " + metadata.getModels().size());
            metadata.getModels().forEach(model -> 
                System.out.println("  " + model.getName() + " (" + 
                    (model.getProperties() != null ? model.getProperties().size() : 0) + " properties)")
            );
        }
        
        if (metadata.getConfigurations() != null) {
            System.out.println("Configuration properties found: " + metadata.getConfigurations().size());
        }
    }
}