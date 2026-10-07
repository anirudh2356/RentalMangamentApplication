package com.realestate.documents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            ".pdf", "application/pdf",
            ".doc", "application/msword",
            ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png"
    );

    private final Path storageRoot;
    private final long maxFileSize;

    public DocumentStorageService(
            @Value("${app.documents.storage-path:uploads/documents}") String storagePath,
            @Value("${app.documents.max-file-size:10485760}") long maxFileSize
    ) {
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize > 0 ? maxFileSize : DEFAULT_MAX_FILE_SIZE;
    }

    public StoredDocument store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a non-empty document file");
        }
        if (file.getSize() > maxFileSize) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Document exceeds the maximum file size");
        }

        String originalFileName = safeOriginalFileName(file.getOriginalFilename());
        String extension = extensionOf(originalFileName);
        String contentType = ALLOWED_TYPES.get(extension);
        if (contentType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported document file type");
        }
        String submittedType = file.getContentType();
        if (submittedType != null && !submittedType.isBlank()
                && !"application/octet-stream".equalsIgnoreCase(submittedType)
                && !contentType.equalsIgnoreCase(submittedType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File extension and content type do not match");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new DocumentStorageException("Unable to read uploaded document", exception);
        }
        if (bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a non-empty document file");
        }
        if (bytes.length > maxFileSize) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Document exceeds the maximum file size");
        }
        if (!hasExpectedSignature(bytes, extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file content is not valid for its extension");
        }

        String storageKey = UUID.randomUUID() + extension;
        Path target = resolveStorageKey(storageKey);
        try {
            Files.createDirectories(storageRoot);
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new DocumentStorageException("Unable to store uploaded document", exception);
        }
        return new StoredDocument(storageKey, originalFileName, contentType, bytes.length);
    }

    public Path resolveStoredFile(String storageKey) {
        return resolveStorageKey(storageKey);
    }

    public void delete(String storageKey) {
        Path target = resolveStorageKey(storageKey);
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new DocumentStorageException("Unable to delete stored document", exception);
        }
    }

    private Path resolveStorageKey(String storageKey) {
        if (storageKey == null || !storageKey.matches(
                "[0-9a-fA-F-]{36}\\.(pdf|doc|docx|jpg|jpeg|png)"
        )) {
            throw new DocumentStorageException("Stored document reference is invalid", null);
        }
        Path resolved = storageRoot.resolve(storageKey).normalize();
        if (!resolved.startsWith(storageRoot) || !storageRoot.equals(resolved.getParent())) {
            throw new DocumentStorageException("Stored document reference is invalid", null);
        }
        return resolved;
    }

    private String safeOriginalFileName(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A document filename is required");
        }
        String basename = filename.replace('\\', '/');
        basename = basename.substring(basename.lastIndexOf('/') + 1);
        if (basename.isBlank() || basename.length() > 255
                || basename.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document filename is invalid");
        }
        return basename;
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 1 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    private boolean hasExpectedSignature(byte[] bytes, String extension) {
        return switch (extension) {
            case ".pdf" -> startsWith(bytes, new byte[]{'%', 'P', 'D', 'F', '-'});
            case ".jpg", ".jpeg" -> startsWith(bytes, new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
            case ".png" -> startsWith(bytes, new byte[]{
                    (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10
            });
            case ".doc" -> startsWith(bytes, new byte[]{
                    (byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0,
                    (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1
            });
            case ".docx" -> startsWith(bytes, new byte[]{'P', 'K', 3, 4});
            default -> false;
        };
    }

    private boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (bytes[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    public record StoredDocument(String storageKey, String originalFileName, String contentType, long fileSize) {
    }
}
