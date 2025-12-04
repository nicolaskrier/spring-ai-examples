# Spring AI Examples

*Examples using Spring AI with Mistral AI and Ollama LLM providers.*

## 🛠️ Pre-Requisites

- Java 25
- Maven 4
- Mistral AI API key
- Ollama
- Docker

## 📚 Use Cases

### 1️⃣ Part 1

- **Ollama API**: Simple local inference using Ollama's low-level API.
- **Mistral AI API**: Simple remote inference using Mistral AI's low-level API.
- **Chat Model**: Example demonstrating the abstraction provided by the chat model.
- **Chat Client**: Example covering the following features:
    - Thinking LLM,
    - JSON structured output,
    - Chat memory,
    - Request and response message logging.

### 2️⃣ Part 2 🚧

- **RAG**: Example covering the following features:
    - Thinking LLM,
    - JSON structured output,
    - Chat memory,
    - Request and response message logging,
    - JSON data with metadata reading to add documents to the vector store,
    - Question and answer with a filtered search limiting data retrieval from the vector store.

## 🧠 Models

### Chat Models

- **Ollama**:
    - Mistral Small 3.2 Latest,
    - Qwen 3 14B.
- **Mistral AI**:
    - Mistral Small Latest,
    - Mistral Medium Latest,
    - Magistral Medium Latest.

### Embedding Models

- **Ollama**:
    - Qwen 3 Embedding Latest.
- **Mistral AI**:
    - Mistral Embed.
