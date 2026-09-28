package com.realestate.property.repository;

import com.realestate.property.entity.PropertyTour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PropertyTourRepository extends JpaRepository<PropertyTour, Long> {

    Optional<PropertyTour> findByPropertyId(Long propertyId);
}