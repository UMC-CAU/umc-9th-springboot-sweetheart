-- 마이페이지 기본 정보 조회
  SELECT
      name,
      email,
      phone,
      points
  FROM USERS
  WHERE id = ?;
  
  -- 생각해보니까 제가 휴대폰 인증여부를
  -- 테이블에서 안만들었네요..