package com.realestate.property.repository;

import com.realestate.property.entity.Property;
import com.realestate.property.entity.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertySimilarRepository extends JpaRepository<Property, Long> {

    List<Property> findTop4ByPropertyTypeAndStatusAndIdNot(String propertyType,
                                                           PropertyStatus status,
                                                           Long id);
}