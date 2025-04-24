package com.debate.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KafkaCommentDto {
    Long receiverId;
    Long senderId;

    @Builder
    public KafkaCommentDto(Long receiverId, Long senderId) {
        this.receiverId = receiverId;
        this.senderId = senderId;
    }
}
