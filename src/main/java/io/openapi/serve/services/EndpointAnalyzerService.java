package io.openapi.serve.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Service for analyzing endpoints from OpenAPI specifications using synchronous programming
 */
@Service
public class EndpointAnalyzerService {

    private static final Logger logger = LoggerFactory.getLogger(EndpointAnalyzerService.class);
    private final OpenApiSpecificationService openApiService;
    private final HttpRequestService httpRequestService;
    private final ObjectMapper objectMapper;

    public EndpointAnalyzerService(OpenApiSpecificationService openApiService, HttpRequestService httpRequestService) {
        this.openApiService = openApiService;
        this.httpRequestService = httpRequestService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Analyzes a URI from an OpenAPI specification to determine its parameters and body requirements
     * 
     * @param domainUrl The URL of the domain containing the OpenAPI specification
     * @param path The path to the API endpoint (e.g., "/pets/{petId}")
     * @param method The HTTP method (GET, POST, PUT, DELETE, etc.)
     * @return A structured analysis of the URI's parameters and body requirements with examples
     */
    @Tool(
            description = """
                Analyze a URI to determine its parameters and body requirements.
                Inputs:
                - domainUrl: The domain where the API is hosted (e.g., 'api.example.com')
                - path: The path to the API endpoint (e.g., '/pets/{petId}')
                - method: The HTTP method (GET, POST, PUT, DELETE, etc.)

                Returns a detailed analysis of the endpoint including:
                - Required parameters (path, query, header)
                - Request body schema (if needed)
                - Example values
                - Response information

                Examples:
                - analyzeEndpoint("petstore.swagger.io", "/v2/pet/{petId}", "GET")
                - analyzeEndpoint("api.github.com", "/repos/{owner}/{repo}", "GET")
                """)
    public String analyzeEndpoint(String domainUrl, String path, String method) {
        logger.info("Analyzing endpoint: {} {} {}", method, domainUrl, path);

        // Normalize the path and method
        final String normalizedPath = httpRequestService.normalizePath(path);
        final String upperCaseMethod = method.toUpperCase();
        final String lowerCaseMethod = upperCaseMethod.toLowerCase();

        try {
            // Fetch the OpenAPI specification and process it
            JsonNode openApiSpec = openApiService.fetchOpenApiSpecAsJson(domainUrl);

            // Extract path information from the OpenAPI spec
            JsonNode pathsNode = openApiSpec.get("paths");
            if (pathsNode == null) {
                return "Error: OpenAPI specification does not contain paths information.";
            }

            // Find the exact path or a matching path pattern
            String matchedPath = findMatchingPath(pathsNode, normalizedPath);
            if (matchedPath == null) {
                return "Error: Path '" + normalizedPath + "' not found in the OpenAPI specification.";
            }

            // Get the method node
            JsonNode pathNode = pathsNode.get(matchedPath);
            JsonNode methodNode = pathNode.get(lowerCaseMethod);

            if (methodNode == null) {
                return "Error: HTTP method '" + upperCaseMethod + "' not supported for path '" + matchedPath + "'.";
            }

            // Build a more user-friendly analysis with examples
            StringBuilder analysis = new StringBuilder();
            analysis.append("# Endpoint Analysis: ").append(upperCaseMethod).append(" ").append(matchedPath).append("\n\n");

            // Add operation summary and description
            if (methodNode.has("summary") || methodNode.has("description")) {
                analysis.append("## Overview\n\n");

                if (methodNode.has("summary")) {
                    analysis.append(methodNode.get("summary").asText()).append("\n\n");
                }

                if (methodNode.has("description")) {
                    analysis.append(methodNode.get("description").asText()).append("\n\n");
                }
            }

            // Extract parameters information
            JsonNode parametersNode = methodNode.get("parameters");
            if (parametersNode != null && parametersNode.isArray() && parametersNode.size() > 0) {
                analysis.append("## Required Parameters\n\n");

                // Group parameters by type for better organization
                Map<String, List<JsonNode>> paramsByType = new HashMap<>();

                for (JsonNode paramNode : parametersNode) {
                    if (paramNode.has("in")) {
                        String paramType = paramNode.get("in").asText();
                        paramsByType.computeIfAbsent(paramType, k -> new ArrayList<>()).add(paramNode);
                    }
                }

                // Process path parameters
                if (paramsByType.containsKey("path")) {
                    analysis.append("### Path Parameters\n\n");
                    for (JsonNode paramNode : paramsByType.get("path")) {
                        appendParameterInfo(analysis, paramNode, openApiSpec);
                    }
                    analysis.append("\n");
                }

                // Process query parameters
                if (paramsByType.containsKey("query")) {
                    analysis.append("### Query Parameters\n\n");
                    for (JsonNode paramNode : paramsByType.get("query")) {
                        appendParameterInfo(analysis, paramNode, openApiSpec);
                    }
                    analysis.append("\n");
                }

                // Process header parameters
                if (paramsByType.containsKey("header")) {
                    analysis.append("### Header Parameters\n\n");
                    for (JsonNode paramNode : paramsByType.get("header")) {
                        appendParameterInfo(analysis, paramNode, openApiSpec);
                    }
                    analysis.append("\n");
                }
            }

            // Extract request body information with examples
            if (methodNode.has("requestBody")) {
                analysis.append("## Request Body\n\n");
                JsonNode requestBodyNode = methodNode.get("requestBody");

                boolean required = requestBodyNode.has("required") ? requestBodyNode.get("required").asBoolean() : false;
                analysis.append("Required: ").append(required ? "Yes" : "No").append("\n\n");

                if (requestBodyNode.has("description")) {
                    analysis.append(requestBodyNode.get("description").asText()).append("\n\n");
                }

                if (requestBodyNode.has("content")) {
                    JsonNode contentNode = requestBodyNode.get("content");
                    Iterator<Map.Entry<String, JsonNode>> contentTypes = contentNode.fields();

                    while (contentTypes.hasNext()) {
                        Map.Entry<String, JsonNode> contentType = contentTypes.next();
                        analysis.append("### Content Type: ").append(contentType.getKey()).append("\n\n");

                        JsonNode schemaNode = contentType.getValue().get("schema");
                        if (schemaNode != null) {
                            analysis.append(extractSchemaInfo(schemaNode, openApiSpec, 0));

                            // Add example if available
                            if (contentType.getValue().has("example")) {
                                try {
                                    JsonNode exampleNode = contentType.getValue().get("example");
                                    analysis.append("#### Example:\n\n```json\n");
                                    analysis.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exampleNode));
                                    analysis.append("\n```\n\n");
                                } catch (Exception e) {
                                    logger.debug("Failed to format example: {}", e.getMessage());
                                }
                            } else {
                                // Generate example from schema
                                analysis.append("#### Example (Generated):\n\n```json\n");
                                analysis.append(generateExampleFromSchema(schemaNode, openApiSpec));
                                analysis.append("\n```\n\n");
                            }
                        }
                    }
                }
            }

            // Add a section for how to call this endpoint
            analysis.append("## How to Call This Endpoint\n\n");
            analysis.append("You can use the `callEndpoint` function to make a request to this endpoint:\n\n");

            // Create an example call based on the method and parameters
            analysis.append("```\ncallEndpoint(\n");
            analysis.append("  \"").append(domainUrl).append("\",\n");
            analysis.append("  \"").append(normalizedPath).append("\",\n");
            analysis.append("  \"").append(upperCaseMethod).append("\",\n");

            // Add example query parameters if applicable
            if (parametersNode != null && parametersNode.isArray()) {
                boolean hasQueryParams = false;
                for (JsonNode paramNode : parametersNode) {
                    if (paramNode.has("in") && "query".equals(paramNode.get("in").asText())) {
                        hasQueryParams = true;
                        break;
                    }
                }

                if (hasQueryParams) {
                    analysis.append("  // Example query parameters\n");
                    analysis.append("  {\n");
                    boolean first = true;
                    for (JsonNode paramNode : parametersNode) {
                        if (paramNode.has("in") && "query".equals(paramNode.get("in").asText())) {
                            if (!first) analysis.append(",\n");
                            String name = paramNode.has("name") ? paramNode.get("name").asText() : "param";
                            analysis.append("    \"").append(name).append("\": \"value\"");
                            first = false;
                        }
                    }
                    analysis.append("\n  },\n");
                } else {
                    analysis.append("  null, // No query parameters needed\n");
                }
            } else {
                analysis.append("  null, // No query parameters needed\n");
            }

            // Add example body if applicable
            if (methodNode.has("requestBody")) {
                analysis.append("  // Example request body\n");
                analysis.append("  {\n");
                analysis.append("    // Add request body properties based on the schema\n");
                analysis.append("  },\n");
            } else {
                analysis.append("  null, // No request body needed\n");
            }

            // Add example headers
            analysis.append("  // Example headers\n");
            analysis.append("  {\n");
            analysis.append("    \"Content-Type\": \"application/json\"\n");
            analysis.append("  }\n");
            analysis.append(")\n```\n\n");

            return analysis.toString();
        } catch (IllegalArgumentException e) {
            // For expected errors, just return the error message
            logger.warn("API analysis error: {}", e.getMessage());
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            // For unexpected errors, log the full stack trace
            String errorMessage = "Failed to analyze endpoint: " + e.getMessage();
            logger.error(errorMessage, e);
            return "Error: " + errorMessage;
        }
    }

    /**
     * Appends parameter information to the analysis
     * @param analysis The StringBuilder to append to
     * @param paramNode The parameter node from the OpenAPI spec
     * @param openApiSpec The full OpenAPI specification
     */
    private void appendParameterInfo(StringBuilder analysis, JsonNode paramNode, JsonNode openApiSpec) {
        // Extract basic parameter information
        String name = paramNode.has("name") ? paramNode.get("name").asText() : "unknown";
        boolean required = paramNode.has("required") && paramNode.get("required").asBoolean();
        String type = extractParameterType(paramNode);
        String description = paramNode.has("description") ? paramNode.get("description").asText() : "";

        // Append parameter name, type and required status
        analysis.append("- **").append(name).append("** (")
               .append(type)
               .append(required ? ", **Required**" : ", Optional")
               .append(")\n");

        // Add description if available
        if (!description.isEmpty()) {
            analysis.append("  - ").append(description).append("\n");
        }

        // Find and add example value
        String example = findExampleValue(paramNode, type);
        if (example != null) {
            analysis.append("  - Example: `").append(example).append("`\n");
        }
    }

    /**
     * Finds an example value for a parameter
     * @param paramNode The parameter node
     * @param type The parameter type
     * @return An example value or null if none can be determined
     */
    private String findExampleValue(JsonNode paramNode, String type) {
        // Check for direct example
        if (paramNode.has("example")) {
            return paramNode.get("example").asText();
        }

        // Check for example in schema
        if (paramNode.has("schema") && paramNode.get("schema").has("example")) {
            return paramNode.get("schema").get("example").asText();
        }

        // Generate example based on type
        return generateExampleValue(type);
    }

    /**
     * Generates an example value based on parameter type
     */
    private String generateExampleValue(String type) {
        if (type.startsWith("integer") || type.startsWith("number")) {
            return "42";
        } else if (type.startsWith("boolean")) {
            return "true";
        } else if (type.startsWith("string")) {
            if (type.contains("date-time")) {
                return "2023-01-01T12:00:00Z";
            } else if (type.contains("date")) {
                return "2023-01-01";
            } else if (type.contains("uuid")) {
                return "123e4567-e89b-12d3-a456-426614174000";
            } else {
                return "example";
            }
        } else if (type.startsWith("array")) {
            return "[\"item1\", \"item2\"]";
        } else if (type.startsWith("object")) {
            return "{\"property\": \"value\"}";
        }
        return null;
    }

    /**
     * Generates an example JSON object from a schema
     * @param schemaNode The schema node to generate an example from
     * @param openApiSpec The full OpenAPI specification
     * @return A JSON string example
     */
    private String generateExampleFromSchema(JsonNode schemaNode, JsonNode openApiSpec) {
        try {
            // Handle reference schemas
            if (schemaNode.has("$ref")) {
                String ref = schemaNode.get("$ref").asText();
                JsonNode resolvedSchema = resolveReference(ref, openApiSpec);
                if (resolvedSchema != null) {
                    return generateExampleFromSchema(resolvedSchema, openApiSpec);
                }
                return "{ \"reference\": \"" + ref + "\" }";
            }

            // Handle typed schemas
            if (schemaNode.has("type")) {
                String type = schemaNode.get("type").asText();

                // Handle object type
                if ("object".equals(type) && schemaNode.has("properties")) {
                    ObjectNode example = objectMapper.createObjectNode();
                    JsonNode propertiesNode = schemaNode.get("properties");

                    propertiesNode.fields().forEachRemaining(property -> {
                        String propertyName = property.getKey();
                        JsonNode propertySchema = property.getValue();

                        // Use example if provided
                        if (propertySchema.has("example")) {
                            example.set(propertyName, propertySchema.get("example"));
                        } 
                        // Otherwise generate based on type
                        else if (propertySchema.has("type")) {
                            String propertyType = propertySchema.get("type").asText();

                            switch (propertyType) {
                                case "string":
                                    example.put(propertyName, "example_" + propertyName);
                                    break;
                                case "integer":
                                case "number":
                                    example.put(propertyName, 42);
                                    break;
                                case "boolean":
                                    example.put(propertyName, true);
                                    break;
                                case "array":
                                    example.set(propertyName, objectMapper.createArrayNode()
                                        .add("item1").add("item2"));
                                    break;
                                case "object":
                                    example.set(propertyName, objectMapper.createObjectNode()
                                        .put("nestedProperty", "value"));
                                    break;
                            }
                        }
                    });

                    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(example);
                }

                // Handle other types with simple examples
                switch (type) {
                    case "string":
                        return "\"example_string\"";
                    case "integer":
                    case "number":
                        return "42";
                    case "boolean":
                        return "true";
                    case "array":
                        return "[\"item1\", \"item2\"]";
                }
            }

            // Default empty object
            return "{ \"example\": \"value\" }";
        } catch (Exception e) {
            logger.debug("Failed to generate example: {}", e.getMessage());
            return "{ \"example\": \"value\" }";
        }
    }

    /**
     * Finds a matching path in the OpenAPI specification
     * @param pathsNode The paths node from the OpenAPI specification
     * @param path The path to find
     * @return The matching path or null if not found
     */
    private String findMatchingPath(JsonNode pathsNode, String path) {
        // First try exact match
        if (pathsNode.has(path)) {
            return path;
        }

        // Try to match path patterns (e.g., /pets/{petId} for /pets/123)
        Iterator<String> pathNames = pathsNode.fieldNames();
        while (pathNames.hasNext()) {
            String pathPattern = pathNames.next();
            if (pathsMatch(pathPattern, path)) {
                return pathPattern;
            }
        }

        return null;
    }

    /**
     * Checks if a path matches a path pattern
     * @param pattern The path pattern (e.g., /pets/{petId})
     * @param path The actual path (e.g., /pets/123)
     * @return True if the path matches the pattern
     */
    private boolean pathsMatch(String pattern, String path) {
        String[] patternParts = pattern.split("/");
        String[] pathParts = path.split("/");

        if (patternParts.length != pathParts.length) {
            return false;
        }

        for (int i = 0; i < patternParts.length; i++) {
            // If this is a path parameter (enclosed in {}), it matches any value
            boolean isPathParam = patternParts[i].startsWith("{") && patternParts[i].endsWith("}");

            // If not a path parameter, the parts must match exactly
            if (!isPathParam && !patternParts[i].equals(pathParts[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Extracts the type of a parameter
     * @param paramNode The parameter node
     * @return The parameter type
     */
    private String extractParameterType(JsonNode paramNode) {
        if (paramNode.has("schema")) {
            JsonNode schemaNode = paramNode.get("schema");
            return extractType(schemaNode);
        } else if (paramNode.has("type")) {
            return paramNode.get("type").asText();
        } else {
            return "unknown";
        }
    }

    /**
     * Extracts the type from a schema node
     * @param schemaNode The schema node
     * @return The type
     */
    private String extractType(JsonNode schemaNode) {
        if (schemaNode.has("type")) {
            String type = schemaNode.get("type").asText();

            if ("array".equals(type) && schemaNode.has("items")) {
                JsonNode itemsNode = schemaNode.get("items");
                return "array of " + extractType(itemsNode);
            } else if ("object".equals(type) && schemaNode.has("properties")) {
                return "object";
            } else {
                if (schemaNode.has("format")) {
                    return type + " (" + schemaNode.get("format").asText() + ")";
                } else {
                    return type;
                }
            }
        } else if (schemaNode.has("$ref")) {
            return schemaNode.get("$ref").asText().replaceAll(".*\\/", "");
        } else {
            return "unknown";
        }
    }

    /**
     * Extracts schema information recursively
     * @param schemaNode The schema node
     * @param openApiSpec The full OpenAPI specification
     * @param indentLevel The current indentation level
     * @return A string representation of the schema
     */
    private String extractSchemaInfo(JsonNode schemaNode, JsonNode openApiSpec, int indentLevel) {
        StringBuilder schema = new StringBuilder();
        String indent = "  ".repeat(indentLevel);

        // Handle reference schemas
        if (schemaNode.has("$ref")) {
            String ref = schemaNode.get("$ref").asText();
            String refName = ref.substring(ref.lastIndexOf('/') + 1);

            schema.append(indent).append("Schema: ").append(refName).append("\n\n");

            // Resolve and process the reference
            JsonNode resolvedSchema = resolveReference(ref, openApiSpec);
            if (resolvedSchema != null) {
                schema.append(extractSchemaInfo(resolvedSchema, openApiSpec, indentLevel));
            }
            return schema.toString();
        }

        // Handle typed schemas
        if (schemaNode.has("type")) {
            String type = schemaNode.get("type").asText();

            // Handle object type
            if ("object".equals(type)) {
                schema.append(indent).append("Type: object");

                if (!schemaNode.has("properties")) {
                    schema.append(" (no properties defined)\n\n");
                    return schema.toString();
                }

                schema.append("\n\n").append(indent).append("Properties:\n\n");
                JsonNode propertiesNode = schemaNode.get("properties");

                propertiesNode.fields().forEachRemaining(property -> {
                    String propertyName = property.getKey();
                    JsonNode propertySchema = property.getValue();

                    boolean required = isPropertyRequired(propertyName, schemaNode);
                    String propertyType = extractType(propertySchema);
                    String description = propertySchema.has("description") ? 
                                         propertySchema.get("description").asText() : "";

                    // Add property details
                    schema.append(indent)
                          .append("- **").append(propertyName).append("** (")
                          .append(propertyType).append(", Required: ")
                          .append(required ? "Yes" : "No").append(")\n");

                    // Add description if available
                    if (!description.isEmpty()) {
                        schema.append(indent)
                              .append("  - Description: ").append(description).append("\n");
                    }

                    // Add enum values if available
                    if (propertySchema.has("enum")) {
                        schema.append(indent).append("  - Enum values: ");
                        JsonNode enumNode = propertySchema.get("enum");

                        for (int i = 0; i < enumNode.size(); i++) {
                            schema.append(enumNode.get(i).asText());
                            if (i < enumNode.size() - 1) {
                                schema.append(", ");
                            }
                        }
                        schema.append("\n");
                    }

                    // Handle nested objects
                    if ("object".equals(propertyType) && propertySchema.has("properties")) {
                        schema.append("\n")
                              .append(extractSchemaInfo(propertySchema, openApiSpec, indentLevel + 1));
                    } 
                    // Handle arrays with complex items
                    else if ("array".equals(propertyType) && propertySchema.has("items")) {
                        JsonNode itemsSchema = propertySchema.get("items");
                        boolean isComplexItem = itemsSchema.has("$ref") || 
                                              (itemsSchema.has("type") && 
                                               "object".equals(itemsSchema.get("type").asText()));

                        if (isComplexItem) {
                            schema.append("\n").append(indent).append("  - Items:\n\n")
                                  .append(extractSchemaInfo(itemsSchema, openApiSpec, indentLevel + 2));
                        }
                    }
                });
            } 
            // Handle array type
            else if ("array".equals(type)) {
                schema.append(indent).append("Type: array\n\n");

                if (schemaNode.has("items")) {
                    schema.append(indent).append("Items:\n\n")
                          .append(extractSchemaInfo(schemaNode.get("items"), openApiSpec, indentLevel + 1));
                }
            } 
            // Handle primitive types
            else {
                schema.append(indent).append("Type: ").append(type);

                if (schemaNode.has("format")) {
                    schema.append(" (").append(schemaNode.get("format").asText()).append(")");
                }

                schema.append("\n\n");

                // Add enum values for primitive types
                if (schemaNode.has("enum")) {
                    schema.append(indent).append("Enum values: ");
                    JsonNode enumNode = schemaNode.get("enum");

                    for (int i = 0; i < enumNode.size(); i++) {
                        schema.append(enumNode.get(i).asText());
                        if (i < enumNode.size() - 1) {
                            schema.append(", ");
                        }
                    }
                    schema.append("\n\n");
                }
            }
        } 
        // Handle unknown schema type
        else {
            schema.append(indent).append("Schema: unknown\n\n");
        }

        return schema.toString();
    }

    /**
     * Resolves a JSON reference
     * @param ref The reference (e.g., #/components/schemas/Pet)
     * @param openApiSpec The full OpenAPI specification
     * @return The resolved schema node
     */
    private JsonNode resolveReference(String ref, JsonNode openApiSpec) {
        if (ref.startsWith("#/")) {
            String[] parts = ref.substring(2).split("/");
            JsonNode current = openApiSpec;

            for (String part : parts) {
                current = current.get(part);
                if (current == null) {
                    return null;
                }
            }

            return current;
        }

        return null;
    }

    /**
     * Checks if a property is required
     * @param propertyName The property name
     * @param schemaNode The schema node
     * @return True if the property is required
     */
    private boolean isPropertyRequired(String propertyName, JsonNode schemaNode) {
        if (schemaNode.has("required") && schemaNode.get("required").isArray()) {
            ArrayNode requiredNode = (ArrayNode) schemaNode.get("required");
            for (JsonNode node : requiredNode) {
                if (node.isTextual() && node.asText().equals(propertyName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Analyzes an endpoint and makes a request to it in one operation
     * 
     * @param domainUrl The domain URL
     * @param path The API path
     * @param method The HTTP method
     * @param queryParams Query parameters as a JSON string
     * @param body Request body as a JSON string
     * @param headers Headers as a JSON string
     * @return The analysis and response combined
     */
    @Tool(
            description = """
                Analyze an endpoint and make a request to it in one operation.
                Inputs:
                - domainUrl: The domain where the API is hosted (e.g., 'api.example.com')
                - path: The path to the API endpoint (e.g., '/pets/{petId}')
                - method: The HTTP method (GET, POST, PUT, DELETE, etc.)
                - queryParams: (Optional) Query parameters as a JSON string
                - body: (Optional) Request body as a JSON string
                - headers: (Optional) Headers as a JSON string

                Returns both the endpoint analysis and the API response.

                Example: analyzeAndCall("petstore.swagger.io", "/v2/pet/1", "GET", null, null, {"api_key": "special-key"})
                """)
    public String analyzeAndCall(String domainUrl, String path, String method, String queryParams, String body, String headers) {
        logger.info("Analyzing and calling endpoint: {} {} {}", method, domainUrl, path);

        try {
            // Normalize path and build full URL
            final String normalizedPath = httpRequestService.normalizePath(path);
            final String normalizedDomain = httpRequestService.normalizeUrl(domainUrl);
            final String fullUrl = httpRequestService.buildUrl(normalizedDomain, normalizedPath);

            // First analyze the endpoint
            String analysis = analyzeEndpoint(domainUrl, normalizedPath, method);

            // Then make the request
            String response = httpRequestService.makeRequest(fullUrl, method, queryParams, body, headers);

            // Combine results
            return "# Endpoint Analysis\n\n" + analysis + "\n\n# API Response\n\n" + response;
        } catch (IllegalArgumentException e) {
            // For expected errors, just return the error message
            logger.warn("API analysis and call error: {}", e.getMessage());
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            // For unexpected errors, log the full stack trace
            String errorMessage = "Failed to analyze and call endpoint: " + e.getMessage();
            logger.error(errorMessage, e);
            return "Error: " + errorMessage;
        }
    }
}
