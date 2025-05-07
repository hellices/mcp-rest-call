# mcp-rest-call

[![Build](https://github.com/hellices/mcp-rest-call/actions/workflows/build-and-push.yml/badge.svg)](https://github.com/hellices/mcp-rest-call/actions/workflows/build-and-push.yml)
[![GitHub Actions Artifacts](https://img.shields.io/badge/GitHub%20Actions-Artifacts-blue?logo=github)](https://github.com/hellices/mcp-rest-call/actions)
[![Java](https://img.shields.io/badge/Java-21-blue.svg?logo=java)](https://openjdk.org/projects/jdk/21/)
[![Gradle](https://img.shields.io/badge/Gradle-8.7-blue?logo=gradle)](https://gradle.org/)
[![Docker](https://img.shields.io/badge/built%20with-Docker-blue?logo=docker)](https://hub.docker.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

This project is a Spring AI Model Context Protocol (MCP) server that provides tools for working with OpenAPI specifications. It enables AI models to interact with REST APIs by providing the following capabilities:

1. **OpenAPI Specification Collection**: Fetches and parses OpenAPI specifications from domains
2. **Endpoint Analysis**: Analyzes API endpoints to determine parameters and requirements
3. **HTTP Request Handling**: Makes API calls to external services

## Key Features

- Built with Spring Boot and Spring AI
- Implements the Model Context Protocol (MCP)
- Provides tools for AI models to discover and interact with REST APIs

---

## 🚀 Quick Start

### 🔧 Run via JAR

```bash
./gradlew build
java -jar build/libs/mcp-rest-call-*.jar
```

### 🐳 Run via Docker

```bash
docker pull ghcr.io/hellices/mcp-rest-call:latest
docker run -p 8080:8080 ghcr.io/hellices/mcp-rest-call:latest
```

> Or build and run it locally:
> ```bash
> ./gradlew build
> docker build -t mcp-rest-call .
> docker run -p 8080:8080 mcp-rest-call
> ```

---

## 🧠 Connect with VS Code (Cline + MCP)

1. Install the [Cline extension for VS Code](https://marketplace.visualstudio.com/items?itemName=saoudrizwan.claude-dev)
2. Open the Cline MCP settings:  
   Press `Cmd+Shift+P` (Mac) or `Ctrl+Shift+P` (Windows/Linux) → `Open Cline MCP settings`
3. Add your MCP server config:
   ```json
   {
      "mcpServers": {
         "openapi-collector-server": {
         "url": "http://127.0.0.1:8080/sse",
         "disabled": false,
         "autoApprove": [
            "analyzeEndpoint",
            "getApiSpecification",
            "makeGetRequest",
            "makePostRequest",
            "makeRequest",
            "analyzeAndCall"
         ],
         "transportType": "sse"
         }
      }
   }
   ```
4. Use Cline commands such as `analyzeEndpoint` or `makeRequest` directly in VS Code

 

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


## Additional Resources

* [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
* [MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
* [Model Context Protocol Specification](https://modelcontextprotocol.github.io/specification/)
