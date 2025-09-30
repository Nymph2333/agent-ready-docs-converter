package com.nymph.converter.springboot.analyzer;

import com.nymph.converter.core.model.EndpointMetadata;
import com.nymph.converter.core.model.ParameterMetadata;
import com.nymph.converter.core.model.ResponseMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Analyzes Spring Boot controllers to extract endpoint metadata
 */
public class ControllerAnalyzer {
    
    private static final Pattern REQUEST_MAPPING_PATTERN = 
        Pattern.compile("@(?:Request|Get|Post|Put|Delete|Patch)Mapping\\s*\\(\\s*(?:value\\s*=\\s*)?[\"']([^\"']*)[\"']");
    
    private static final Pattern METHOD_PATTERN = 
        Pattern.compile("@(Get|Post|Put|Delete|Patch)Mapping");
    
    private static final Pattern CLASS_REQUEST_MAPPING_PATTERN = 
        Pattern.compile("@RequestMapping\\s*\\(\\s*(?:value\\s*=\\s*)?[\"']([^\"']*)[\"']");
    
    private static final Pattern METHOD_SIGNATURE_PATTERN = 
        Pattern.compile("public\\s+\\w+\\s+(\\w+)\\s*\\(([^)]*)\\)");
    
    private static final Pattern PARAMETER_PATTERN = 
        Pattern.compile("@(PathVariable|RequestParam|RequestBody)(?:\\s*\\([^)]*\\))?\\s+\\w+\\s+(\\w+)");
    
    public List<EndpointMetadata> analyzeControllers(Path projectPath) {
        List<EndpointMetadata> endpoints = new ArrayList<>();
        
        Path srcPath = projectPath.resolve("src/main/java");
        if (!Files.exists(srcPath)) {
            System.out.println("DEBUG: src/main/java does not exist at: " + srcPath);
            return endpoints;
        }
        
        System.out.println("DEBUG: Analyzing controllers in: " + srcPath);
        
        try (Stream<Path> paths = Files.walk(srcPath)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                 .forEach(javaFile -> {
                     System.out.println("DEBUG: Found Java file: " + javaFile);
                     if (isControllerFile(javaFile)) {
                         System.out.println("DEBUG: Identified as controller: " + javaFile);
                         List<EndpointMetadata> controllerEndpoints = analyzeController(javaFile);
                         System.out.println("DEBUG: Found " + controllerEndpoints.size() + " endpoints in " + javaFile);
                         endpoints.addAll(controllerEndpoints);
                     }
                 });
        } catch (IOException e) {
            System.err.println("Error analyzing controllers: " + e.getMessage());
        }
        
        System.out.println("DEBUG: Total endpoints found: " + endpoints.size());
        return endpoints;
    }
    
    private boolean isControllerFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            return content.contains("@Controller") || 
                   content.contains("@RestController") ||
                   content.contains("@RequestMapping");
        } catch (IOException e) {
            return false;
        }
    }
    
    private List<EndpointMetadata> analyzeController(Path controllerPath) {
        List<EndpointMetadata> endpoints = new ArrayList<>();
        
        try {
            String content = Files.readString(controllerPath);
            System.out.println("DEBUG: Controller content preview: " + content.substring(0, Math.min(200, content.length())) + "...");
            
            String classBasePath = extractClassBasePath(content);
            System.out.println("DEBUG: Class base path: " + classBasePath);
            
            // Find all method mappings
            Pattern methodPattern = Pattern.compile(
                "@(Get|Post|Put|Delete|Patch|Request)Mapping[^}]*?public\\s+\\w+\\s+(\\w+)\\s*\\(([^)]*)\\)[^{]*\\{",
                Pattern.DOTALL
            );
            
            Matcher matcher = methodPattern.matcher(content);
            System.out.println("DEBUG: Searching for mapping patterns...");
            
            while (matcher.find()) {
                System.out.println("DEBUG: Found mapping match: " + matcher.group(0));
                String annotationType = matcher.group(1);
                String methodName = matcher.group(2);
                String parameters = matcher.group(3);
                
                EndpointMetadata endpoint = createEndpoint(
                    matcher.group(0), annotationType, methodName, parameters, classBasePath
                );
                
                if (endpoint != null) {
                    endpoints.add(endpoint);
                    System.out.println("DEBUG: Created endpoint: " + endpoint.getMethod() + " " + endpoint.getPath());
                }
            }
            
            if (endpoints.isEmpty()) {
                System.out.println("DEBUG: No mapping patterns found. Trying simpler pattern...");
                // Try a simpler pattern
                Pattern simplePattern = Pattern.compile("@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)");
                Matcher simpleMatcher = simplePattern.matcher(content);
                while (simpleMatcher.find()) {
                    System.out.println("DEBUG: Found simple mapping: " + simpleMatcher.group(0));
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error reading controller file: " + controllerPath + " - " + e.getMessage());
        }
        
        return endpoints;
    }
    
    private String extractClassBasePath(String content) {
        Matcher matcher = CLASS_REQUEST_MAPPING_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
    
    private EndpointMetadata createEndpoint(String mappingBlock, String annotationType, 
                                          String methodName, String parameters, String classBasePath) {
        
        String path = extractPath(mappingBlock);
        String httpMethod = mapAnnotationToHttpMethod(annotationType);
        
        // Combine class base path with method path
        String fullPath = combinePaths(classBasePath, path);
        
        EndpointMetadata endpoint = new EndpointMetadata(fullPath, httpMethod, 
            "Generated from method: " + methodName);
        
        // Extract parameters
        List<ParameterMetadata> params = extractParameters(parameters);
        endpoint.setParameters(params);
        
        // Set default response
        Map<String, ResponseMetadata> responses = new HashMap<>();
        responses.put("200", new ResponseMetadata(200, "Successful response"));
        endpoint.setResponses(responses);
        
        return endpoint;
    }
    
    private String extractPath(String mappingBlock) {
        // Try to extract value from mapping annotation
        Pattern pathPattern = Pattern.compile("(?:value\\s*=\\s*)?[\"']([^\"']*)[\"']");
        Matcher matcher = pathPattern.matcher(mappingBlock);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "/";
    }
    
    private String mapAnnotationToHttpMethod(String annotationType) {
        return switch (annotationType.toLowerCase()) {
            case "get", "getmapping" -> "GET";
            case "post", "postmapping" -> "POST";
            case "put", "putmapping" -> "PUT";
            case "delete", "deletemapping" -> "DELETE";
            case "patch", "patchmapping" -> "PATCH";
            default -> "GET";
        };
    }
    
    private String combinePaths(String basePath, String methodPath) {
        if (basePath == null || basePath.isEmpty()) {
            return methodPath.startsWith("/") ? methodPath : "/" + methodPath;
        }
        
        if (methodPath == null || methodPath.isEmpty() || methodPath.equals("/")) {
            return basePath.startsWith("/") ? basePath : "/" + basePath;
        }
        
        String normalizedBase = basePath.startsWith("/") ? basePath : "/" + basePath;
        String normalizedMethod = methodPath.startsWith("/") ? methodPath : "/" + methodPath;
        
        if (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }
        
        return normalizedBase + normalizedMethod;
    }
    
    private List<ParameterMetadata> extractParameters(String parametersString) {
        List<ParameterMetadata> parameters = new ArrayList<>();
        
        if (parametersString == null || parametersString.trim().isEmpty()) {
            return parameters;
        }
        
        // Simple parameter extraction
        String[] params = parametersString.split(",");
        for (String param : params) {
            param = param.trim();
            
            ParameterMetadata paramMeta = parseParameter(param);
            if (paramMeta != null) {
                parameters.add(paramMeta);
            }
        }
        
        return parameters;
    }
    
    private ParameterMetadata parseParameter(String param) {
        // Extract annotation type and parameter name
        if (param.contains("@PathVariable")) {
            String name = extractParameterName(param);
            String type = extractParameterType(param);
            return new ParameterMetadata(name, type, "path", true);
        } else if (param.contains("@RequestParam")) {
            String name = extractParameterName(param);
            String type = extractParameterType(param);
            boolean required = !param.contains("required = false");
            return new ParameterMetadata(name, type, "query", required);
        } else if (param.contains("@RequestBody")) {
            String name = extractParameterName(param);
            String type = extractParameterType(param);
            return new ParameterMetadata(name, type, "body", true);
        }
        
        return null;
    }
    
    private String extractParameterName(String param) {
        // Extract the last word (parameter name)
        String[] words = param.trim().split("\\s+");
        return words.length > 0 ? words[words.length - 1] : "param";
    }
    
    private String extractParameterType(String param) {
        // Simple type extraction
        if (param.contains("String")) return "string";
        if (param.contains("Integer") || param.contains("int")) return "integer";
        if (param.contains("Long") || param.contains("long")) return "integer";
        if (param.contains("Boolean") || param.contains("boolean")) return "boolean";
        if (param.contains("Double") || param.contains("double")) return "number";
        if (param.contains("Float") || param.contains("float")) return "number";
        
        return "string"; // default
    }
}