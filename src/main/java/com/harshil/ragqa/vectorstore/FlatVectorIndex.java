package com.harshil.ragqa.vectorstore;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IMPORTANT — read this before you put "FAISS" on your resume for this code:
 *
 * This class implements brute-force cosine-similarity search over vectors
 * held in memory — which is functionally what FAISS's `IndexFlatIP` does
 * (FAISS's simplest index type: no approximation, exact nearest-neighbor
 * search by full scan). Real FAISS is a C++ library with SIMD-optimized
 * distance computation and, at scale, approximate indexes (IVF, HNSW) that
 * trade a little accuracy for huge speedups on millions of vectors.
 *
 * For a portfolio project at the scale of "a handful of documents", a flat
 * index is the *correct* engineering choice — FAISS's own docs recommend
 * flat indexes under ~10k-100k vectors, since approximate indexes add
 * complexity without a measurable benefit at that size. The honest resume
 * framing is: "implemented a FAISS-style flat vector index"; if you want a
 * real FAISS/pgvector dependency, see the note in vectorSearchTodo() below
 * for where that swap goes.
 */
@Component
public class FlatVectorIndex {

    private record IndexedVector(UUID chunkId, UUID documentId, float[] embedding) {
    }

    private final List<IndexedVector> vectors = new ArrayList<>();
    private final Map<UUID, String> chunkTextById = new ConcurrentHashMap<>();

    public synchronized void add(UUID chunkId, UUID documentId, float[] embedding, String text) {
        vectors.add(new IndexedVector(chunkId, documentId, embedding));
        chunkTextById.put(chunkId, text);
    }

    public record SearchResult(UUID chunkId, String text, double score) {
    }

    /**
     * Brute-force top-k cosine similarity search — O(n) per query. Fine up
     * to tens of thousands of chunks; beyond that, swap this method's body
     * for a call to a real vector DB (pgvector `ORDER BY embedding <=> ?`,
     * or FAISS via a sidecar Python service) without touching any caller.
     */
    public List<SearchResult> search(float[] queryEmbedding, int topK) {
        synchronized (this) {
            return vectors.stream()
                    .map(v -> new SearchResult(v.chunkId(), chunkTextById.get(v.chunkId()),
                            cosineSimilarity(queryEmbedding, v.embedding())))
                    .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                    .limit(topK)
                    .toList();
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public int size() {
        return vectors.size();
    }
}
