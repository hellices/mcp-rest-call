package io.openapi.serve;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.openapi.serve.services.HttpRequestService;
import io.openapi.serve.services.OpenApiSpecificationService;
import io.openapi.serve.services.EndpointAnalyzerService;

/**
 * Main application class for the OpenAPI server
 * Configured to serve tools directly from services
 */
@SpringBootApplication
public class McpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpServerApplication.class, args);
	}

	/**
	 * Bean for HTTP request tools
	 */
	@Bean
	public ToolCallbackProvider httpRequestTools(HttpRequestService httpRequestService) {
		return MethodToolCallbackProvider.builder().toolObjects(httpRequestService).build();
	}

	/**
	 * Bean for OpenAPI specification tools
	 */
	@Bean
	public ToolCallbackProvider openApiTools(OpenApiSpecificationService openApiSpecificationService) {
		return MethodToolCallbackProvider.builder().toolObjects(openApiSpecificationService).build();
	}

	/**
	 * Bean for endpoint analyzer tools
	 */
	@Bean
	public ToolCallbackProvider endpointAnalyzerTools(EndpointAnalyzerService endpointAnalyzerService) {
		return MethodToolCallbackProvider.builder().toolObjects(endpointAnalyzerService).build();
	}
}
