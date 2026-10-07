package com.realestate.deals.repository;

import com.realestate.deals.model.Deal;
import com.realestate.deals.model.DealStage;
import com.realestate.deals.model.DealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface DealRepository extends JpaRepository<Deal, Long> {

    @Query("""
            select d from Deal d
            join fetch d.lead
            join fetch d.property
            where d.broker.id = :brokerId
              and (lower(d.lead.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(d.lead.email, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(d.lead.phone, '')) like lower(concat('%', :search, '%'))
                   or lower(d.property.title) like lower(concat('%', :search, '%'))
                   or lower(d.property.location) like lower(concat('%', :search, '%'))
                   or lower(d.property.city) like lower(concat('%', :search, '%'))
                   or lower(d.property.state) like lower(concat('%', :search, '%')))
            order by d.createdAt desc
            """)
    List<Deal> searchOwnedForWorkspace(
            @Param("brokerId") Long brokerId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            select d from Deal d
            join fetch d.lead
            join fetch d.property
            where d.broker.id = :brokerId
              and (:stage is null or d.stage = :stage)
              and (:status is null or d.status = :status)
              and (:search is null or
                   lower(d.lead.name) like lower(concat('%', :search, '%')) or
                   lower(d.property.title) like lower(concat('%', :search, '%')) or
                   lower(d.property.city) like lower(concat('%', :search, '%')))
            order by d.createdAt desc
            """)
    List<Deal> searchOwned(
            @Param("brokerId") Long brokerId,
            @Param("search") String search,
            @Param("stage") DealStage stage,
            @Param("status") DealStatus status
    );

    @Query("""
            select d from Deal d
            join fetch d.lead
            join fetch d.property
            where d.id = :id and d.broker.id = :brokerId
            """)
    Optional<Deal> findOwnedById(@Param("id") Long id, @Param("brokerId") Long brokerId);

    long countByBrokerIdAndStatus(Long brokerId, DealStatus status);

    @Query("""
            select d from Deal d
            join fetch d.lead
            join fetch d.property
            where d.broker.id = :brokerId and d.status = :status
            order by d.updatedAt desc
            """)
    List<Deal> findOwnedByStatus(
            @Param("brokerId") Long brokerId,
            @Param("status") DealStatus status
    );

    long countByBrokerIdAndStage(Long brokerId, DealStage stage);

    @Query("select coalesce(sum(d.dealValue), 0) from Deal d where d.broker.id = :brokerId")
    BigDecimal sumDealValueByBrokerId(@Param("brokerId") Long brokerId);

    @Query("""
            select coalesce(sum(d.commissionAmount), 0) from Deal d
            where d.broker.id = :brokerId and d.status = :status
            """)
    BigDecimal sumCommissionByBrokerIdAndStatus(
            @Param("brokerId") Long brokerId,
            @Param("status") DealStatus status
    );
}
