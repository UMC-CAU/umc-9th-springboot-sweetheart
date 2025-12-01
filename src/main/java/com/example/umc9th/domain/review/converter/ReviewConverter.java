package com.example.umc9th.domain.review.converter;

import com.example.umc9th.domain.review.dto.ReviewResponse;
import com.example.umc9th.domain.review.entity.Review;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public class ReviewConverter {

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
}
