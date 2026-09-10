package com.harshil.ragqa.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public class Dtos {

    public record IngestDocumentRequest(@NotBlank String title, @NotBlank String content) {
    }

    public record IngestDocumentResponse(UUID documentId, String status) {
    }

    public record QueryRequest(@NotBlank String question) {
    }

    public record QueryResponse(String answer, List<SourceChunk> sources, long latencyMs) {
    }

    public record SourceChunk(UUID chunkId, String snippet, double relevanceScore) {
    }
}
