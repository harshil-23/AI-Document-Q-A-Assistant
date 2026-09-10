package com.harshil.ragqa.service;

import com.harshil.ragqa.entity.Document;
import com.harshil.ragqa.entity.DocumentChunk;
import com.harshil.ragqa.repository.DocumentChunkRepository;
import com.harshil.ragqa.repository.DocumentRepository;
import com.harshil.ragqa.vectorstore.FlatVectorIndex;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingModel embeddingModel;
    private final FlatVectorIndex vectorIndex;

    public DocumentIngestionService(DocumentRepository documentRepository,
                                     DocumentChunkRepository chunkRepository,
                                     EmbeddingModel embeddingModel,
                                     FlatVectorIndex vectorIndex) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingModel = embeddingModel;
        this.vectorIndex = vectorIndex;
    }

    /**
     * Entry point called by the controller. Returns immediately after
     * persisting a PENDING document — the actual chunking/embedding work
     * happens on the ingestion thread pool (see triggerAsyncProcessing),
     * which is what keeps the upload endpoint fast regardless of document
     * size.
     */
    @Transactional
    public Document submitForIngestion(String title, String content) {
        String contentHash = sha256(content);

        // IDEMPOTENCY CHECK: if this exact content was already submitted,
        // don't re-ingest it — return the existing record. This is the
        // dedup pattern you'd also use on an SQS consumer keyed by message
        // content hash instead of relying on "at-least-once" delivery to
        // never duplicate work.
        var existing = documentRepository.findByContentHash(contentHash);
        if (existing.isPresent()) {
            log.info("Duplicate submission detected for hash={}, returning existing document {}",
                    contentHash, existing.get().getId());
            return existing.get();
        }

        Document document = new Document();
        document.setTitle(title);
        document.setContentHash(contentHash);
        document.setStatus(Document.IngestionStatus.PENDING);
        document = documentRepository.save(document);

        triggerAsyncProcessing(document.getId(), content);
        return document;
    }

    @Async("ingestionExecutor")
    public void triggerAsyncProcessing(UUID documentId, String content) {
        try {
            updateStatus(documentId, Document.IngestionStatus.CHUNKING);
            List<TextSegment> segments = DocumentSplitters
                    .recursive(500, 50) // ~500 chars per chunk, 50 char overlap
                    .split(dev.langchain4j.data.document.Document.from(content));

            updateStatus(documentId, Document.IngestionStatus.EMBEDDING);
            for (int i = 0; i < segments.size(); i++) {
                embedAndStoreChunk(documentId, i, segments.get(i).text());
            }

            updateStatus(documentId, Document.IngestionStatus.READY);
            log.info("Document {} ready with {} chunks", documentId, segments.size());
        } catch (Exception e) {
            log.error("Ingestion failed for document {}", documentId, e);
            updateStatus(documentId, Document.IngestionStatus.FAILED, e.getMessage());
        }
    }

    private void embedAndStoreChunk(UUID documentId, int index, String text) {
        Response<dev.langchain4j.data.embedding.Embedding> response = embeddingModel.embed(text);
        float[] vector = response.content().vector();

        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(documentId);
        chunk.setChunkIndex(index);
        chunk.setText(text);
        chunk.setEmbeddingCsv(toCsv(vector));
        chunk = chunkRepository.save(chunk);

        vectorIndex.add(chunk.getId(), documentId, vector, text);
    }

    private void updateStatus(UUID documentId, Document.IngestionStatus status) {
        updateStatus(documentId, status, null);
    }

    @Transactional
    protected void updateStatus(UUID documentId, Document.IngestionStatus status, String errorMessage) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setStatus(status);
            doc.setErrorMessage(errorMessage);
            documentRepository.save(doc);
        });
    }

    private String toCsv(float[] vector) {
        StringBuilder sb = new StringBuilder();
        for (float f : vector) {
            if (sb.length() > 0) sb.append(',');
            sb.append(f);
        }
        return sb.toString();
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash content", e);
        }
    }
}
