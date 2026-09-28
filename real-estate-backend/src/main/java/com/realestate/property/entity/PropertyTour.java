package com.realestate.property.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "property_tours")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyTour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long propertyId;   // unique = only one row per property

    private String videoUrl;
    private String virtualTourUrl;
}