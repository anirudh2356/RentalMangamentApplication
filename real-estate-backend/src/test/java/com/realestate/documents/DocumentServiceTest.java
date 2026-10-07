package com.realestate.documents;

import com.realestate.deals.model.Deal;
import com.realestate.deals.repository.DealRepository;
import com.realestate.documents.dto.DocumentUpdateRequest;
import com.realestate.documents.model.Document;
import com.realestate.documents.model.DocumentType;
import com.realestate.documents.repository.DocumentRepository;
import com.realestate.leads.model.Lead;
import com.realestate.leads.repository.LeadRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.notifications.NotificationService;
import com.realestate.notifications.model.NotificationType;
import com.realestate.property.model.Property;
import com.realestate.property.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private LeadRepository leadRepository;
    @Mock private DealRepository dealRepository;
    @Mock private DocumentStorageService storageService;
    @Mock private NotificationService notificationService;

    @InjectMocks private DocumentService documentService;

    private User broker;

    @BeforeEach
    void setUp() {
        broker = new User();
        broker.setId(45L);
        broker.setEmail("broker@example.local");
        broker.setRole(Role.BROKER);
        broker.setEnabled(true);
        when(userRepository.findByEmailIgnoreCase("broker@example.local")).thenReturn(Optional.of(broker));
    }

    @Test
    void uploadSetsAuthenticatedBrokerAndReturnsNoInternalStorageReference() {
        MockMultipartFile upload = validUpload();
        when(storageService.store(upload)).thenReturn(new DocumentStorageService.StoredDocument(
                "2b001888-f0b9-4812-b5d9-34717a1b32e8.pdf",
                "contract.pdf",
                "application/pdf",
                upload.getSize()
        ));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(12L);
            return document;
        });

        var response = documentService.upload(
                "broker@example.local", upload, DocumentType.AGREEMENT, "Signed copy", null, null, null
        );

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        assertEquals(broker, captor.getValue().getBroker());
        assertEquals("2b001888-f0b9-4812-b5d9-34717a1b32e8.pdf", captor.getValue().getStorageKey());
        assertEquals("contract.pdf", response.originalFileName());
        assertEquals("Signed copy", response.description());
        assertNull(response.propertyId());
        assertEquals(12L, response.id());
        verify(notificationService).createForBroker(
                broker,
                NotificationType.DOCUMENT,
                "Document uploaded",
                "A document was uploaded: contract.pdf",
                "DOCUMENT",
                12L
        );
    }

    @Test
    void listAndDetailQueriesAlwaysUseAuthenticatedBrokerScope() {
        when(documentRepository.searchOwned(45L, null, null, null, null, null)).thenReturn(java.util.List.of());
        documentService.list("broker@example.local", null, null, null, null, null);

        when(documentRepository.findOwnedById(77L, 45L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> documentService.get("broker@example.local", 77L));
        verify(documentRepository).searchOwned(45L, null, null, null, null, null);
        verify(documentRepository).findOwnedById(77L, 45L);
    }

    @Test
    void anotherBrokersDocumentCannotBeDownloadedUpdatedOrDeleted() {
        when(documentRepository.findOwnedById(91L, 45L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> documentService.download("broker@example.local", 91L));
        assertThrows(ResponseStatusException.class, () -> documentService.update(
                "broker@example.local",
                91L,
                new DocumentUpdateRequest(DocumentType.OTHER, "changed", null, null, null)
        ));
        assertThrows(ResponseStatusException.class, () -> documentService.delete("broker@example.local", 91L));

        verify(documentRepository, org.mockito.Mockito.times(3)).findOwnedById(91L, 45L);
        verify(documentRepository, never()).delete(any(Document.class));
        verify(storageService, never()).delete(any());
    }

    @Test
    void refusesForeignPropertyBeforeWritingAnyFile() {
        when(propertyRepository.findByIdAndBrokerId(88L, 45L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> documentService.upload(
                "broker@example.local", validUpload(), DocumentType.PROPERTY_DOCUMENT,
                null, 88L, null, null
        ));

        verify(storageService, never()).store(any());
        verify(propertyRepository).findByIdAndBrokerId(88L, 45L);
    }

    @Test
    void refusesForeignLeadAndDealWhenUpdatingMetadata() {
        Document existing = new Document();
        existing.setId(12L);
        existing.setBroker(broker);
        when(documentRepository.findOwnedById(12L, 45L)).thenReturn(Optional.of(existing));
        when(leadRepository.findOwnedById(91L, 45L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> documentService.update(
                "broker@example.local",
                12L,
                new DocumentUpdateRequest(DocumentType.OTHER, "notes", null, 91L, null)
        ));
        verify(documentRepository, never()).save(any());
        verify(leadRepository).findOwnedById(91L, 45L);

        when(dealRepository.findOwnedById(92L, 45L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> documentService.upload(
                "broker@example.local", validUpload(), DocumentType.DEAL_DOCUMENT,
                null, null, null, 92L
        ));
        verify(storageService, never()).store(any());
        verify(dealRepository).findOwnedById(92L, 45L);
    }

    @Test
    void rejectsNonBrokerAndDisabledBroker() {
        broker.setRole(Role.BUYER);
        assertThrows(ResponseStatusException.class, () -> documentService.list(
                "broker@example.local", null, null, null, null, null
        ));

        broker.setRole(Role.BROKER);
        broker.setEnabled(false);
        assertThrows(ResponseStatusException.class, () -> documentService.list(
                "broker@example.local", null, null, null, null, null
        ));
    }

    private MockMultipartFile validUpload() {
        return new MockMultipartFile(
                "file", "contract.pdf", "application/pdf", "%PDF-1.7 test file".getBytes()
        );
    }
}
