package com.nymph.converter.core.converter;

import com.nymph.converter.core.model.ApplicationMetadata;

/**
 * Interface for converting application metadata to different formats
 */
public interface DocumentConverter {
    
    /**
     * Converts application metadata to the target format
     * @param metadata the application metadata to convert
     * @return the converted document as a string
     */
    String convert(ApplicationMetadata metadata);
    
    /**
     * Gets the format name this converter produces
     * @return the format name (e.g., "openapi", "json-schema", "agentcard")
     */
    String getFormatName();
    
    /**
     * Gets the file extension for the output format
     * @return the file extension (e.g., "json", "yaml")
     */
    String getFileExtension();
}