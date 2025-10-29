package com.example.umc9th.domain.review.repository;

import com.example.umc9th.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT DISTINCT r FROM Review r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.store " +
           "WHERE r.store.id = :storeId " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByStoreIdWithUser(@Param("storeId") Long storeId);

    @Query("SELECT r FROM Review r " +
           "JOIN FETCH r.store s " +
           "WHERE r.user.id = :memberId " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByMemberIdWithStore(@Param("memberId") Long memberId);
}
