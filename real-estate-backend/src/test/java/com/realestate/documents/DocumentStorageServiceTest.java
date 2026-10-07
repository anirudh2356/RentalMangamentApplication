package com.realestate.documents;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesOnlySafeFileKeyAndDeletesStoredContent() throws Exception {
        DocumentStorageService storage = new DocumentStorageService(temporaryDirectory.toString(), 1024);
        MockMultipartFile upload = pdfUpload("C:\\private\\purchase.pdf");

        DocumentStorageService.StoredDocument stored = storage.store(upload);
        Path saved = storage.resolveStoredFile(stored.storageKey());

        assertEquals("purchase.pdf", stored.originalFileName());
        assertEquals("application/pdf", stored.contentType());
        assertEquals(upload.getSize(), stored.fileSize());
        assertTrue(saved.startsWith(temporaryDirectory));
        assertTrue(Files.exists(saved));

        storage.delete(stored.storageKey());
        assertFalse(Files.exists(saved));
    }

    @Test
    void rejectsEmptyUnsupportedOversizedAndInvalidSignatureFiles() {
        DocumentStorageService storage = new DocumentStorageService(temporaryDirectory.toString(), 20);

        assertThrows(ResponseStatusException.class, () -> storage.store(
                new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0])
        ));
        assertThrows(ResponseStatusException.class, () -> storage.store(
                new MockMultipartFile("file", "run.exe", "application/octet-stream", new byte[]{1, 2})
        ));
        assertThrows(ResponseStatusException.class, () -> storage.store(
                new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[21])
        ));
        assertThrows(ResponseStatusException.class, () -> storage.store(
                new MockMultipartFile("file", "fake.pdf", "application/pdf", "not a pdf".getBytes())
        ));
    }

    @Test
    void rejectsMismatchBetweenExtensionAndSubmittedContentType() {
        DocumentStorageService storage = new DocumentStorageService(temporaryDirectory.toString(), 1024);
        MockMultipartFile upload = new MockMultipartFile(
                "file", "file.pdf", "image/png", "%PDF-1.7".getBytes()
        );

        assertThrows(ResponseStatusException.class, () -> storage.store(upload));
    }

    @Test
    void refusesUnsafeStorageReferences() {
        DocumentStorageService storage = new DocumentStorageService(temporaryDirectory.toString(), 1024);
        assertThrows(DocumentStorageException.class, () -> storage.resolveStoredFile("../secret.pdf"));
    }

    private MockMultipartFile pdfUpload(String filename) {
        return new MockMultipartFile(
                "file",
                filename,
                "application/pdf",
                "%PDF-1.7\nvalid test payload".getBytes()
        );
    }
}
