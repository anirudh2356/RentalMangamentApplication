package com.realestate.property.repository;

import com.realestate.property.model.Property;
import com.realestate.property.model.PropertyStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    @Query("""
            select p from Property p
            where p.broker.id = :brokerId
              and (lower(p.title) like lower(concat('%', :search, '%'))
                   or lower(p.location) like lower(concat('%', :search, '%'))
                   or lower(p.city) like lower(concat('%', :search, '%'))
                   or lower(p.state) like lower(concat('%', :search, '%')))
            order by p.createdAt desc
            """)
    List<Property> searchOwnedForWorkspace(
            @Param("brokerId") Long brokerId,
            @Param("search") String search,
            Pageable pageable
    );

    List<Property> findByBrokerIdOrderByCreatedAtDesc(Long brokerId);

    Optional<Property> findByIdAndBrokerId(Long id, Long brokerId);

    long countByBrokerId(Long brokerId);

    long countByBrokerIdAndStatus(Long brokerId, PropertyStatus status);
}
