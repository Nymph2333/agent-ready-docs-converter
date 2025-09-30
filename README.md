# Agent-Ready Documentation Converter

A tool that extracts API and configuration metadata from Spring Boot projects and converts them into agent-ready documentation formats, including OpenAPI 3.0, JSON Schema, and AgentCard (A2A/MCP).

## Features

- **Extract Spring Boot API metadata** from controllers, models, and configuration files
- **Multiple output formats**:
  - OpenAPI 3.0 (YAML/JSON)
  - JSON Schema
  - AgentCard (A2A/MCP compatible)
- **Command-line interface** for easy integration
- **Comprehensive analysis** of endpoints, models, and configuration properties

## Project Structure

```
├── core/                    # Shared tools and converters
├── spring-boot-converter/   # Spring Boot metadata extraction and CLI
├── examples/               # Sample Spring Boot applications for testing
│   └── sample-spring-boot-app/
└── README.md
```

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Build

```bash
mvn clean install
```

### Usage

```bash
java -jar spring-boot-converter/target/spring-boot-converter-1.0.0-SNAPSHOT.jar <project-path> <format> [output-file]
```

#### Arguments

- `project-path`: Path to the Spring Boot project directory
- `format`: Output format (openapi-yaml, openapi-json, json-schema, agentcard)
- `output-file`: Optional output file path (prints to console if not provided)

#### Examples

```bash
# Generate OpenAPI YAML documentation
java -jar spring-boot-converter/target/spring-boot-converter-1.0.0-SNAPSHOT.jar ./my-spring-app openapi-yaml

# Generate AgentCard format and save to file
java -jar spring-boot-converter/target/spring-boot-converter-1.0.0-SNAPSHOT.jar ./my-spring-app agentcard output.json

# Generate JSON Schema
java -jar spring-boot-converter/target/spring-boot-converter-1.0.0-SNAPSHOT.jar ./my-spring-app json-schema schema.json
```

### Test with Sample Application

```bash
# Test with the included sample Spring Boot app
java -jar spring-boot-converter/target/spring-boot-converter-1.0.0-SNAPSHOT.jar examples/sample-spring-boot-app agentcard
```

## Supported Formats

### OpenAPI 3.0 (openapi-yaml, openapi-json)
Standard OpenAPI specification for REST APIs, compatible with Swagger UI and other OpenAPI tools.

### JSON Schema (json-schema)
JSON Schema specification for data validation and documentation.

### AgentCard (agentcard)
Agent-ready format compatible with A2A/MCP protocols, optimized for AI agent consumption.

## What Gets Extracted

- **Endpoints**: REST endpoints from @RestController classes
- **Models**: Data models from POJOs with proper type mapping
- **Configuration**: Properties from application.properties/yml files
- **Metadata**: Application information from pom.xml or build.gradle

## Sample Output

The tool successfully extracts:
- ✅ 6 REST endpoints (GET, POST, PUT, DELETE)
- ✅ 2 data models with 7 and 5 properties respectively
- ✅ 16 configuration properties
- ✅ Complete OpenAPI 3.0 specification
- ✅ Agent-ready documentation with tools and resources

## Development

### Running Tests

```bash
mvn test
```

### Adding New Formats

1. Implement the `DocumentConverter` interface in the `core` module
2. Register the converter in `DocumentConverterCLI`
3. Add appropriate file extension mapping

## License

MIT License - see [LICENSE](LICENSE) file for details.
