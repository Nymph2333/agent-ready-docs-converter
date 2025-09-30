package com.nymph.converter.core.format;

import com.nymph.converter.core.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class OpenApiConverterTest {
    
    @Test
    public void testOpenApiConversion() {
        // Create test metadata
        ApplicationMetadata metadata = new ApplicationMetadata("Test App", "1.0.0", "Test Description");
        
        // Add test endpoint
        EndpointMetadata endpoint = new EndpointMetadata("/api/test", "GET", "Test endpoint");
        Map<String, ResponseMetadata> responses = new HashMap<>();
        responses.put("200", new ResponseMetadata(200, "Success"));
        endpoint.setResponses(responses);
        
        metadata.setEndpoints(Arrays.asList(endpoint));
        
        // Add test model
        ModelMetadata model = new ModelMetadata("TestModel", "object");
        Map<String, PropertyMetadata> properties = new HashMap<>();
        properties.put("id", new PropertyMetadata("integer"));
        properties.put("name", new PropertyMetadata("string"));
        model.setProperties(properties);
        model.setRequired(Arrays.asList("id"));
        
        metadata.setModels(Arrays.asList(model));
        
        // Test conversion
        OpenApiConverter converter = new OpenApiConverter();
        String result = converter.convert(metadata);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result.contains("openapi: \"3.0.3\""));
        assertTrue(result.contains("title: \"Test App\""));
        assertTrue(result.contains("/api/test:"));
        assertTrue(result.contains("TestModel:"));
        assertTrue(result.contains("type: \"object\""));
    }
    
    @Test
    public void testGetFormatName() {
        OpenApiConverter converter = new OpenApiConverter();
        assertEquals("openapi", converter.getFormatName());
    }
    
    @Test
    public void testGetFileExtension() {
        OpenApiConverter yamlConverter = new OpenApiConverter(true);
        assertEquals("yaml", yamlConverter.getFileExtension());
        
        OpenApiConverter jsonConverter = new OpenApiConverter(false);
        assertEquals("json", jsonConverter.getFileExtension());
    }
}