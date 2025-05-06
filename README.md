# Spring AI MCP OpenAPI Server Sample with STDIO Starter

This sample project demonstrates how to create an MCP server using the Spring AI MCP Server Boot Starter with STDIO transport. It implements:
1. An OpenAPI specification collector service that fetches OpenAPI specifications from domains
2. An endpoint analyzer service that analyzes API endpoints
3. An HTTP request service that makes API calls

For more information, see the [MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html) reference documentation.

## Overview

The sample showcases:
- Integration with `spring-ai-starter-mcp-server-stdio`
- Support for STDIO transport
- Automatic tool registration using Spring AI's `@Tool` annotation
- Organized services in a dedicated package structure
- OpenAPI-related tools:
  - Fetch OpenAPI specification from a domain URL
  - Analyze API endpoints
  - Make HTTP requests to APIs

## Dependencies

The project requires the Spring AI MCP Server STDIO Boot Starter:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-stdio</artifactId>
</dependency>
```

This starter provides:
- STDIO transport for MCP server
- Auto-configured STDIO communication
- Simplified configuration for STDIO-based applications

## Building the Project

### Using Gradle (Recommended)

This project has been migrated from Maven to Gradle with Kotlin DSL. Build the project using Gradle:

```bash
./gradlew build
```

To run the application:

```bash
./gradlew bootRun
```

### Using Maven (Legacy)

Alternatively, you can still build the project using Maven:

```bash
./mvnw clean install -DskipTests
```

## Gradle Migration

The project has been migrated from Maven to Gradle with Kotlin DSL. Key files:

- `build.gradle.kts`: Main build script with dependencies and plugins
- `settings.gradle.kts`: Project settings
- `gradle.properties`: Gradle properties for performance optimization
- `gradle/wrapper/gradle-wrapper.properties`: Wrapper configuration

Benefits of the Gradle migration:
- More concise and readable build scripts with Kotlin DSL
- Better performance with parallel execution and build caching
- Improved dependency management

## Code Improvements

The codebase has been optimized by:

1. Removing unnecessary functions:
   - Removed the example `toUpperCase` function from `McpServerApplication`
   - Consolidated duplicate HTTP methods in `RequestMakerService`
   - Streamlined the `OpenApiService` by removing redundant methods

2. Improving code patterns:
   - Extracted common functionality into helper methods
   - Enhanced error handling
   - Used modern Java features like records, text blocks, and var
   - Improved method organization and naming

## Running the Server

The server uses STDIO transport mode:

### Using Gradle

```bash
./gradlew bootRun
```

### Using Java JAR

```bash
# If built with Gradle
java -jar build/libs/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar

# If built with Maven
java -jar target/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar
```

The STDIO transport is enabled by default in the application.yaml configuration.

## Configuration

Configure the server through `application.yaml`:

```yaml
# NOTE: You must disable the banner and the console logging 
# to allow the STDIO transport to work !!!
spring:
  main:
    web-application-type: none
    banner-mode: off
  ai:
    mcp:
      server:
        name: openapi-collector-server
        version: 0.0.1
        stdio: true
        enabled: true

logging:
  pattern:
    console: 
  file:
    name: ./target/starter-stdio-server.log
```

Note that the `type` configuration is not necessary for STDIO transport.

## Available Tools

### Fetch OpenAPI Specification Tool
- Name: `fetchOpenApiSpec`
- Description: Fetch OpenAPI specification from a domain URL
- Parameters:
  - `domainUrl`: String - The URL of the domain to fetch the OpenAPI specification from
- Example:
```java
CallToolResult openApiResult = client.callTool(new CallToolRequest("fetchOpenApiSpec", 
    Map.of("domainUrl", "petstore.swagger.io")));
```

### Fetch OpenAPI Specification with Path Tool
- Name: `fetchOpenApiSpecWithPath`
- Description: Fetch OpenAPI specification from a domain URL with a specific path
- Parameters:
  - `domainUrl`: String - The URL of the domain to fetch the OpenAPI specification from
  - `path`: String - The specific path to the OpenAPI specification
- Example:
```java
CallToolResult openApiPathResult = client.callTool(new CallToolRequest("fetchOpenApiSpecWithPath", 
    Map.of("domainUrl", "petstore.swagger.io", "path", "/v2/swagger.json")));
```

### Fetch OpenAPI Specification as JSON Tool
- Name: `fetchOpenApiSpecAsJson`
- Description: Fetch OpenAPI specification from a domain URL and return as JSON
- Parameters:
  - `domainUrl`: String - The URL of the domain to fetch the OpenAPI specification from
- Example:
```java
CallToolResult jsonResult = client.callTool(new CallToolRequest("fetchOpenApiSpecAsJson", 
    Map.of("domainUrl", "petstore.swagger.io")));
```

### Fetch OpenAPI Specification with Path as JSON Tool
- Name: `fetchOpenApiSpecWithPathAsJson`
- Description: Fetch OpenAPI specification from a domain URL with a specific path and return as JSON
- Parameters:
  - `domainUrl`: String - The URL of the domain to fetch the OpenAPI specification from
  - `path`: String - The specific path to the OpenAPI specification
- Example:
```java
CallToolResult jsonPathResult = client.callTool(new CallToolRequest("fetchOpenApiSpecWithPathAsJson", 
    Map.of("domainUrl", "petstore.swagger.io", "path", "/v2/swagger.json")));
```

> **Note**: The `makeApiCallable` tools have been removed as part of the code improvements.

### URI Analyzer Tool
- Name: `analyzeUri`
- Description: Analyze a URI from an OpenAPI specification to determine its parameters and body requirements
- Parameters:
  - `domainUrl`: String - The domain where the API is hosted (e.g., 'api.example.com')
  - `path`: String - The path to the API endpoint (e.g., '/pets/{petId}')
  - `method`: String - The HTTP method (GET, POST, PUT, DELETE, etc.)
- Example:
```java
CallToolResult uriAnalysisResult = client.callTool(new CallToolRequest("analyzeUri", 
    Map.of("domainUrl", "petstore.swagger.io", "path", "/v2/pet/{petId}", "method", "GET")));
```

### Request Maker Tools
The following tools allow making HTTP requests to any URI with different methods:

#### Generic Request Tool
- Name: `makeRequest`
- Description: Make an HTTP request to a URI with any method
- Parameters:
  - `url`: String - The full URL to make the request to
  - `method`: String - The HTTP method to use (GET, POST, PUT, DELETE, PATCH)
  - `queryParams`: String - (Optional) Query parameters as a JSON string
  - `body`: String - (Optional) Request body as a JSON string (for POST, PUT, PATCH)
  - `headers`: String - (Optional) Headers as a JSON string
- Example:
```java
CallToolResult requestResult = client.callTool(new CallToolRequest("makeRequest", 
    Map.of("url", "https://api.example.com/resource", 
           "method", "GET",
           "queryParams", "{\"limit\": \"10\"}", 
           "body", "null",
           "headers", "{\"Authorization\": \"Bearer token\"}")));
```

#### GET Request Tool
- Name: `makeGetRequest`
- Description: Make a GET request to a URI
- Parameters:
  - `url`: String - The full URL to make the request to
  - `queryParams`: String - (Optional) Query parameters as a JSON string
  - `headers`: String - (Optional) Headers as a JSON string
- Example:
```java
CallToolResult getResult = client.callTool(new CallToolRequest("makeGetRequest", 
    Map.of("url", "https://api.example.com/resource", 
           "queryParams", "{\"limit\": \"10\"}", 
           "headers", "{\"Authorization\": \"Bearer token\"}")));
```

#### POST Request Tool
- Name: `makePostRequest`
- Description: Make a POST request to a URI
- Parameters:
  - `url`: String - The full URL to make the request to
  - `queryParams`: String - (Optional) Query parameters as a JSON string
  - `body`: String - (Optional) Request body as a JSON string
  - `headers`: String - (Optional) Headers as a JSON string
- Example:
```java
CallToolResult postResult = client.callTool(new CallToolRequest("makePostRequest", 
    Map.of("url", "https://api.example.com/resource", 
           "queryParams", "{}", 
           "body", "{\"name\": \"John\", \"age\": 30}", 
           "headers", "{\"Authorization\": \"Bearer token\"}")));
```

> **Note**: The PUT, DELETE, and PATCH specific methods have been removed in favor of the more flexible `makeRequest` method. Use the generic `makeRequest` method with the appropriate HTTP method parameter instead.

## Server Implementation

The server uses Spring Boot and Spring AI's tool annotations for automatic tool registration. The services are organized in a dedicated package structure:

```java
package io.openapi.serve;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import io.openapi.serve.services.HttpRequestService;
import io.openapi.serve.services.OpenApiSpecificationService;
import io.openapi.serve.services.EndpointAnalyzerService;

@SpringBootApplication
public class McpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider httpRequestTools(HttpRequestService httpRequestService) {
        return MethodToolCallbackProvider.builder().toolObjects(httpRequestService).build();
    }

    @Bean
    public ToolCallbackProvider openApiTools(OpenApiSpecificationService openApiSpecificationService) {
        return MethodToolCallbackProvider.builder().toolObjects(openApiSpecificationService).build();
    }

    @Bean
    public ToolCallbackProvider endpointAnalyzerTools(EndpointAnalyzerService endpointAnalyzerService) {
        return MethodToolCallbackProvider.builder().toolObjects(endpointAnalyzerService).build();
    }
}
```

The services are located in the `io.openapi.serve.services` package:

1. `HttpRequestService`: Provides tools for making HTTP requests to external APIs
2. `OpenApiSpecificationService`: Provides tools for fetching and parsing OpenAPI specifications
3. `EndpointAnalyzerService`: Provides tools for analyzing API endpoints

Each service implements its functionality using the `@Tool` annotation to expose methods to the AI model:

```java
package io.openapi.serve.services;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OpenApiSpecificationService {
    @Tool(description = "Discover and fetch the REST API specification from a domain using OpenAPI.")
    public Mono<String> getApiSpecification(String domainUrl) {
        // Implementation to fetch OpenAPI spec from domain URL
    }

    // Other methods...
}
```

```java
package io.openapi.serve.services;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class EndpointAnalyzerService {
    @Tool(description = "Analyze a URI to determine its parameters and body requirements.")
    public Mono<String> analyzeEndpoint(String domainUrl, String path, String method) {
        // Implementation to analyze an endpoint
    }

    // Other methods...
}
```

```java
package io.openapi.serve.services;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class HttpRequestService {
    @Tool(description = "Make an HTTP request to a URI.")
    public Mono<String> makeRequest(String url, String method, String queryParams, String body, String headers) {
        // Implementation to make an HTTP request
    }

    // Other methods...
}
```

## MCP Clients 

You can connect to the server using STDIO transport:

### OpenAPI Command-Line Client

The project includes a command-line client specifically for the OpenAPI service:

```bash
# If using Gradle
./gradlew build -x test

# Note: The client examples below are no longer applicable as the test directory has been removed.
# These examples are kept for reference purposes only.

# Run the OpenAPI client with SSE transport (server must be running)
java -cp build/libs/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar io.openapi.serve.client.OpenApiClient fetch petstore.swagger.io

# Run the OpenAPI client with a specific path
java -cp build/libs/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar io.openapi.serve.client.OpenApiClient fetchWithPath petstore.swagger.io /v2/swagger.json

# Run the OpenAPI client with STDIO transport (automatically starts the server)
java -cp build/libs/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar io.openapi.serve.client.OpenApiClient fetch petstore.swagger.io --stdio

# If using Maven
./mvnw clean install -DskipTests
java -cp target/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar io.openapi.serve.client.OpenApiClient fetch petstore.swagger.io
```

Available commands:
- `fetch` - Fetch OpenAPI spec from a domain
- `fetchWithPath` - Fetch OpenAPI spec from a domain with a specific path
- `fetchAsJson` - Fetch OpenAPI spec from a domain as JSON
- `fetchWithPathAsJson` - Fetch OpenAPI spec from a domain with a specific path as JSON

> **Note**: The `makeCallable` and `makeCallableWithPath` commands have been removed as part of the code improvements.

### Manual Clients

The server only supports STDIO transport:

#### STDIO Client

For servers using STDIO transport:

```java
// If using Gradle build
var stdioParams = ServerParameters.builder("java")
    .args("-Dspring.ai.mcp.server.stdio=true",
          "-Dspring.main.web-application-type=none",
          "-Dspring.main.banner-mode=off",
          "-Dlogging.pattern.console=",
          "-jar",
          "build/libs/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar")
    .build();

// If using Maven build
// var stdioParams = ServerParameters.builder("java")
//     .args("-Dspring.ai.mcp.server.stdio=true",
//           "-Dspring.main.web-application-type=none",
//           "-Dspring.main.banner-mode=off",
//           "-Dlogging.pattern.console=",
//           "-jar",
//           "target/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar")
//     .build();

var transport = new StdioClientTransport(stdioParams);
var client = McpClient.sync(transport).build();
```

The sample project previously included example client implementations, but these have been removed:
- SampleClient.java: Manual MCP client implementation
- ClientStdio.java: STDIO transport connection
- ClientSse.java: SSE transport connection

For a better development experience, consider using the [MCP Client Boot Starters](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html). These starters enable auto-configuration of multiple STDIO and/or SSE connections to MCP servers. See the [starter-default-client](../../client-starter/starter-default-client) and [starter-webflux-client](../../client-starter/starter-webflux-client) projects for examples.

### Boot Starter Clients

Lets use a client to connect to our weather server.

#### STDIO Transport

1. Create a `mcp-servers-config.json` configuration file with this content:

```json
{
  "mcpServers": {
    "weather-stdio-server": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

> **Note**: Use the appropriate path based on your build system:
> - For Gradle: `/absolute/path/to/build/libs/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar`
> - For Maven: `/absolute/path/to/target/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar`

2. Run the client using the configuration file:

```bash
java -Dspring.ai.mcp.client.stdio.servers-configuration=file:mcp-servers-config.json \
 -Dai.user.input='What is the weather in NY?' \
 -Dlogging.pattern.console= \
 -jar mcp-client-0.0.1-SNAPSHOT.jar
```

## Additional Resources

* [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
* [MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
* [MCP Client Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-client-docs.html)
* [Model Context Protocol Specification](https://modelcontextprotocol.github.io/specification/)
* [Spring Boot Auto-configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)
