package com.realestate.property.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "property_amenities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyAmenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long propertyId;   // which property this amenity belongs to

    @Column(nullable = false)
    private String amenityName;   // e.g. "Swimming Pool", "Gym"
}