-- 내가 진행중/완료한 미션 목록 조회 (페이징 포함)
  SELECT
      um.id AS cursor_id,
      m.reward_points AS points,
      um.status,
      s.name AS store_name,
      m.description,
      r.id IS NULL AS show_review_button
  FROM USER_MISSIONS um
  INNER JOIN MISSIONS m ON um.mission_id = m.id
  INNER JOIN STORES s ON m.store_id = s.id
  LEFT JOIN REVIEWS r ON r.user_id = um.user_id
                      AND r.store_id = m.store_id
  WHERE um.user_id = ?
    AND um.status IN ('진행중', '진행완료')
    AND um.id < ?  -- 커서: 이전 페이지 마지막 id (첫 요청시 생략 또는 매우 큰 값)
  ORDER BY um.id DESC
  LIMIT 10;