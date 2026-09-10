# 🔍 AI Document Q&A Assistant

**A production-style RAG (Retrieval-Augmented Generation) pipeline** — ask questions over your own documents and get grounded, source-cited answers. Built with Spring Boot, LangChain4j, and OpenAI, with async and idempotent ingestion designed the way a real backend service would handle it.

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.34-blue)](https://github.com/langchain4j/langchain4j)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

---

## ✨ Features

- **Semantic search** over ingested documents using OpenAI embeddings
- **Grounded answers** — the model is prompted to answer only from retrieved context, with source chunks returned alongside every answer
- **Idempotent ingestion** — duplicate document submissions (e.g. retried uploads) are detected via content hash and never double-processed
- **Async processing** — ingestion runs off the request thread on a dedicated pool, so uploads return immediately regardless of document size
- **FAISS-style vector search** — a flat cosine-similarity index, matching what FAISS's `IndexFlatIP` does at this scale

## 🏗️ Architecture

```
 POST /documents                         POST /query
       │                                       │
       ▼                                       ▼
┌─────────────┐   async    ┌──────────────┐  embed question   ┌──────────────────┐
│  Ingestion   │──────────▶│  Chunking +  │                   │  FlatVectorIndex  │
│  Controller  │  (thread   │  Embedding   │──── stores ─────▶│  (cosine search)  │
│ (idempotent, │   pool)    │   Service    │                   └─────────┬────────┘
│  hash-check) │            └──────────────┘                             │ top-K chunks
└─────────────┘                                                          ▼
                                                                  ┌───────────────┐
                                                                  │  Query Service │
                                                                  │ (RAG prompt +  │
                                                                  │  chat model)   │
                                                                  └───────────────┘
```

## 🚀 Quick start

```bash
export OPENAI_API_KEY=sk-...
mvn spring-boot:run
```

**Ingest a document:**
```bash
curl -X POST http://localhost:8080/documents \
  -H "Content-Type: application/json" \
  -d '{"title": "Sample Doc", "content": "Your document text here..."}'
```

**Ask a question:**
```bash
curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What does the document say about X?"}'
```

**Or with Docker:**
```bash
docker build -t rag-document-qa .
docker run -p 8080:8080 -e OPENAI_API_KEY=sk-... rag-document-qa
```

## 🧠 Design decisions

| Decision | Why |
|---|---|
| SHA-256 content-hash dedup | Prevents re-embedding (and re-billing) the same document on retried uploads |
| Dedicated ingestion thread pool | Keeps the upload endpoint fast; swaps cleanly for an SQS consumer at scale |
| In-memory flat vector index | FAISS's own docs recommend flat indexes under ~10k-100k vectors — the right call here, not a corner cut |
| Grounded-only prompting | Reduces hallucination; every answer is traceable to source chunks |

## 🛠️ Tech stack

Java 17 · Spring Boot 3 · LangChain4j · OpenAI (embeddings + chat) · H2/PostgreSQL · Docker · GitHub Actions CI

## 📋 Honest scope

This is a portfolio/learning project, not a hardened production service:
- No auth on endpoints yet
- No chunk deletion or re-ingestion of an updated document
- Vector index is in-memory and rebuilds on restart (fine at this scale — see design decisions above)

## 👤 Author

**Harshil Agarwal** — Backend Software Engineer
[GitHub](https://github.com/harshil-23) · [LinkedIn](https://www.linkedin.com/in/harshil-agarwal-015482152/)
