package com.ragchatbot.controller;

import com.ragchatbot.dto.ApiResponse;
import com.ragchatbot.dto.document.DocumentDto;
import com.ragchatbot.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Upload and manage RAG documents")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    @Operation(summary = "Upload a document for RAG processing")
    public ResponseEntity<ApiResponse<DocumentDto>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws Exception {
        DocumentDto doc = documentService.uploadDocument(file, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(doc, "Document uploaded and queued for processing"));
    }

    @GetMapping
    @Operation(summary = "List user's uploaded documents")
    public ResponseEntity<ApiResponse<Page<DocumentDto>>> getDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Page<DocumentDto> docs = documentService.getDocuments(authentication.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.ok(docs));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document and its vector data")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long id,
            Authentication authentication) {
        documentService.deleteDocument(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Document deleted"));
    }
}
