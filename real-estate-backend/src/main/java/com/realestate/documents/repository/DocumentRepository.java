package com.realestate.documents.repository;

import com.realestate.documents.model.Document;
import com.realestate.documents.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("""
            select d from Document d
            where d.broker.id = :brokerId
              and (lower(d.originalFileName) like lower(concat('%', :search, '%'))
                   or lower(coalesce(d.description, '')) like lower(concat('%', :search, '%')))
            order by d.uploadedAt desc
            """)
    List<Document> searchOwnedForWorkspace(
            @Param("brokerId") Long brokerId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            select d from Document d
            left join fetch d.property
            left join fetch d.lead
            left join fetch d.deal
            where d.broker.id = :brokerId
              and (:search is null or
                   lower(d.originalFileName) like lower(concat('%', :search, '%')) or
                   lower(coalesce(d.description, '')) like lower(concat('%', :search, '%')))
              and (:documentType is null or d.documentType = :documentType)
              and (:propertyId is null or d.property.id = :propertyId)
              and (:leadId is null or d.lead.id = :leadId)
              and (:dealId is null or d.deal.id = :dealId)
            order by d.uploadedAt desc
            """)
    List<Document> searchOwned(
            @Param("brokerId") Long brokerId,
            @Param("search") String search,
            @Param("documentType") DocumentType documentType,
            @Param("propertyId") Long propertyId,
            @Param("leadId") Long leadId,
            @Param("dealId") Long dealId
    );

    @Query("""
            select d from Document d
            left join fetch d.property
            left join fetch d.lead
            left join fetch d.deal
            where d.id = :id and d.broker.id = :brokerId
            """)
    Optional<Document> findOwnedById(@Param("id") Long id, @Param("brokerId") Long brokerId);
}
