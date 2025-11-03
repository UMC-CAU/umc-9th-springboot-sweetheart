package com.example.umc9th.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
public abstract class BaseEntity extends BaseTimeEntity {

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /**
   * Soft delete 처리
   */
  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }

  /**
   * 삭제 여부 확인
   */
  public boolean isDeleted() {
    return this.deletedAt != null;
  }
}
