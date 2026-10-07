package com.realestate.sitevisit.repository;

import com.realestate.sitevisit.model.SiteVisit;
import com.realestate.sitevisit.model.SiteVisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface SiteVisitRepository extends JpaRepository<SiteVisit, Long> {

    @Query("""
            select v from SiteVisit v
            join fetch v.lead
            join fetch v.property
            where v.broker.id = :brokerId
              and (lower(v.lead.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(v.lead.email, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(v.lead.phone, '')) like lower(concat('%', :search, '%'))
                   or lower(v.property.title) like lower(concat('%', :search, '%'))
                   or lower(v.property.location) like lower(concat('%', :search, '%'))
                   or lower(v.property.city) like lower(concat('%', :search, '%'))
                   or lower(v.property.state) like lower(concat('%', :search, '%')))
            order by v.scheduledAt desc
            """)
    List<SiteVisit> searchOwnedForWorkspace(
            @Param("brokerId") Long brokerId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            select v from SiteVisit v
            join fetch v.lead
            join fetch v.property
            where v.broker.id = :brokerId
              and (:status is null or v.status = :status)
            order by v.scheduledAt asc
            """)
    List<SiteVisit> findOwnedVisits(
            @Param("brokerId") Long brokerId,
            @Param("status") SiteVisitStatus status
    );

    @Query("""
            select v from SiteVisit v
            join fetch v.lead
            join fetch v.property
            where v.id = :id and v.broker.id = :brokerId
            """)
    Optional<SiteVisit> findOwnedById(@Param("id") Long id, @Param("brokerId") Long brokerId);

    @Query("""
            select v from SiteVisit v
            join fetch v.lead
            join fetch v.property
            where v.broker.id = :brokerId
              and (:fromDate is null or v.scheduledAt >= :fromDate)
              and (:toDateExclusive is null or v.scheduledAt < :toDateExclusive)
            order by v.scheduledAt asc
            """)
    List<SiteVisit> findCalendarVisits(
            @Param("brokerId") Long brokerId,
            @Param("fromDate") Instant fromDate,
            @Param("toDateExclusive") Instant toDateExclusive
    );

    @Query("""
            select v from SiteVisit v
            join fetch v.lead
            join fetch v.property
            where v.broker.id = :brokerId
              and v.status in :statuses
              and v.scheduledAt >= CURRENT_TIMESTAMP
            order by v.scheduledAt asc
            """)
    List<SiteVisit> findUpcoming(
            @Param("brokerId") Long brokerId,
            @Param("statuses") List<SiteVisitStatus> statuses
    );

    long countByBrokerIdAndStatus(Long brokerId, SiteVisitStatus status);

    @Query("""
            select count(v) from SiteVisit v
            where v.broker.id = :brokerId
              and v.status in :statuses
              and v.scheduledAt >= CURRENT_TIMESTAMP
            """)
    long countUpcoming(
            @Param("brokerId") Long brokerId,
            @Param("statuses") List<SiteVisitStatus> statuses
    );
}
