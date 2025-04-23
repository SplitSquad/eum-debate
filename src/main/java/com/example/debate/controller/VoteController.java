package com.example.debate.controller;

import com.example.debate.dto.VoteReqDto;
import com.example.debate.entity.Vote;
import com.example.debate.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/debate/vote")
public class VoteController {
    private final VoteService voteService;

    @PostMapping
    public ResponseEntity<?> reactToVote(@RequestHeader("Authorization") String token,
                                         @RequestBody VoteReqDto voteReqDto) {
        return voteService.reactToVote(token, voteReqDto);
    }

    @GetMapping
    public ResponseEntity<?> getVotes(@RequestBody VoteReqDto voteReqDto) {
        return voteService.getVotes(voteReqDto);
    }
}
