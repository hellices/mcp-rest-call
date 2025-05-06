# Spring AI MCP OpenAPI Server Sample

This project is a Spring AI Model Context Protocol (MCP) server that provides tools for working with OpenAPI specifications. It enables AI models to interact with REST APIs by providing the following capabilities:

1. **OpenAPI Specification Collection**: Fetches and parses OpenAPI specifications from domains
2. **Endpoint Analysis**: Analyzes API endpoints to determine parameters and requirements
3. **HTTP Request Handling**: Makes API calls to external services

## Key Features

- Built with Spring Boot and Spring AI
- Implements the Model Context Protocol (MCP)
- Provides tools for AI models to discover and interact with REST APIs
- Uses reactive programming with Spring WebFlux

## Project Structure

The server is organized into three main services:

1. `OpenApiSpecificationService`: Discovers and fetches OpenAPI specifications
2. `EndpointAnalyzerService`: Analyzes API endpoints from specifications
3. `HttpRequestService`: Makes HTTP requests to external APIs

Each service exposes methods as tools using Spring AI's `@Tool` annotation, making them available to AI models through the MCP protocol.

## Available Tools

### OpenAPI Specification Tools

#### getApiSpecification
Discovers and fetches the REST API specification from a domain using OpenAPI.

**Parameters:**
- `domainUrl`: A domain like 'example.com' (with or without protocol)

**Returns:** The OpenAPI specification with information about available endpoints, methods, and schemas.


### Endpoint Analyzer Tools

#### analyzeEndpoint
Analyzes a URI from an OpenAPI specification to determine its parameters and body requirements.

**Parameters:**
- `domainUrl`: The domain where the API is hosted (e.g., 'api.example.com')
- `path`: The path to the API endpoint (e.g., '/pets/{petId}')
- `method`: The HTTP method (GET, POST, PUT, DELETE, etc.)

**Returns:** Detailed information about the endpoint, including parameters, request body schema, and response schema.


#### analyzeAndCall
Analyzes an endpoint and then makes a call to it.

**Parameters:**
- `domainUrl`: The domain where the API is hosted
- `path`: The path to the API endpoint
- `method`: The HTTP method
- `queryParams`: (Optional) Query parameters as a JSON string
- `body`: (Optional) Request body as a JSON string
- `headers`: (Optional) Headers as a JSON string

**Returns:** The result of the API call.


### HTTP Request Tools

#### makeRequest
Makes an HTTP request to a URI with any method.

**Parameters:**
- `url`: The full URL to make the request to
- `method`: The HTTP method to use (GET, POST, PUT, DELETE, PATCH)
- `queryParams`: (Optional) Query parameters as a JSON string
- `body`: (Optional) Request body as a JSON string (for POST, PUT, PATCH)
- `headers`: (Optional) Headers as a JSON string

**Returns:** The response from the API call.


#### makeGetRequest
Makes a GET request to a URI.

**Parameters:**
- `url`: The full URL to make the request to
- `queryParams`: (Optional) Query parameters as a JSON string
- `headers`: (Optional) Headers as a JSON string

**Returns:** The response from the API call.


#### makePostRequest
Makes a POST request to a URI.

**Parameters:**
- `url`: The full URL to make the request to
- `queryParams`: (Optional) Query parameters as a JSON string
- `body`: (Optional) Request body as a JSON string
- `headers`: (Optional) Headers as a JSON string

**Returns:** The response from the API call.



## Building and Running

### Using Gradle

```bash
./gradlew build
./gradlew bootRun
```

### Using Java JAR

```bash
java -jar build/libs/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar
```

## Additional Resources

* [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
* [MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
* [Model Context Protocol Specification](https://modelcontextprotocol.github.io/specification/)
