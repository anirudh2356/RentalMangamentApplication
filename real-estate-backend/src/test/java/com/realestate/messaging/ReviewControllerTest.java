package com.realestate.messaging;

import com.realestate.messaging.dto.CreateReviewRequest;
import com.realestate.messaging.dto.ReviewDto;
import com.realestate.messaging.model.Review;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.ReviewRepository;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.messaging.web.ReviewController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewControllerTest {

    private final ReviewRepository reviewRepository = org.mockito.Mockito.mock(ReviewRepository.class);
    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
    private ReviewController controller;

    @BeforeEach
    void setUp() {
        controller = new ReviewController(reviewRepository, userRepository, 1L);
    }

    @Test
    void reviewSummaryUsesValidRatingsAndReturnsZeroForEmptyProperties() {
        Review review = review(10L, 7L, 1L, 5, "Well maintained");
        when(reviewRepository.findByPropertyIdAndRatingBetweenOrderByCreatedAtDesc(7L, 1, 5))
                .thenReturn(List.of(review));
        when(reviewRepository.findAverageValidRating(7L)).thenReturn(4.5);
        when(reviewRepository.countByPropertyIdAndRatingBetween(7L, 1, 5)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User(1L, "Alice")));

        var result = controller.listReviews(7L);

        assertEquals(4.5, (Double) result.get("averageRating"), 0.001);
        assertEquals(1L, result.get("count"));
        ReviewDto dto = (ReviewDto) ((List<?>) result.get("reviews")).get(0);
        assertEquals("Alice", dto.getUserName());
        assertEquals(5, dto.getRating());

        when(reviewRepository.findByPropertyIdAndRatingBetweenOrderByCreatedAtDesc(8L, 1, 5))
                .thenReturn(List.of());
        when(reviewRepository.findAverageValidRating(8L)).thenReturn(null);
        when(reviewRepository.countByPropertyIdAndRatingBetween(8L, 1, 5)).thenReturn(0L);
        var emptyResult = controller.listReviews(8L);
        assertEquals(0.0, (Double) emptyResult.get("averageRating"), 0.001);
        assertEquals(0L, emptyResult.get("count"));
    }

    @Test
    void createUpdateAndDeleteReviewForOwner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User(1L, "Alice")));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        ReviewDto created = controller.createReview(7L, new CreateReviewRequest(4, "  Good location  "));
        assertEquals(11L, created.getId());
        assertEquals("Good location", created.getComment());
        assertEquals(1L, created.getUserId());

        Review existing = review(11L, 7L, 1L, 4, "Good location");
        when(reviewRepository.findById(11L)).thenReturn(Optional.of(existing));
        ReviewDto updated = controller.updateReview(7L, 11L, new CreateReviewRequest(5, "Updated"));
        assertEquals(5, updated.getRating());
        assertEquals("Updated", updated.getComment());

        controller.deleteReview(7L, 11L);
        verify(reviewRepository).delete(existing);
    }

    @Test
    void validatesRatingCommentAndLengthBeforePersistence() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(
                ResponseStatusException.class,
                () -> controller.createReview(7L, new CreateReviewRequest(0, "Comment"))
        ).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(
                ResponseStatusException.class,
                () -> controller.createReview(7L, new CreateReviewRequest(5, "  "))
        ).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(
                ResponseStatusException.class,
                () -> controller.createReview(7L, new CreateReviewRequest(5, "x".repeat(1001)))
        ).getStatusCode());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void updateAndDeleteRejectAnotherUsersReview() {
        Review otherUsersReview = review(12L, 7L, 2L, 3, "Not yours");
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(otherUsersReview));

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(
                ResponseStatusException.class,
                () -> controller.updateReview(7L, 12L, new CreateReviewRequest(4, "Edit"))
        ).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(
                ResponseStatusException.class,
                () -> controller.deleteReview(7L, 12L)
        ).getStatusCode());
        verify(reviewRepository, never()).delete(otherUsersReview);
    }

    private Review review(Long id, Long propertyId, Long userId, int rating, String comment) {
        Review review = new Review();
        review.setId(id);
        review.setPropertyId(propertyId);
        review.setUserId(userId);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(Instant.parse("2026-09-30T09:00:00Z"));
        return review;
    }
}