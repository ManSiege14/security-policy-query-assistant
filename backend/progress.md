# VectorDB Java — Development Progress

**Project:** VectorDB Java
**Author:** Mansij
**Stack:** Java 21 • Spring Boot 3.2 • React + Vite • Ollama • PDFBox

---

# Project Goal

Build a Retrieval-Augmented Generation (RAG) system completely from scratch to understand how modern AI applications work internally.

The project intentionally avoids frameworks such as LangChain and Spring AI. Every major component—including vector storage, semantic search, document ingestion, prompt construction, and persistence—is implemented manually.

---

# Current Status

## Backend

* ✅ Spring Boot REST API
* ✅ Custom Vector Database
* ✅ Ollama Integration
* ✅ Document Pipeline
* ✅ Semantic Search
* ✅ Retrieval-Augmented Generation
* ✅ PDF Upload
* ✅ JSON Persistence
* ✅ Startup Recovery

## Frontend

* ✅ React + Vite
* ✅ Search Page
* ✅ Documents Page
* ✅ Ask AI Page
* ✅ PDF Upload
* ✅ System Status Dashboard

---

# Completed Milestones

---

# ✅ Step 1 — Spring Boot Foundation

## Goal

Create the backend project structure.

## Implemented

* Maven setup
* Spring Boot application
* Configuration management
* Base REST server

## Verification

```bash
mvn spring-boot:run
GET /status
```

---

# ✅ Step 2 — Vector Mathematics

## Goal

Implement similarity calculations used by the vector database.

## Implemented

* Cosine Distance
* Cosine Similarity
* Euclidean Distance
* Manhattan Distance
* Distance dispatcher

## Result

Supports multiple search metrics without changing higher-level code.

---

# ✅ Step 3 — Text Chunking

## Goal

Prepare large documents for embedding.

## Implemented

* Word-based chunking
* Configurable overlap
* Word counting

Default:

* 250 words
* 30 word overlap

---

# ✅ Step 4 — Ollama Integration

## Goal

Connect to local AI models.

## Implemented

Embedding

```
nomic-embed-text
```

Generation

```
llama3.2
```

Status monitoring

```
GET /status
```

---

# ✅ Step 5 — Vector Database

## Goal

Implement a custom vector database.

## Implemented

VectorStoreService

Operations

* insert
* delete
* list
* search

Features

* ConcurrentHashMap storage
* Exact KNN search
* Cosine distance
* Euclidean distance
* Manhattan distance

Two independent stores

```
demoVectorStore
docVectorStore
```

---

# ✅ Step 6 — Document Pipeline

## Goal

Store real documents inside the vector database.

Pipeline

```
Document

↓

Chunk

↓

Embedding

↓

Vector Storage

↓

Metadata Storage
```

Implemented

* DocumentService
* DocumentController
* Metadata store
* Chunk tracking

Endpoints

```
POST /api/documents

GET /api/documents

DELETE /api/documents/{id}
```

---

# ✅ Step 7 — Retrieval-Augmented Generation

Pipeline

```
Question

↓

Embedding

↓

Vector Search

↓

Top K Chunks

↓

Prompt Builder

↓

llama3.2

↓

Answer
```

Endpoint

```
POST /api/rag/ask
```

Features

* Grounded answers
* Configurable context size
* Source chunk retrieval

---

# ✅ Step 8 — Search Improvements

Implemented

* Text query search
* Search endpoint for frontend
* Search directly over uploaded documents
* Multiple distance metrics
* Improved result handling

---

# ✅ Step 9 — React Frontend

Implemented

React + Vite frontend.

Pages

* Search
* Documents
* Ask AI

Features

* Sidebar navigation
* Status polling
* REST API integration

---

# ✅ Step 10 — Semantic Search UI

Implemented

Search interface

Supports

* Natural language queries
* Top-K selection
* Metric selection

Displays

* Matching chunks
* Metadata
* Delete actions

---

# ✅ Step 11 — PDF Upload

Implemented

Apache PDFBox integration.

Pipeline

```
PDF

↓

PdfService

↓

Extract Text

↓

Chunk

↓

Embedding

↓

Vector Store
```

Frontend

* Upload PDF directly
* Automatic ingestion
* Existing document pipeline reused

Endpoint

```
POST /api/documents/upload
```

---

# ✅ Step 12 — Persistence Layer

Goal

Ensure uploaded documents survive backend restarts.

Implemented

PersistenceService

Files

```
vectors.json

documents.json
```

Automatic save

* Insert
* Delete

Automatic recovery

```
@PostConstruct

↓

Load vectors

↓

Load metadata

↓

Restore memory
```

Verification

```
Upload PDF

↓

Restart backend

↓

Documents still available

↓

Search works

↓

Ask AI works
```

---

# Current Architecture

```
React Frontend

        │

        ▼

Spring Boot REST API

        │

 ┌──────┼─────────────┐

 ▼      ▼             ▼

Search  Documents     RAG

        │

        ▼

DocumentService

        │

        ▼

TextChunker

        │

        ▼

Ollama Embeddings

        │

        ▼

VectorStoreService

        │

        ▼

PersistenceService

        │

        ▼

vectors.json

documents.json
```

---

# REST API

## Status

```
GET /status
```

---

## Demo Vector Store

```
GET /api/demo/items

POST /api/demo/insert

POST /api/demo/search

DELETE /api/demo/delete/{id}
```

---

## Documents

```
POST /api/documents

POST /api/documents/upload

POST /api/documents/search

GET /api/documents

DELETE /api/documents/{id}
```

---

## RAG

```
POST /api/rag/ask
```

---

# Frontend

Pages

```
Search

Documents

Ask AI
```

Capabilities

* Semantic search
* Upload PDFs
* Browse documents
* Ask questions
* System status
* Delete documents

---

# Persistence

Files

```
data/

vectors.json

documents.json
```

Behavior

```
Insert

↓

Auto Save

↓

Restart

↓

Auto Load

↓

Application Restored
```

---

# Folder Structure

```
backend

config

controller

core

model

service

AppConfig

DocumentService

RagService

PdfService

PersistenceService

OllamaService

VectorStoreService

frontend

pages

SearchPage

DocumentsPage

AskAIPage

api.js
```

---

# Current Capabilities

* Custom Vector Database
* Exact KNN Search
* Multiple Distance Metrics
* Ollama Embeddings
* Local LLM
* PDF Upload
* Plain Text Upload
* Automatic Chunking
* Semantic Search
* Retrieval-Augmented Generation
* React Frontend
* Persistent Storage
* Startup Recovery

---

# Known Limitations

Current implementation intentionally keeps the architecture simple.

Remaining limitations

* Exact linear search O(N)
* No Approximate Nearest Neighbor index
* No retrieval scores in UI
* No grouped document management
* No streaming responses
* JSON persistence only
* Single-user application
* No authentication
* No Docker deployment

---

# Next Development Phase

## Step 13

Retrieval explainability

* Similarity scores
* Better source ranking

---

## Step 14

Document management

* Group chunks by document
* Delete complete documents
* Document statistics

---

## Step 15

UI improvements

* Loading indicators
* Toast notifications
* Better upload experience
* Improved styling

---

## Step 16

Conversation memory

* Multi-turn chat
* Chat history
* Context reuse

---

# Long-Term Roadmap

* Approximate Nearest Neighbor Search (HNSW)
* PostgreSQL + pgvector
* Hybrid Search
* Streaming Responses
* Docker
* Cloud Deployment
* User Authentication
* Admin Dashboard

---

# Major Design Decisions

* No LangChain
* No Spring AI
* Manual vector database implementation
* Manual RAG pipeline
* Local AI models using Ollama
* JSON persistence before database integration
* React frontend separated from backend
* Reusable services with clear separation of concerns

---

# Quick Commands

## Backend

```bash
cd backend
mvn spring-boot:run
```

## Frontend

```bash
cd frontend
npm install
npm run dev
```

## Ollama

```bash
ollama serve

ollama pull nomic-embed-text

ollama pull llama3.2
```

---

# Current Version

**v0.12**

The project is currently a fully functional local RAG application featuring:

* PDF ingestion
* Semantic search
* Local AI responses
* Persistent storage
* React frontend
* Startup recovery

The next phase focuses on improving retrieval quality, document management, and user experience rather than adding core functionality.
