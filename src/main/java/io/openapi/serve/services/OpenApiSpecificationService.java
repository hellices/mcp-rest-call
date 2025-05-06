package io.openapi.serve.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Service for discovering and fetching OpenAPI specifications from domains using synchronous programming
 */
@Service
public class OpenApiSpecificationService {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiSpecificationService.class);
    private final HttpRequestService httpRequestService;
    private final ObjectMapper objectMapper;

    // Common paths for OpenAPI specifications
    private static final String[] COMMON_OPENAPI_PATHS = {
        "/openapi.json",
        "/swagger.json",
        "/api-docs",
        "/v3/api-docs",
        "/swagger/v1/swagger.json",
        "/api/v3/api-docs",
        "/api/swagger.json"
    };

    public OpenApiSpecificationService(HttpRequestService httpRequestService) {
        this.httpRequestService = httpRequestService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Helper method to parse a JSON string into a JsonNode
     * @param jsonString The JSON string to parse
     * @return The parsed JsonNode
     * @throws IOException if parsing fails
     */
    private JsonNode parseJsonString(String jsonString) throws IOException {
        try {
            return objectMapper.readTree(jsonString);
        } catch (IOException e) {
            logger.error("Failed to parse JSON: {}", e.getMessage());
            throw new IOException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches OpenAPI specification from a given URL
     * @param url The URL to fetch the OpenAPI specification from
     * @return The OpenAPI specification as a String
     * @throws IOException if fetching fails
     */
    private String fetchSpec(String url) throws IOException {
        logger.info("Trying URL: {}", url);

        String response = httpRequestService.makeGetRequest(url, null, null);

        // Check if the response contains an error
        if (response.startsWith("Error:") || response.startsWith("Status Code: 4") || response.startsWith("Status Code: 5")) {
            throw new IOException("Failed to fetch from " + url + ": " + response);
        }

        // Extract the body from the response
        int bodyIndex = response.indexOf("Response Body:");
        if (bodyIndex != -1) {
            String body = response.substring(bodyIndex + "Response Body:".length()).trim();
            if (!body.isEmpty() && !body.equals("<empty>")) {
                logger.info("Successfully fetched OpenAPI specification from: {}", url);
                return body;
            }
        }

        throw new IOException("Empty or invalid response received from " + url);
    }

    /**
     * Discovers and fetches the REST API specification from a domain using OpenAPI
     * 
     * @param domainUrl The URL of the domain to fetch the OpenAPI specification from
     * @return The OpenAPI specification as a formatted string with information about available endpoints
     */
    @Tool(
            description = """
                Discover and fetch the REST API specification from a domain using OpenAPI.
                Inputs:
                - domainUrl: A domain like 'example.com' (with or without protocol)

                Returns the OpenAPI specification with information about available endpoints, methods, and schemas.

                Examples:
                - getApiSpecification("api.example.com")
                - getApiSpecification("petstore.swagger.io")
                """)
    public String getApiSpecification(String domainUrl) {
        logger.info("Fetching API specification from: {}", domainUrl);
        domainUrl = httpRequestService.normalizeUrl(domainUrl);

        final String normalizedDomainUrl = domainUrl;

        // Try common paths
        final List<String> errors = new ArrayList<>();
        String spec = null;

        // Try each path until we find a valid specification
        for (String path : COMMON_OPENAPI_PATHS) {
            String url = httpRequestService.buildUrl(normalizedDomainUrl, path);
            try {
                spec = fetchSpec(url);
                // If we get here, we found a valid spec
                break;
            } catch (IOException e) {
                logger.debug("Failed to fetch from {}: {}", url, e.getMessage());
                errors.add(e.getMessage());
            }
        }

        // If we couldn't find a spec, return an error
        if (spec == null) {
            String errorMessage = "Could not find OpenAPI specification at " + normalizedDomainUrl + 
                                 ". Tried the following paths: " + String.join(", ", COMMON_OPENAPI_PATHS) +
                                 ". Errors: " + String.join("; ", errors);
            logger.error(errorMessage);
            return "Error: " + errorMessage;
        }

        // If we have an error message, return it
        if (spec.startsWith("Error:")) {
            return spec;
        }

        try {
            // Parse the JSON spec
            JsonNode specJson = parseJsonString(spec);

            // Format the specification for better readability
            StringBuilder formattedSpec = new StringBuilder();
            formattedSpec.append("# API Specification for ").append(normalizedDomainUrl).append("\n\n");

            // Add info section if available
            if (specJson.has("info")) {
                JsonNode info = specJson.get("info");
                if (info.has("title")) {
                    formattedSpec.append("## ").append(info.get("title").asText()).append("\n\n");
                }
                if (info.has("description")) {
                    formattedSpec.append(info.get("description").asText()).append("\n\n");
                }
                if (info.has("version")) {
                    formattedSpec.append("API Version: ").append(info.get("version").asText()).append("\n\n");
                }
            }

            // Add paths section
            if (specJson.has("paths")) {
                formattedSpec.append("## Available Endpoints\n\n");
                JsonNode paths = specJson.get("paths");
                Iterator<Map.Entry<String, JsonNode>> pathsIter = paths.fields();

                while (pathsIter.hasNext()) {
                    Map.Entry<String, JsonNode> pathEntry = pathsIter.next();
                    String path = pathEntry.getKey();
                    JsonNode pathItem = pathEntry.getValue();

                    formattedSpec.append("### ").append(path).append("\n\n");

                    Iterator<Map.Entry<String, JsonNode>> methodsIter = pathItem.fields();
                    while (methodsIter.hasNext()) {
                        Map.Entry<String, JsonNode> methodEntry = methodsIter.next();
                        String method = methodEntry.getKey().toUpperCase();
                        JsonNode operation = methodEntry.getValue();

                        formattedSpec.append("#### ").append(method).append("\n\n");

                        if (operation.has("summary")) {
                            formattedSpec.append(operation.get("summary").asText()).append("\n\n");
                        }

                        if (operation.has("description")) {
                            formattedSpec.append(operation.get("description").asText()).append("\n\n");
                        }

                        // Add a note about using analyzeUri for more details
                        formattedSpec.append("*For detailed parameter and response information, use the analyzeEndpoint function with this path.*\n\n");
                    }
                }
            }

            return formattedSpec.toString();
        } catch (IOException e) {
            String errorMessage = "Failed to parse API specification: " + e.getMessage();
            logger.error(errorMessage, e);
            return "Error: " + errorMessage;
        } catch (Exception e) {
            String errorMessage = "Failed to fetch API specification: " + e.getMessage();
            logger.error(errorMessage, e);
            return "Error: " + errorMessage;
        }
    }

    /**
     * Helper method to fetch OpenAPI specification as JsonNode
     * @param domainUrl The URL of the domain to fetch the OpenAPI specification from
     * @return The OpenAPI specification as a JsonNode
     * @throws IOException if fetching or parsing fails
     */
    public JsonNode fetchOpenApiSpecAsJson(String domainUrl) throws IOException {
        logger.info("Fetching OpenAPI specification as JSON from: {}", domainUrl);

        final List<String> errors = new ArrayList<>();
        final String normalizedDomainUrl = httpRequestService.normalizeUrl(domainUrl);

        // Try each common path
        for (String path : COMMON_OPENAPI_PATHS) {
            String url = httpRequestService.buildUrl(normalizedDomainUrl, path);
            try {
                String spec = fetchSpec(url);
                return parseJsonString(spec);
            } catch (IOException e) {
                logger.debug("Failed to fetch from {}: {}", url, e.getMessage());
                errors.add(e.getMessage());
            }
        }

        // If we get here, we couldn't find a spec
        String errorMessage = "Could not find OpenAPI specification at " + normalizedDomainUrl + 
                             ". Tried the following paths: " + String.join(", ", COMMON_OPENAPI_PATHS) +
                             ". Errors: " + String.join("; ", errors);
        logger.error(errorMessage);
        throw new IOException(errorMessage);
    }
}
