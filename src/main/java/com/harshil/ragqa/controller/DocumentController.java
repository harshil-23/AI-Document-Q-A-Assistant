package com.harshil.ragqa.controller;

import com.harshil.ragqa.dto.Dtos;
import com.harshil.ragqa.entity.Document;
import com.harshil.ragqa.repository.DocumentRepository;
import com.harshil.ragqa.service.DocumentIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentIngestionService ingestionService;
    private final DocumentRepository documentRepository;

    public DocumentController(DocumentIngestionService ingestionService, DocumentRepository documentRepository) {
        this.ingestionService = ingestionService;
        this.documentRepository = documentRepository;
    }

    @PostMapping
    public ResponseEntity<Dtos.IngestDocumentResponse> ingest(@Valid @RequestBody Dtos.IngestDocumentRequest request) {
        Document document = ingestionService.submitForIngestion(request.title(), request.content());
        return ResponseEntity.accepted()
                .body(new Dtos.IngestDocumentResponse(document.getId(), document.getStatus().name()));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Dtos.IngestDocumentResponse> status(@PathVariable UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Document not found: " + id));
        return ResponseEntity.ok(new Dtos.IngestDocumentResponse(document.getId(), document.getStatus().name()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}
