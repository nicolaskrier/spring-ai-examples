# Spring AI Examples

*Spring AI examples using Mistral AI and Ollama as AI model providers.*

## 🛠️ Pre-Requisites

- Java 25
- Maven 4
- Mistral AI API key
- Ollama
- Docker

## 📚 Use Cases

- **Ollama API**: Simple local inference using Ollama's low-level API.
- **Mistral AI API**: Simple remote inference using Mistral AI's low-level API.
- **Chat Model**: Example demonstrating the abstraction provided by the chat model.
- **Chat Client**: Example covering the following features:
    - Thinking model,
    - JSON structured output,
    - In memory chat memory,
    - Request and response message logging.
- **RAG**: Example covering the following features:
    - Embedding model,
    - JSON data with metadata reading to add documents to Qdrant vector store only if the stored collection is empty,
    - Question and answer with a filtered search limiting data retrieval from the vector store,
    - JSON structured output,
    - JDBC chat memory,
    - Request and response message logging,
    - Docker compose file containing PostgreSQL and Qdrant.
- **Tools**: Example covering the following features:
    - Custom tools to fetch current date time and to search pope either by date or by pontiff number,
    - JSON structured output,
    - In memory chat memory,
    - Request and response message logging.
- **MCP**: Example covering the following features:
    - Stateless Streamable-HTTP MCP servers offering tools to fetch current date time and to search pope either by date or by pontiff number,
    - MCP client using these two MCP servers,
    - JSON structured output,
    - JDBC chat memory,
    - Request and response message logging,
    - Docker compose file containing PostgreSQL,
    - Docker compose file containing both MCP servers.

## 🧠 Models

### Chat Models

- **Ollama**:
    - Gemma 4 Latest,
    - Ministral 3 14B,
    - Qwen 3.5 9B.
- **Mistral AI**:
    - Ministral 3 14B Latest,
    - Mistral Medium Latest,
    - Magistral Medium Latest.

### Embedding Models

- **Ollama**:
    - Embedding Gemma Latest.
- **Mistral AI**:
    - Mistral Embed.

## 🗃️ Vector Stores

- **Qdrant**
