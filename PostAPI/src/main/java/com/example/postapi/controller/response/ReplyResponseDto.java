package com.example.postapi.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReplyResponseDto {
    private Long id;
    private String author;
    private String content;
    private Long heartCount;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}
