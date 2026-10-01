package com.realestate.realestatebackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.realestate.realestatebackend.entity.Property;

public interface PropertyRepository extends JpaRepository<Property, Long> {

}