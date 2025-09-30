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
            return endpoints;
        }
        
        try (Stream<Path> paths = Files.walk(srcPath)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                 .forEach(javaFile -> {
                     if (isControllerFile(javaFile)) {
                         List<EndpointMetadata> controllerEndpoints = analyzeController(javaFile);
                         endpoints.addAll(controllerEndpoints);
                     }
                 });
        } catch (IOException e) {
            System.err.println("Error analyzing controllers: " + e.getMessage());
        }
        
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
            String classBasePath = extractClassBasePath(content);
            
            // Split content into lines for easier processing
            String[] lines = content.split("\n");
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                
                // Look for mapping annotations
                if (line.matches(".*@(Get|Post|Put|Delete|Patch)Mapping.*")) {
                    String annotationType = extractAnnotationType(line);
                    String path = extractPathFromAnnotation(line);
                    
                    // Find the method declaration (usually next few lines)
                    String methodName = null;
                    String parameters = null;
                    
                    for (int j = i + 1; j < Math.min(i + 5, lines.length); j++) {
                        String methodLine = lines[j].trim();
                        if (methodLine.contains("public") && methodLine.contains("(")) {
                            methodName = extractMethodName(methodLine);
                            parameters = extractMethodParameters(methodLine, lines, j);
                            break;
                        }
                    }
                    
                    if (methodName != null) {
                        EndpointMetadata endpoint = createEndpointSimple(
                            annotationType, methodName, parameters, path, classBasePath
                        );
                        if (endpoint != null) {
                            endpoints.add(endpoint);
                        }
                    }
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

    
    private String extractAnnotationType(String line) {
        if (line.contains("@GetMapping")) return "Get";
        if (line.contains("@PostMapping")) return "Post";
        if (line.contains("@PutMapping")) return "Put";
        if (line.contains("@DeleteMapping")) return "Delete";
        if (line.contains("@PatchMapping")) return "Patch";
        return "Get"; // default
    }
    
    private String extractPathFromAnnotation(String line) {
        // Look for value in parentheses
        Pattern pathPattern = Pattern.compile("\\(\\s*[\"']([^\"']*)[\"']");
        Matcher matcher = pathPattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Look for value= parameter
        Pattern valuePattern = Pattern.compile("value\\s*=\\s*[\"']([^\"']*)[\"']");
        Matcher valueMatcher = valuePattern.matcher(line);
        if (valueMatcher.find()) {
            return valueMatcher.group(1);
        }
        
        return "/"; // default
    }
    
    private String extractMethodName(String methodLine) {
        // Look for method name after public Type
        Pattern methodPattern = Pattern.compile("public\\s+\\w+(?:<[^>]*>)?\\s+(\\w+)\\s*\\(");
        Matcher matcher = methodPattern.matcher(methodLine);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknownMethod";
    }
    
    private String extractMethodParameters(String methodLine, String[] lines, int startIndex) {
        // Simple parameter extraction - look for parameters between parentheses
        StringBuilder params = new StringBuilder();
        boolean foundStart = false;
        
        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i];
            int openParen = line.indexOf('(');
            int closeParen = line.indexOf(')');
            
            if (openParen != -1) {
                foundStart = true;
                params.append(line.substring(openParen + 1));
                if (closeParen > openParen) {
                    // Parameters end on same line
                    int endIndex = params.length() - (line.length() - closeParen);
                    return params.substring(0, endIndex);
                }
            } else if (foundStart) {
                params.append(line);
                if (closeParen != -1) {
                    // Parameters end on this line
                    int endIndex = params.length() - (line.length() - closeParen);
                    return params.substring(0, endIndex);
                }
            }
        }
        
        return params.toString();
    }
    
    private EndpointMetadata createEndpointSimple(String annotationType, String methodName, 
                                                String parameters, String path, String classBasePath) {
        String httpMethod = mapAnnotationToHttpMethod(annotationType);
        String fullPath = combinePaths(classBasePath, path);
        
        EndpointMetadata endpoint = new EndpointMetadata(fullPath, httpMethod, 
            "Generated from method: " + methodName);
        
        // Extract parameters
        if (parameters != null && !parameters.trim().isEmpty()) {
            List<ParameterMetadata> params = extractParametersSimple(parameters);
            endpoint.setParameters(params);
        }
        
        // Set default response
        Map<String, ResponseMetadata> responses = new HashMap<>();
        responses.put("200", new ResponseMetadata(200, "Successful response"));
        endpoint.setResponses(responses);
        
        return endpoint;
    }
    
    private List<ParameterMetadata> extractParametersSimple(String parametersString) {
        List<ParameterMetadata> parameters = new ArrayList<>();
        
        // Split by comma and analyze each parameter
        String[] params = parametersString.split(",");
        for (String param : params) {
            param = param.trim();
            
            if (param.contains("@PathVariable")) {
                String name = extractLastWord(param);
                String type = extractParameterType(param);
                parameters.add(new ParameterMetadata(name, type, "path", true));
            } else if (param.contains("@RequestParam")) {
                String name = extractLastWord(param);
                String type = extractParameterType(param);
                boolean required = !param.contains("required = false");
                parameters.add(new ParameterMetadata(name, type, "query", required));
            } else if (param.contains("@RequestBody")) {
                String name = extractLastWord(param);
                String type = extractParameterType(param);
                parameters.add(new ParameterMetadata(name, type, "body", true));
            }
        }
        
        return parameters;
    }
    
    private String extractLastWord(String param) {
        String[] words = param.trim().split("\\s+");
        return words.length > 0 ? words[words.length - 1] : "param";
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