import { useEffect, useState } from 'react';
import {
  fetchPropertyReviews,
  postPropertyReview,
  updatePropertyReview,
  deletePropertyReview,
} from './api';
import './App.css';

function formatTimestamp(iso) {
  if (!iso) return '';

  const date = new Date(iso);

  return date.toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

function Stars({ value, large = false }) {
  const rating = Math.max(0, Math.min(5, Number(value) || 0));

  return (
    <span className={large ? 'review-stars large' : 'review-stars'}>
      {[1, 2, 3, 4, 5].map((star) => (
        <span key={star} className={star <= rating ? 'filled' : ''}>
          ★
        </span>
      ))}
    </span>
  );
}

function StarSelector({ value, onChange }) {
  return (
    <div className="star-selector" aria-label="Select rating">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          className={star <= value ? 'selected' : ''}
          onClick={() => onChange(star)}
          aria-label={`${star} star${star > 1 ? 's' : ''}`}
        >
          ★
        </button>
      ))}
    </div>
  );
}

function getInitial(name) {
  if (!name) return '?';
  return name.trim().charAt(0).toUpperCase();
}

function RatingDistribution({ reviews }) {
  const total = reviews.length;

  return (
    <div className="rating-distribution">
      {[5, 4, 3, 2, 1].map((rating) => {
        const count = reviews.filter(
          (review) => Number(review.rating) === rating
        ).length;

        const percentage = total
          ? Math.round((count / total) * 100)
          : 0;

        return (
          <div className="rating-row" key={rating}>
            <span className="rating-number">{rating}</span>
            <span className="rating-star">★</span>

            <div className="rating-bar">
              <div
                className="rating-bar-fill"
                style={{ width: `${percentage}%` }}
              />
            </div>

            <span className="rating-count">{count}</span>
          </div>
        );
      })}
    </div>
  );
}

export default function Reviews() {
  const [propertyId, setPropertyId] = useState('1');
  const [data, setData] = useState(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // New review
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Edit review
  const [editingReviewId, setEditingReviewId] = useState(null);
  const [editRating, setEditRating] = useState(5);
  const [editComment, setEditComment] = useState('');
  const [updating, setUpdating] = useState(false);
  const [deletingReviewId, setDeletingReviewId] = useState(null);

  useEffect(() => {
    load(propertyId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load(id) {
    if (!id) return;

    setLoading(true);
    setError('');

    try {
      const result = await fetchPropertyReviews(Number(id));
      setData(result);
    } catch (e) {
      setData(null);
      setError(e.message || 'Failed to load reviews');
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();

    setError('');
    setSuccessMsg('');

    if (rating < 1 || rating > 5) {
      setError('Rating must be between 1 and 5');
      return;
    }

    if (!comment.trim()) {
      setError('Please write a review before submitting.');
      return;
    }

    setSubmitting(true);

    try {
      await postPropertyReview(
        Number(propertyId),
        rating,
        comment.trim()
      );

      setSuccessMsg('Your review has been published.');
      setComment('');
      setRating(5);

      await load(propertyId);
    } catch (e) {
      setError(e.message || 'Failed to submit review');
    } finally {
      setSubmitting(false);
    }
  }

  function startEditing(review) {
    setEditingReviewId(review.id);
    setEditRating(review.rating);
    setEditComment(review.comment);
    setError('');
    setSuccessMsg('');
  }

  function cancelEditing() {
    setEditingReviewId(null);
    setEditRating(5);
    setEditComment('');
  }

  async function handleUpdate(reviewId) {
    setError('');
    setSuccessMsg('');

    if (editRating < 1 || editRating > 5) {
      setError('Rating must be between 1 and 5');
      return;
    }

    if (!editComment.trim()) {
      setError('Comment is required');
      return;
    }

    setUpdating(true);

    try {
      await updatePropertyReview(
        Number(propertyId),
        reviewId,
        editRating,
        editComment.trim()
      );

      setSuccessMsg('Your review has been updated.');
      cancelEditing();

      await load(propertyId);
    } catch (e) {
      setError(e.message || 'Failed to update review');
    } finally {
      setUpdating(false);
    }
  }

  async function handleDelete(reviewId) {
    const confirmed = window.confirm(
      'Are you sure you want to delete this review?'
    );

    if (!confirmed) return;

    setError('');
    setSuccessMsg('');
    setDeletingReviewId(reviewId);

    try {
      await deletePropertyReview(
        Number(propertyId),
        reviewId
      );

      setSuccessMsg('Your review has been deleted.');

      if (editingReviewId === reviewId) {
        cancelEditing();
      }

      await load(propertyId);
    } catch (e) {
      setError(e.message || 'Failed to delete review');
    } finally {
      setDeletingReviewId(null);
    }
  }

  const reviews = data?.reviews || [];
  const averageRating = Number(data?.averageRating || 0).toFixed(1);

  return (
    <div className="reviews-page">

      {/* Header */}
      <div className="reviews-page-header">
        <div>
          <div className="section-eyebrow">PROPERTY EXPERIENCE</div>

          <h2>Reviews & Ratings</h2>

          <p>
            See what people are saying about this property.
          </p>
        </div>

        <div className="property-selector">
          <label htmlFor="property-id">
            Property
          </label>

          <div className="property-selector-row">
            <input
              id="property-id"
              type="number"
              min="1"
              value={propertyId}
              onChange={(e) => setPropertyId(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  load(propertyId);
                }
              }}
            />

            <button
              type="button"
              className="primary-button"
              onClick={() => load(propertyId)}
              disabled={loading}
            >
              {loading ? 'Loading...' : 'View Reviews'}
            </button>
          </div>
        </div>
      </div>

      {/* Messages */}
      {error && (
        <div className="error-banner">
          <span>!</span>
          {error}
        </div>
      )}

      {successMsg && (
        <div className="success-banner">
          <span>✓</span>
          {successMsg}
        </div>
      )}

      {/* Loading */}
      {loading && (
        <div className="reviews-loading">
          <div className="loading-spinner" />
          <p>Loading property reviews...</p>
        </div>
      )}

      {/* Content */}
      {!loading && data && (
        <>
          {/* Rating summary */}
          <section className="rating-overview">

            <div className="rating-main">
              <div className="rating-score">
                {averageRating}
              </div>

              <Stars
                value={Math.round(Number(averageRating))}
                large
              />

              <p>
                Based on {data.count} review
                {data.count === 1 ? '' : 's'}
              </p>
            </div>

            <div className="rating-divider" />

            <RatingDistribution reviews={reviews} />

            <div className="rating-trust">
              <div className="trust-icon">✓</div>

              <div>
                <strong>Community feedback</strong>
                <span>
                  Ratings help buyers make informed property decisions.
                </span>
              </div>
            </div>
          </section>

          {/* Reviews */}
          <section className="reviews-section">

            <div className="reviews-section-header">
              <div>
                <h3>What people are saying</h3>
                <p>
                  {data.count === 0
                    ? 'Be the first to share your experience.'
                    : `${data.count} people have shared their experience.`}
                </p>
              </div>
            </div>

            {reviews.length === 0 ? (
              <div className="reviews-empty">
                <div className="empty-review-icon">★</div>

                <h3>No reviews yet</h3>

                <p>
                  Be the first person to share your experience
                  with this property.
                </p>
              </div>
            ) : (
              <div className="reviews-list">

                {reviews.map((review) => (
                  <article
                    className="review-card"
                    key={review.id}
                  >

                    <div className="review-card-header">

                      <div className="review-author">

                        <div className="review-avatar">
                          {getInitial(review.userName)}
                        </div>

                        <div>
                          <strong>
                            {review.userName || 'Anonymous'}
                          </strong>

                          <span>
                            Reviewed on{' '}
                            {formatTimestamp(review.createdAt)}
                          </span>
                        </div>

                      </div>

                      {review.userId === 1 && (
                        <div className="review-menu">

                          <button
                            type="button"
                            className="secondary-button small"
                            onClick={() =>
                              startEditing(review)
                            }
                          >
                            Edit
                          </button>

                          <button
                            type="button"
                            className="danger-button small"
                            disabled={
                              deletingReviewId === review.id
                            }
                            onClick={() =>
                              handleDelete(review.id)
                            }
                          >
                            {deletingReviewId === review.id
                              ? 'Deleting...'
                              : 'Delete'}
                          </button>

                        </div>
                      )}

                    </div>

                    {editingReviewId === review.id ? (
                      <div className="edit-review-form">

                        <div className="edit-rating-row">
                          <span>Your rating</span>

                          <StarSelector
                            value={editRating}
                            onChange={setEditRating}
                          />

                          <strong>
                            {editRating}/5
                          </strong>
                        </div>

                        <textarea
                          value={editComment}
                          onChange={(e) =>
                            setEditComment(e.target.value)
                          }
                          rows={4}
                          maxLength={1000}
                          placeholder="Update your review..."
                        />

                        <div className="edit-actions">

                          <button
                            type="button"
                            className="primary-button"
                            disabled={updating}
                            onClick={() =>
                              handleUpdate(review.id)
                            }
                          >
                            {updating
                              ? 'Saving...'
                              : 'Save Changes'}
                          </button>

                          <button
                            type="button"
                            className="secondary-button"
                            disabled={updating}
                            onClick={cancelEditing}
                          >
                            Cancel
                          </button>

                        </div>

                      </div>
                    ) : (
                      <>
                        <div className="review-card-rating">
                          <Stars value={review.rating} />

                          <span>
                            {review.rating}.0
                          </span>
                        </div>

                        <p className="review-card-comment">
                          {review.comment}
                        </p>
                      </>
                    )}

                  </article>
                ))}

              </div>
            )}

          </section>

          {/* Write review */}
          <section className="write-review-card">

            <div className="write-review-header">

              <div className="write-review-icon">
                ✎
              </div>

              <div>
                <h3>Share your experience</h3>

                <p>
                  Your review can help other people
                  evaluate this property.
                </p>
              </div>

            </div>

            <form onSubmit={handleSubmit}>

              <div className="new-rating-row">

                <div>
                  <label>Your rating</label>

                  <span className="rating-helper">
                    How would you rate this property?
                  </span>
                </div>

                <div className="new-rating-control">

                  <StarSelector
                    value={rating}
                    onChange={setRating}
                  />

                  <strong>{rating}/5</strong>

                </div>

              </div>

              <div className="review-text-field">

                <label htmlFor="review-comment">
                  Your review
                </label>

                <textarea
                  id="review-comment"
                  value={comment}
                  onChange={(e) =>
                    setComment(e.target.value)
                  }
                  rows={5}
                  maxLength={1000}
                  placeholder="What did you like about this property? Share details that may help other buyers or renters..."
                />

                <span>
                  {comment.length}/1000
                </span>

              </div>

              <div className="write-review-footer">

                <p>
                  Be honest and respectful in your review.
                </p>

                <button
                  type="submit"
                  className="primary-button submit-review-button"
                  disabled={submitting}
                >
                  {submitting
                    ? 'Publishing...'
                    : 'Publish Review'}
                </button>

              </div>

            </form>

          </section>
        </>
      )}

      {!loading && !data && !error && (
        <div className="reviews-empty initial">
          <div className="empty-review-icon">⌕</div>

          <h3>Select a property</h3>

          <p>
            Enter a property ID above to view its
            reviews and ratings.
          </p>
        </div>
      )}

    </div>
  );
}