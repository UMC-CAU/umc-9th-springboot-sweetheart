-- 리뷰 등록 SQL
  INSERT INTO REVIEWS (
      user_id,
      store_id,
      order_id,
      rating,
      content,
      is_public,
      created_at,
      updated_at
  ) VALUES (?, ?, ?, ?, ?, true, NOW(), NOW());
  
  -- 리뷰 이미지 등록 (다중 이미지 지원)
  INSERT INTO REVIEW_IMAGES (
      review_id,
      image_url,
      image_order,
      created_at
  ) VALUES (?, ?, ?, NOW());