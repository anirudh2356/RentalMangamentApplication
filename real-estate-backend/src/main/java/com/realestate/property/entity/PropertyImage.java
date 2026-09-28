package com.realestate.property.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "property_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long propertyId;   // which property this image belongs to

    @Column(nullable = false)
    private String imageUrl;

    private Integer displayOrder;  // controls the order images appear in the gallery
}