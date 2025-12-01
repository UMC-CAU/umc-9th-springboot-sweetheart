package com.example.umc9th.domain.mission.repository;

import com.example.umc9th.domain.mission.entity.Mission;
import com.example.umc9th.domain.store.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionRepository extends JpaRepository<Mission, Long> {

  /**
   * 가게의 미션 목록 조회 (Page 방식)
   * COUNT 쿼리를 실행하여 전체 개수 제공
   *
   * @EntityGraph로 Store를 fetch join하여 N+1 문제 예방
   */
  @EntityGraph(attributePaths = {"store"})
  Page<Mission> findAllByStore(Store store, Pageable pageable);

  /**
   * 가게의 미션 목록 조회 (Slice 방식)
   * COUNT 쿼리를 실행하지 않아 성능 우수
   * 무한 스크롤에 적합
   *
   * @EntityGraph로 Store를 fetch join하여 N+1 문제 예방
   */
  @EntityGraph(attributePaths = {"store"})
  Slice<Mission> findSliceByStore(Store store, Pageable pageable);
}
