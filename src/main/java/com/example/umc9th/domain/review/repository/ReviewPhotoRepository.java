package com.example.umc9th.domain.review.repository;

import com.example.umc9th.domain.review.entity.ReviewPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewPhotoRepository extends JpaRepository<ReviewPhoto, Long> {

    @Query("SELECT rp FROM ReviewPhoto rp " +
           "WHERE rp.review.id = :reviewId " +
           "ORDER BY rp.id ASC")
    List<ReviewPhoto> findByReviewId(@Param("reviewId") Long reviewId);

    @Query("SELECT rp FROM ReviewPhoto rp " +
           "WHERE rp.review.id IN :reviewIds " +
           "ORDER BY rp.review.id ASC, rp.id ASC")
    List<ReviewPhoto> findByReviewIdIn(@Param("reviewIds") List<Long> reviewIds);
}
