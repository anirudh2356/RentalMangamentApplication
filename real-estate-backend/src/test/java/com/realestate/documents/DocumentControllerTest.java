package com.realestate.documents;

import com.realestate.documents.dto.DocumentResponse;
import com.realestate.documents.dto.DocumentUpdateRequest;
import com.realestate.documents.model.DocumentType;
import com.realestate.messaging.config.SecurityConfig;
import com.realestate.messaging.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, DocumentExceptionHandler.class})
class DocumentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private DocumentService documentService;
    @MockitoBean private UserRepository userRepository;
    @TempDir Path temporaryDirectory;

    @Test
    void authenticatedBrokerCanUploadWithCsrfAndDoesNotExposeInternalPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "agreement.pdf", "application/pdf", "%PDF-1.7".getBytes());
        when(documentService.upload(
                eq("broker@example.local"), any(), eq(DocumentType.AGREEMENT), eq("Purchase"), eq(5L), eq(6L), eq(7L)
        )).thenReturn(response());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("documentType", "AGREEMENT")
                        .param("description", "Purchase")
                        .param("propertyId", "5")
                        .param("leadId", "6")
                        .param("dealId", "7")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(14))
                .andExpect(jsonPath("$.storagePath").doesNotExist())
                .andExpect(jsonPath("$.storageKey").doesNotExist())
                .andExpect(jsonPath("$.broker").doesNotExist());

        verify(documentService).upload(
                eq("broker@example.local"), any(), eq(DocumentType.AGREEMENT), eq("Purchase"), eq(5L), eq(6L), eq(7L)
        );
    }

    @Test
    void documentWritesRequireCsrf() throws Exception {
        mockMvc.perform(post("/api/documents")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/documents/14")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/documents/14")
                        .with(user("broker@example.local").roles("BROKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentType":"AGREEMENT"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedAndNonBrokerAccessAreRejected() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/documents")
                        .with(user("buyer@example.local").roles("BUYER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void brokerListUsesAuthenticatedPrincipalAndAcceptsFilters() throws Exception {
        when(documentService.list("broker@example.local", "lease", DocumentType.AGREEMENT, 5L, null, null))
                .thenReturn(List.of(response()));

        mockMvc.perform(get("/api/documents")
                        .param("search", "lease")
                        .param("documentType", "AGREEMENT")
                        .param("propertyId", "5")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalFileName").value("agreement.pdf"));

        verify(documentService).list("broker@example.local", "lease", DocumentType.AGREEMENT, 5L, null, null);
    }

    @Test
    void brokerCanUpdateMetadataWithCsrf() throws Exception {
        when(documentService.update(eq("broker@example.local"), eq(14L), any(DocumentUpdateRequest.class)))
                .thenReturn(response());

        mockMvc.perform(put("/api/documents/14")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentType":"AGREEMENT","description":"Updated","propertyId":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("AGREEMENT"));

        verify(documentService).update(eq("broker@example.local"), eq(14L), any(DocumentUpdateRequest.class));
    }

    @Test
    void brokerCanDownloadOwnDocumentThroughApi() throws Exception {
        Path storedFile = temporaryDirectory.resolve("stored.pdf");
        Files.writeString(storedFile, "%PDF-1.7 test");
        when(documentService.download("broker@example.local", 14L)).thenReturn(
                new DocumentService.DocumentDownload(
                        storedFile,
                        "agreement.pdf",
                        "application/pdf",
                        Files.size(storedFile)
                )
        );

        mockMvc.perform(get("/api/documents/14/download")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("agreement.pdf")));
    }

    private static DocumentResponse response() {
        return new DocumentResponse(
                14L, "agreement.pdf", "application/pdf", 24L, DocumentType.AGREEMENT,
                "Purchase", 5L, "House", 6L, "Buyer", 7L, "Buyer · House",
                Instant.parse("2026-10-07T12:00:00Z"), Instant.parse("2026-10-07T12:00:00Z")
        );
    }
}
