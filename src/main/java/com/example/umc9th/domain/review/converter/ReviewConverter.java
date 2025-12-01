package com.example.umc9th.domain.review.converter;

import com.example.umc9th.domain.review.dto.ReviewResponse;
import com.example.umc9th.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.stream.Collectors;

public class ReviewConverter {

  /**
   * Page를 ReviewPreViewListDTO로 변환
   * 전체 페이지 수와 전체 데이터 개수 정보 포함
   */
  public static ReviewResponse.ReviewPreViewListDTO toReviewPreViewListDTO(Page<Review> reviewPage) {
    List<ReviewResponse.MyReview> reviewList = reviewPage.stream()
        .map(ReviewResponse.MyReview::from)
        .collect(Collectors.toList());

    return ReviewResponse.ReviewPreViewListDTO.builder()
        .reviewList(reviewList)
        .listSize(reviewList.size())
        .totalPage(reviewPage.getTotalPages())
        .totalElements(reviewPage.getTotalElements())
        .isFirst(reviewPage.isFirst())
        .isLast(reviewPage.isLast())
        .build();
  }

  /**
   * Slice를 ReviewPreViewSliceDTO로 변환
   * 다음 페이지 존재 여부만 포함 (COUNT 쿼리 불필요)
   */
  public static ReviewResponse.ReviewPreViewSliceDTO toReviewPreViewSliceDTO(Slice<Review> reviewSlice) {
    List<ReviewResponse.MyReview> reviewList = reviewSlice.stream()
        .map(ReviewResponse.MyReview::from)
        .collect(Collectors.toList());

    return ReviewResponse.ReviewPreViewSliceDTO.builder()
        .reviewList(reviewList)
        .listSize(reviewList.size())
        .hasNext(reviewSlice.hasNext())
        .isFirst(reviewSlice.isFirst())
        .isLast(reviewSlice.isLast())
        .build();
  }
}
