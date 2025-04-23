package com.example.debate.controller;

import com.example.debate.dto.CommentReqDto;
import com.example.debate.service.CommentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/debate/comment")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<?> addComment(@RequestHeader("Authorization") String token,
                                        @RequestBody CommentReqDto commentReqDto) {
        return commentService.addComment(token, commentReqDto);
    }

    @GetMapping
    public ResponseEntity<?> getComments(@RequestHeader("Authorization") String token, long debateId,
                                         String sort, int page, int size){
        return commentService.getComments(token, debateId, sort, page, size);
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<?> updateComment(@RequestHeader("Authorization") String token,
                                           @PathVariable long commentId,
                                           @RequestBody  CommentReqDto commentReqDto){
        return commentService.updateComment(token, commentId, commentReqDto);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@RequestHeader("Authorization") String token,
                                           @PathVariable long commentId){
        return commentService.deleteComment(token, commentId);
    }

    @PostMapping("/{commentId}")
    public ResponseEntity<?> reactToComment(@RequestHeader("Authorization") String token,
                                            @PathVariable long commentId,
                                            @RequestBody CommentReqDto commentReqDto){
        return  commentService.reactToComment(token, commentId, commentReqDto);
    }
}
