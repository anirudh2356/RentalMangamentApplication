package com.realestate.documents;

import com.realestate.documents.dto.DocumentResponse;
import com.realestate.documents.dto.DocumentUpdateRequest;
import com.realestate.documents.model.DocumentType;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<DocumentResponse> list(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DocumentType documentType,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long leadId,
            @RequestParam(required = false) Long dealId
    ) {
        return documentService.list(
                authentication.getName(), search, documentType, propertyId, leadId, dealId
        );
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam DocumentType documentType,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long leadId,
            @RequestParam(required = false) Long dealId
    ) {
        return documentService.upload(
                authentication.getName(), file, documentType, description, propertyId, leadId, dealId
        );
    }

    @GetMapping("/{id}")
    public DocumentResponse get(Authentication authentication, @PathVariable Long id) {
        return documentService.get(authentication.getName(), id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<FileSystemResource> download(Authentication authentication, @PathVariable Long id) {
        DocumentService.DocumentDownload download = documentService.download(authentication.getName(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new FileSystemResource(download.path()));
    }

    @PutMapping("/{id}")
    public DocumentResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody DocumentUpdateRequest request
    ) {
        return documentService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        documentService.delete(authentication.getName(), id);
    }
}
