package com.realestate.property.repository;

import com.realestate.property.entity.PropertyAmenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyAmenityRepository extends JpaRepository<PropertyAmenity, Long> {

    List<PropertyAmenity> findByPropertyId(Long propertyId);
}