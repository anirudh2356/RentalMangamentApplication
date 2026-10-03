package com.realestate.messaging.web;

import com.realestate.messaging.dto.CreateReviewRequest;
import com.realestate.messaging.dto.ReviewDto;
import com.realestate.messaging.model.Review;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.ReviewRepository;
import com.realestate.messaging.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/properties/{propertyId}/reviews")
public class ReviewController {

        private static final int MAX_COMMENT_LENGTH = 1000;

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final Long currentUserId;

    public ReviewController(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            Long currentUserId) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.currentUserId = currentUserId;
    }

    @GetMapping
    public Map<String, Object> listReviews(@PathVariable Long propertyId) {
        List<Review> reviews =
                reviewRepository.findByPropertyIdAndRatingBetweenOrderByCreatedAtDesc(
                        propertyId,
                        1,
                        5
                );

        List<ReviewDto> dtos = reviews.stream().map(r -> {
            String name = userRepository.findById(r.getUserId())
                    .map(User::getName)
                    .orElse("Unknown");

            return new ReviewDto(
                    r.getId(),
                    r.getUserId(),
                    name,
                    r.getRating(),
                    r.getComment(),
                    r.getCreatedAt()
            );
        }).collect(Collectors.toList());

        Double avg = reviewRepository.findAverageValidRating(propertyId);
        if (avg == null) {
            avg = 0.0;
        }

        long count = reviewRepository.countByPropertyIdAndRatingBetween(propertyId, 1, 5);

        Map<String, Object> result = new HashMap<>();
        result.put("averageRating", Math.round(avg * 100.0) / 100.0);
        result.put("count", count);
        result.put("reviews", dtos);

        return result;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto createReview(
            @PathVariable Long propertyId,
            @RequestBody CreateReviewRequest req) {

        validateReview(req);

        User user = getCurrentUser();

        Review review = new Review();
        review.setPropertyId(propertyId);
        review.setUserId(currentUserId);
        review.setRating(req.getRating());
        review.setComment(req.getComment().trim());
        review.setCreatedAt(Instant.now());

        review = reviewRepository.save(review);

        return toDto(review, user.getName());
    }

    @PutMapping("/{reviewId}")
    public ReviewDto updateReview(
            @PathVariable Long propertyId,
            @PathVariable Long reviewId,
            @RequestBody CreateReviewRequest req) {

        validateReview(req);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Review not found"
                ));

        if (!review.getPropertyId().equals(propertyId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Review not found for this property"
            );
        }

        if (!review.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only update your own review"
            );
        }

        review.setRating(req.getRating());
        review.setComment(req.getComment().trim());

        review = reviewRepository.save(review);

        User user = getCurrentUser();

        return toDto(review, user.getName());
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
            @PathVariable Long propertyId,
            @PathVariable Long reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Review not found"
                ));

        if (!review.getPropertyId().equals(propertyId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Review not found for this property"
            );
        }

        if (!review.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only delete your own review"
            );
        }

        reviewRepository.delete(review);
    }

    private void validateReview(CreateReviewRequest req) {
        if (req == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Review data is required"
            );
        }

        if (req.getRating() < 1 || req.getRating() > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rating must be between 1 and 5"
            );
        }

        if (req.getComment() == null || req.getComment().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Comment is required"
            );
        }

        if (req.getComment().length() > MAX_COMMENT_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Comment must be 1000 characters or fewer"
            );
        }
    }

    private User getCurrentUser() {
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Current user not found"
                ));
    }

    private ReviewDto toDto(Review review, String userName) {
        return new ReviewDto(
                review.getId(),
                review.getUserId(),
                userName,
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}