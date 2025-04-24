package com.debate.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CommentResDto {
    Long commentId;
    Long like;
    Long dislike;
    Long reply;
    String isState;
    String content;
    String userName;
    String createdAt;

    @Builder
    public CommentResDto(Long commentId, Long like, Long dislike, Long reply, String isState, String content, String userName, String createdAt) {
        this.commentId = commentId;
        this.like = like;
        this.dislike = dislike;
        this.reply = reply;
        this.isState = isState;
        this.content = content;
        this.userName = userName;
        this.createdAt = createdAt;
    }
}
