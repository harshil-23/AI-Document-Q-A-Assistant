# AI Document Q&A Assistant

A RAG (Retrieval-Augmented Generation) pipeline for semantic document
search and Q&A, built with Spring Boot, LangChain4j, and OpenAI —
with async, idempotent ingestion designed to survive retries and
duplicate submissions without wasted work or duplicate data.

## Architecture

```
 POST /documents                         POST /query
       │                                       │
       ▼                                       ▼
┌─────────────┐   async    ┌──────────────┐  embed question   ┌────────────────┐
│  Ingestion   │──────────▶│  Chunking +  │                   │  FlatVectorIndex │
│  Controller  │  (thread   │  Embedding   │──── stores ─────▶│  (cosine search) │
│ (idempotent, │   pool)    │   Service    │                   └────────┬─────────┘
│  hash-check) │            └──────────────┘                            │ top-K chunks
└─────────────┘                                                        ▼
                                                                 ┌───────────────┐
                                                                 │  Query Service │
                                                                 │ (RAG prompt +  │
                                                                 │  chat model)   │
                                                                 └───────────────┘
```

## Key design decisions (worth knowing cold for interviews)

- **Idempotent ingestion** — every document is hashed (SHA-256); a
  duplicate submission (e.g. a client retry after a timeout) returns the
  existing document instead of re-chunking and re-embedding it. Same
  pattern you'd use for a dedup key on an SQS consumer.
- **Async ingestion** — `POST /documents` persists a `PENDING` record and
  returns immediately; chunking and embedding run on a dedicated thread
  pool, decoupled from the request thread. In a multi-instance prod
  deployment this becomes an SQS queue + consumer instead of an in-process
  executor — same decoupling idea, durable across restarts.
- **Vector search: FAISS-equivalent flat index, not the FAISS library** —
  `FlatVectorIndex` does brute-force cosine similarity in memory, which is
  functionally what FAISS's `IndexFlatIP` does. FAISS's own guidance is
  that a flat index is the right choice under ~10k-100k vectors — see the
  class-level comment in `FlatVectorIndex.java` for the honest framing and
  where a real FAISS/pgvector swap would go at larger scale.
- **Grounded answers** — the chat model is prompted to answer only from
  retrieved context, reducing hallucination and keeping answers traceable
  to source chunks (returned in the response).

## Run it locally

```bash
export OPENAI_API_KEY=sk-...
mvn spring-boot:run
```

### Ingest a document

```bash
curl -X POST http://localhost:8080/documents \
  -H "Content-Type: application/json" \
  -d '{"title": "Sample Doc", "content": "Your document text here..."}'
```

### Check ingestion status

```bash
curl http://localhost:8080/documents/<id>/status
```

### Ask a question

```bash
curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What does the document say about X?"}'
```

## Run with Docker

```bash
docker build -t rag-document-qa .
docker run -p 8080:8080 -e OPENAI_API_KEY=sk-... rag-document-qa
```

## Tech stack

Java 17 · Spring Boot 3 · LangChain4j · OpenAI (embeddings + chat) ·
H2/PostgreSQL · Docker · GitHub Actions CI

## What's not here yet (honest scope)

- No authentication on the endpoints — add before any real deployment
- No chunk deletion/document re-ingestion (only insert-once by hash)
- Vector index is in-memory and rebuilt on restart — fine for a demo, not
  for production (persist to pgvector or reload from DB chunks on startup)

## Author

**Harshil Agarwal** — Backend Software Engineer
[GitHub](https://github.com/harshil-23) · [LinkedIn](https://www.linkedin.com/in/harshil-agarwal-015482152/)
