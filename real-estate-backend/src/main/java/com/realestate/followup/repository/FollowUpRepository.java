package com.realestate.followup.repository;

import com.realestate.followup.model.FollowUp;
import com.realestate.followup.model.FollowUpPriority;
import com.realestate.followup.model.FollowUpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    @Query("""
            select f from FollowUp f
            join fetch f.lead
            where f.broker.id = :brokerId
              and (lower(f.title) like lower(concat('%', :search, '%'))
                   or lower(coalesce(f.description, '')) like lower(concat('%', :search, '%'))
                   or lower(f.lead.name) like lower(concat('%', :search, '%')))
            order by f.followUpAt asc
            """)
    List<FollowUp> searchOwnedForWorkspace(
            @Param("brokerId") Long brokerId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            select f from FollowUp f
            join fetch f.lead
            where f.broker.id = :brokerId
              and (:status is null or f.status = :status)
              and (:priority is null or f.priority = :priority)
              and (:leadId is null or f.lead.id = :leadId)
              and (:fromDate is null or f.followUpAt >= :fromDate)
              and (:toDate is null or f.followUpAt <= :toDate)
              and (:search is null or
                   lower(f.title) like lower(concat('%', :search, '%')) or
                   lower(coalesce(f.description, '')) like lower(concat('%', :search, '%')) or
                   lower(f.lead.name) like lower(concat('%', :search, '%')))
            order by f.followUpAt asc
            """)
    List<FollowUp> findOwned(
            @Param("brokerId") Long brokerId,
            @Param("status") FollowUpStatus status,
            @Param("priority") FollowUpPriority priority,
            @Param("leadId") Long leadId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("search") String search
    );

    @Query("""
            select f from FollowUp f
            join fetch f.lead
            where f.id = :id and f.broker.id = :brokerId
            """)
    Optional<FollowUp> findOwnedById(@Param("id") Long id, @Param("brokerId") Long brokerId);

    @Query("""
            select f from FollowUp f
            join fetch f.lead
            where f.broker.id = :brokerId
              and (:fromDate is null or f.followUpAt >= :fromDate)
              and (:toDateExclusive is null or f.followUpAt < :toDateExclusive)
            order by f.followUpAt asc
            """)
    List<FollowUp> findCalendarFollowUps(
            @Param("brokerId") Long brokerId,
            @Param("fromDate") Instant fromDate,
            @Param("toDateExclusive") Instant toDateExclusive
    );
}
