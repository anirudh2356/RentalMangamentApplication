package com.realestate.documents;

import com.realestate.deals.model.Deal;
import com.realestate.deals.repository.DealRepository;
import com.realestate.documents.dto.DocumentResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;

@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final LeadRepository leadRepository;
    private final DealRepository dealRepository;
    private final DocumentStorageService storageService;
    private final NotificationService notificationService;

    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            PropertyRepository propertyRepository,
            LeadRepository leadRepository,
            DealRepository dealRepository,
            DocumentStorageService storageService,
            NotificationService notificationService
    ) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.leadRepository = leadRepository;
        this.dealRepository = dealRepository;
        this.storageService = storageService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(
            String email,
            String search,
            DocumentType documentType,
            Long propertyId,
            Long leadId,
            Long dealId
    ) {
        User broker = requireBroker(email);
        String cleanSearch = search == null || search.isBlank() ? null : search.trim();
        return documentRepository.searchOwned(
                        broker.getId(), cleanSearch, documentType, propertyId, leadId, dealId
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DocumentResponse upload(
            String email,
            MultipartFile file,
            DocumentType documentType,
            String description,
            Long propertyId,
            Long leadId,
            Long dealId
    ) {
        User broker = requireBroker(email);
        if (documentType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document type is required");
        }

        Property property = findOwnedProperty(propertyId, broker.getId());
        Lead lead = findOwnedLead(leadId, broker.getId());
        Deal deal = findOwnedDeal(dealId, broker.getId());
        String cleanDescription = cleanDescription(description);
        DocumentStorageService.StoredDocument stored = storageService.store(file);
        Document document = new Document();
        document.setFileName(stored.storageKey());
        document.setStorageKey(stored.storageKey());
        document.setOriginalFileName(stored.originalFileName());
        document.setContentType(stored.contentType());
        document.setFileSize(stored.fileSize());
        document.setDocumentType(documentType);
        document.setDescription(cleanDescription);
        document.setProperty(property);
        document.setLead(lead);
        document.setDeal(deal);
        document.setBroker(broker);

        try {
            Document saved = documentRepository.save(document);
            notificationService.createForBroker(
                    broker,
                    NotificationType.DOCUMENT,
                    "Document uploaded",
                    "A document was uploaded: " + saved.getOriginalFileName(),
                    "DOCUMENT",
                    saved.getId()
            );
            return toResponse(saved);
        } catch (RuntimeException exception) {
            try {
                storageService.delete(stored.storageKey());
            } catch (RuntimeException cleanupException) {
                logger.error("Could not clean up document after database save failed", cleanupException);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(String email, Long id) {
        User broker = requireBroker(email);
        return toResponse(findOwnedDocument(id, broker.getId()));
    }

    @Transactional(readOnly = true)
    public DocumentDownload download(String email, Long id) {
        User broker = requireBroker(email);
        Document document = findOwnedDocument(id, broker.getId());
        Path path = storageService.resolveStoredFile(document.getStorageKey());
        if (!path.toFile().isFile()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored document file not found");
        }
        return new DocumentDownload(path, document.getOriginalFileName(), document.getContentType(), document.getFileSize());
    }

    @Transactional
    public DocumentResponse update(String email, Long id, DocumentUpdateRequest request) {
        User broker = requireBroker(email);
        Document document = findOwnedDocument(id, broker.getId());
        document.setDocumentType(request.documentType());
        document.setDescription(cleanDescription(request.description()));
        document.setProperty(findOwnedProperty(request.propertyId(), broker.getId()));
        document.setLead(findOwnedLead(request.leadId(), broker.getId()));
        document.setDeal(findOwnedDeal(request.dealId(), broker.getId()));
        return toResponse(documentRepository.save(document));
    }

    @Transactional
    public void delete(String email, Long id) {
        User broker = requireBroker(email);
        Document document = findOwnedDocument(id, broker.getId());
        storageService.delete(document.getStorageKey());
        documentRepository.delete(document);
    }

    private User requireBroker(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required"));
        if (user.getRole() != Role.BROKER || !user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required");
        }
        return user;
    }

    private Document findOwnedDocument(Long id, Long brokerId) {
        return documentRepository.findOwnedById(id, brokerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    private Property findOwnedProperty(Long id, Long brokerId) {
        if (id == null) {
            return null;
        }
        return propertyRepository.findByIdAndBrokerId(id, brokerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
    }

    private Lead findOwnedLead(Long id, Long brokerId) {
        if (id == null) {
            return null;
        }
        return leadRepository.findOwnedById(id, brokerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));
    }

    private Deal findOwnedDeal(Long id, Long brokerId) {
        if (id == null) {
            return null;
        }
        return dealRepository.findOwnedById(id, brokerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deal not found"));
    }

    private String cleanDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String clean = description.trim();
        if (clean.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description must not exceed 2000 characters");
        }
        return clean;
    }

    private DocumentResponse toResponse(Document document) {
        Property property = document.getProperty();
        Lead lead = document.getLead();
        Deal deal = document.getDeal();
        String dealSummary = deal == null ? null
                : deal.getLead().getName() + " — " + deal.getProperty().getTitle();
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getDocumentType(),
                document.getDescription(),
                property == null ? null : property.getId(),
                property == null ? null : property.getTitle(),
                lead == null ? null : lead.getId(),
                lead == null ? null : lead.getName(),
                deal == null ? null : deal.getId(),
                dealSummary,
                document.getUploadedAt(),
                document.getUpdatedAt()
        );
    }

    public record DocumentDownload(Path path, String fileName, String contentType, long fileSize) {
    }
}
