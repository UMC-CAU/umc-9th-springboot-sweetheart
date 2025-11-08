package com.example.umc9th.domain.store.repository;

import com.example.umc9th.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Store 기본 JPA Repository
 * 간단한 CRUD와 기본 조회 기능을 제공합니다.
 */
@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    /**
     * N+1 문제 해결: Store + Location + Food를 한 번에 조회
     */
    @Query("SELECT DISTINCT s FROM Store s " +
           "LEFT JOIN FETCH s.location " +
           "LEFT JOIN FETCH s.food")
    List<Store> findAllWithDetails();

    /**
     * ID로 Store 조회 (Location, Food Fetch Join)
     */
    @Query("SELECT s FROM Store s " +
           "LEFT JOIN FETCH s.location " +
           "LEFT JOIN FETCH s.food " +
           "WHERE s.id = :id")
    Optional<Store> findByIdWithDetails(Long id);
}
