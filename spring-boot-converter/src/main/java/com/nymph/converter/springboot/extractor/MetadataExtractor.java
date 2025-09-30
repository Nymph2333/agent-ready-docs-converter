package com.nymph.converter.springboot.extractor;

import com.nymph.converter.core.model.ApplicationMetadata;

import java.nio.file.Path;

/**
 * Interface for extracting metadata from Spring Boot applications
 */
public interface MetadataExtractor {
    
    /**
     * Extracts metadata from a Spring Boot project
     * @param projectPath the root path of the Spring Boot project
     * @return extracted application metadata
     */
    ApplicationMetadata extractMetadata(Path projectPath);
    
    /**
     * Checks if the given path contains a valid Spring Boot project
     * @param projectPath the path to check
     * @return true if it's a valid Spring Boot project
     */
    boolean isValidSpringBootProject(Path projectPath);
}