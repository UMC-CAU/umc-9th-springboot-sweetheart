SELECT
      m.id AS cursor_id,
      s.name AS store_name,
      m.description,
      m.reward_points AS points,
      DATEDIFF(m.end_date, NOW()) AS days_left,
      m.end_date
  FROM MISSIONS m
  INNER JOIN STORES s ON m.store_id = s.id
  LEFT JOIN USER_MISSIONS um ON um.mission_id = m.id
                              AND um.user_id = ?
  WHERE s.region_id = ?  -- 선택된 지역
    AND m.status = '진행가능'  -- 진행가능 활성 미션만
    AND m.end_date > NOW()  -- 마감 전
    AND um.id IS NULL  -- 아직 참여 안한 미션
    AND m.id < ?  -- 커서 (첫 요청시 생략)
  ORDER BY m.id DESC
  LIMIT 10;