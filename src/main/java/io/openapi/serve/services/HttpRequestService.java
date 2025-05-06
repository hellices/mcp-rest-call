package io.openapi.serve.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for executing HTTP requests to external APIs using synchronous RestTemplate
 */
@Service
public class HttpRequestService {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HttpRequestService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Makes an HTTP request to a URI with the specified method
     * @param url The full URL to make the request to
     * @param method The HTTP method to use (GET, POST, PUT, DELETE, PATCH)
     * @param queryParams Query parameters as a JSON string (e.g., {"param1": "value1", "param2": "value2"})
     * @param body Request body as a JSON string (for POST, PUT, PATCH)
     * @param headers Headers as a JSON string (e.g., {"Content-Type": "application/json", "Authorization": "Bearer token"})
     * @return The response with status code and body
     */
    @Tool(
            description = """
                Make an HTTP request to a URI.
                Inputs:
                - url: The full URL to make the request to (e.g., 'https://api.example.com/resource')
                - method: The HTTP method to use (GET, POST, PUT, DELETE, PATCH)
                - queryParams: (Optional) Query parameters as a JSON string (e.g., {"param1": "value1", "param2": "value2"})
                - body: (Optional) Request body as a JSON string (for POST, PUT, PATCH)
                - headers: (Optional) Headers as a JSON string (e.g., {"Content-Type": "application/json", "Authorization": "Bearer token"})
                Returns the response with status code and body.
                Example: makeRequest("https://api.example.com/resource", "GET", {"limit": "10"}, null, {"Authorization": "Bearer token"})
                """)
    public String makeRequest(String url, String method, String queryParams, String body, String headers) {
        HttpMethod httpMethod;
        try {
            httpMethod = HttpMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Error: Invalid HTTP method: " + method + ". Supported methods are GET, POST, PUT, DELETE, PATCH.";
        }

        return executeRequest(url, httpMethod, queryParams, body, headers);
    }

    /**
     * Makes a GET request to a URI
     * @param url The full URL to make the request to
     * @param queryParams Query parameters as a JSON string (e.g., {"param1": "value1", "param2": "value2"})
     * @param headers Headers as a JSON string (e.g., {"Content-Type": "application/json", "Authorization": "Bearer token"})
     * @return The response with status code and body
     */
    @Tool(
            description = """
                Make a GET request to a URI.
                Inputs:
                - url: The full URL to make the request to (e.g., 'https://api.example.com/resource')
                - queryParams: (Optional) Query parameters as a JSON string (e.g., {"param1": "value1", "param2": "value2"})
                - headers: (Optional) Headers as a JSON string (e.g., {"Content-Type": "application/json", "Authorization": "Bearer token"})
                Returns the response with status code and body.
                Example: makeGetRequest("https://api.example.com/resource", {"limit": "10"}, {"Authorization": "Bearer token"})
                """)
    public String makeGetRequest(String url, String queryParams, String headers) {
        return executeRequest(url, HttpMethod.GET, queryParams, null, headers);
    }

    /**
     * Makes a POST request to a URI
     * @param url The full URL to make the request to
     * @param queryParams Query parameters as a JSON string (e.g., {"param1": "value1", "param2": "value2"})
     * @param body Request body as a JSON string
     * @param headers Headers as a JSON string (e.g., {"Content-Type": "application/json", "Authorization": "Bearer token"})
     * @return The response with status code and body
     */
    @Tool(
            description = """
                Make a POST request to a URI.
                Inputs:
                - url: The full URL to make the request to (e.g., 'https://api.example.com/resource')
                - queryParams: (Optional) Query parameters as a JSON string (e.g., {"param1": "value1", "param2": "value2"})
                - body: (Optional) Request body as a JSON string
                - headers: (Optional) Headers as a JSON string (e.g., {"Content-Type": "application/json", "Authorization": "Bearer token"})
                Returns the response with status code and body.
                Example: makePostRequest("https://api.example.com/resource", {}, {"name": "John", "age": 30}, {"Authorization": "Bearer token"})
                """)
    public String makePostRequest(String url, String queryParams, String body, String headers) {
        return executeRequest(url, HttpMethod.POST, queryParams, body, headers);
    }

    /**
     * Executes an HTTP request with the specified parameters
     * @param url The full URL to make the request to
     * @param method The HTTP method to use
     * @param queryParamsJson Query parameters as a JSON string
     * @param bodyJson Request body as a JSON string
     * @param headersJson Headers as a JSON string
     * @return The response with status code and body
     */
    public String executeRequest(String url, HttpMethod method, String queryParamsJson, String bodyJson, String headersJson) {
        logger.info("Making {} request to: {}", method, url);

        try {
            // Parse query parameters
            Map<String, String> queryParams = parseJsonToMap(queryParamsJson);

            // Parse headers
            Map<String, String> headerMap = parseJsonToMap(headersJson);

            // Build URL with query parameters
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
            String urlWithParams = builder.build().toUriString();

            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            headers.set(HttpHeaders.USER_AGENT, "ApiTools/1.0");

            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                headers.set(entry.getKey(), entry.getValue());
            }

            // Create request entity with body if needed
            HttpEntity<?> requestEntity;
            if (bodyJson != null && !bodyJson.isEmpty() && 
                (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH)) {
                requestEntity = new HttpEntity<>(bodyJson, headers);
            } else {
                requestEntity = new HttpEntity<>(headers);
            }

            // Execute the request
            ResponseEntity<String> response = restTemplate.exchange(
                urlWithParams, 
                method, 
                requestEntity, 
                String.class
            );

            return formatResponse(response.getStatusCode().value(), response.getBody());
        } catch (HttpStatusCodeException e) {
            // Handle HTTP error responses
            return formatResponse(e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception e) {
            String errorMessage = "Failed to make request: " + e.getMessage();
            logger.error(errorMessage, e);
            return "Error: " + errorMessage;
        }
    }

    /**
     * Parses a JSON string to a Map
     * @param json The JSON string to parse
     * @return A Map of key-value pairs
     */
    private Map<String, String> parseJsonToMap(String json) {
        Map<String, String> map = new HashMap<>();

        if (json == null || json.isEmpty() || json.equals("{}") || json.equals("null")) {
            return map;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            jsonNode.fields().forEachRemaining(entry -> {
                map.put(entry.getKey(), entry.getValue().asText());
            });
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse JSON: {}", e.getMessage());
        }

        return map;
    }

    /**
     * Formats the response with status code and body
     * @param statusCode The HTTP status code
     * @param body The response body
     * @return A formatted response string
     */
    private String formatResponse(int statusCode, String body) {
        StringBuilder response = new StringBuilder();
        response.append("Status Code: ").append(statusCode).append("\n\n");

        if (body != null && !body.isEmpty()) {
            try {
                // Try to format JSON response
                JsonNode jsonNode = objectMapper.readTree(body);
                response.append("Response Body:\n").append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));
            } catch (JsonProcessingException e) {
                // Not JSON, return as plain text
                response.append("Response Body:\n").append(body);
            }
        } else {
            response.append("Response Body: <empty>");
        }

        return response.toString();
    }

    /**
     * Normalizes a URL to ensure it has a protocol
     * @param url The URL to normalize
     * @return The normalized URL
     */
    public String normalizeUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
            logger.info("Normalized URL to: {}", url);
        }
        return url;
    }

    /**
     * Builds a full URL from a domain and path
     * @param domain The domain
     * @param path The path
     * @return The full URL
     */
    public String buildUrl(String domain, String path) {
        // Ensure path starts with a slash
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return domain.endsWith("/") ? domain + path.substring(1) : domain + path;
    }

    /**
     * Normalizes a path by ensuring it has a leading slash and no trailing slash
     */
    public String normalizePath(String path) {
        // Add leading slash if missing
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        // Remove trailing slash if present (except for root path)
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }
}
