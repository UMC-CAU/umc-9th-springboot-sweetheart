package com.example.umc9th.domain.review.repository;

import com.example.umc9th.domain.review.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

       @EntityGraph(attributePaths = { "user", "store" })
       @Query("SELECT DISTINCT r FROM Review r " +
                     "WHERE r.store.id = :storeId " +
                     "ORDER BY r.createdAt DESC")
       List<Review> findByStoreIdWithUser(@Param("storeId") Long storeId);

       @EntityGraph(attributePaths = { "store" })
       @Query("SELECT r FROM Review r " +
                     "WHERE r.user.id = :memberId " +
                     "ORDER BY r.createdAt DESC")
       Page<Review> findAllByUser(@Param("memberId") Long memberId, Pageable pageable);

       @EntityGraph(attributePaths = { "store" })
       @Query("SELECT r FROM Review r " +
                     "WHERE r.user.id = :memberId " +
                     "ORDER BY r.createdAt DESC")
       List<Review> findByMemberIdWithStore(@Param("memberId") Long memberId);
}
