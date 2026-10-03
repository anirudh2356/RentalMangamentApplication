package com.realestate.messaging.repository;

import com.realestate.messaging.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByPropertyIdAndRatingBetweenOrderByCreatedAtDesc(
            Long propertyId,
            int minimumRating,
            int maximumRating
    );

    @Query("select avg(r.rating) from Review r where r.propertyId = :propertyId and r.rating between 1 and 5")
    Double findAverageValidRating(@Param("propertyId") Long propertyId);

    long countByPropertyIdAndRatingBetween(Long propertyId, int minimumRating, int maximumRating);
}
