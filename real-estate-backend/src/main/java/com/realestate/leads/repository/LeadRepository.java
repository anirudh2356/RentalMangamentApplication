package com.realestate.leads.repository;

import com.realestate.leads.model.Lead;
import com.realestate.leads.model.LeadSource;
import com.realestate.leads.model.LeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    @Query("""
            select l from Lead l
            left join fetch l.interestedProperty
            where l.broker.id = :brokerId
              and (lower(l.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(l.email, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(l.phone, '')) like lower(concat('%', :search, '%')))
            order by l.createdAt desc
            """)
    List<Lead> searchOwnedForWorkspace(
            @Param("brokerId") Long brokerId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            select l from Lead l
            left join fetch l.interestedProperty
            where l.broker.id = :brokerId
              and (:status is null or l.status = :status)
              and (:source is null or l.source = :source)
              and (:propertyId is null or l.interestedProperty.id = :propertyId)
              and (:search is null or
                   lower(l.name) like lower(concat('%', :search, '%')) or
                   lower(coalesce(l.email, '')) like lower(concat('%', :search, '%')) or
                   lower(coalesce(l.phone, '')) like lower(concat('%', :search, '%')))
            order by l.createdAt desc
            """)
    List<Lead> searchOwned(
            @Param("brokerId") Long brokerId,
            @Param("search") String search,
            @Param("status") LeadStatus status,
            @Param("source") LeadSource source,
            @Param("propertyId") Long propertyId
    );

    @Query("""
            select l from Lead l
            left join fetch l.interestedProperty
            where l.id = :id and l.broker.id = :brokerId
            """)
    Optional<Lead> findOwnedById(@Param("id") Long id, @Param("brokerId") Long brokerId);

    long countByBrokerId(Long brokerId);

    long countByBrokerIdAndStatus(Long brokerId, LeadStatus status);
}
