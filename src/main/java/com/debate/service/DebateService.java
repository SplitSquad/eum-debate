package com.debate.service;

import com.debate.dto.DebateReqDto;
import com.debate.dto.DebateResDto;
import com.debate.entity.*;
import com.debate.repository.*;
import util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import util.TranslationJob;
import util.TranslationQueue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DebateService {
    private final JwtUtil jwtUtil;
    private final TranslationQueue translationQueue;

    private final UserRepository userRepository;
    private final DebateRepository debateRepository;
    private final TranslatedDebateRepository translatedDebateRepository;
    private final VoteRepository voteRepository;
    private final DebateReactionRepository debateReactionRepository;

    private Optional<User> verifyToken(String token) {    // 토큰 검증 함수
        try {
            long userId = jwtUtil.getUserId(token);
            User user = userRepository.findById(userId).orElse(null);
            if(user == null) {
                return Optional.empty();
            }
            return Optional.of(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Map<String, Double> calculateVotePercent(Long agreeCnt, Long disagreeCnt) {
        Long voteCnt = agreeCnt + disagreeCnt;
        double agreePercent = 0;
        double disagreePercent = 0;

        if (voteCnt > 0) {
            agreePercent = (double) agreeCnt * 100 / voteCnt;
            disagreePercent = (double) disagreeCnt * 100 / voteCnt;
        }

        Map<String, Double> result = new HashMap<>();
        result.put("agreePercent", agreePercent);
        result.put("disagreePercent", disagreePercent);
        return result;
    }

    private String getTopNationByDebateId(Long debateId) {
        List<Vote> voteList = voteRepository.findByDebate_DebateId(debateId);
        if (voteList.isEmpty()) return null;

        Map<String, Long> nationCount = new HashMap<>();
        for (Vote vote : voteList) {
            String nation = vote.getUser().getNation();
            nationCount.put(nation, nationCount.getOrDefault(nation, 0L) + 1);
        }

        return nationCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private DebateResDto buildDebateResDto(Debate debate, String language) {
        TranslatedDebate translatedDebate = translatedDebateRepository
                .findByDebate_DebateIdAndLanguage(debate.getDebateId(), language);

        Map<String, Double> percentMap =
                calculateVotePercent(debate.getAgreeCnt(), debate.getDisagreeCnt());

        DebateResDto dto = DebateResDto.builder()
                .title(translatedDebate.getTitle())
                .debateId(debate.getDebateId())
                .views(debate.getViews())
                .createdAt(debate.getCreatedAt())
                .voteCnt(debate.getVoteCnt())
                .agreePercent(percentMap.get("agreePercent"))
                .disagreePercent(percentMap.get("disagreePercent"))
                .commentCnt(debate.getCommentCnt())
                .category(debate.getCategory())
                .nation(getTopNationByDebateId(debate.getDebateId()))
                .build();

        return dto;
    }

    public ResponseEntity<?> write(DebateReqDto debateReqDto) {
        Debate debate = Debate.builder()
                .category(debateReqDto.getCategory())
                .views(0L)
                .agreeCnt(0L)
                .voteCnt(0L)
                .commentCnt(0L)
                .disagreeCnt(0L)
                .build();
        debateRepository.save(debate);

        translationQueue.enqueue(new TranslationJob(debate, debateReqDto, null));

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> getDebates(String token, int page, int size, String sort, String category) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        String language = user.get().getLanguage();

        Sort sortOption;
        switch (sort) {
            case "view":
                sortOption = Sort.by(Sort.Direction.DESC, "views");
                break;
            case "comment":
                sortOption = Sort.by(Sort.Direction.DESC, "commentCnt");
                break;
            case "vote":
                sortOption = Sort.by(Sort.Direction.DESC, "voteCnt");
                break;
            default:
                sortOption = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        Pageable pageable = PageRequest.of(page, size, sortOption);
        Page<Debate> debateList = debateRepository.findByCategory(category, pageable);

        long total = debateList.getTotalElements();

        List<DebateResDto> debateResDtoList = new ArrayList<>();

        for(Debate debate : debateList) {
            debateResDtoList.add(buildDebateResDto(debate, language));
        }
        return ResponseEntity.ok(Map.of(
                "debateList", debateResDtoList,
                "total", total));
    }

    public ResponseEntity<?> getTodayDebate(String token) {
        Optional<User> user = verifyToken(token);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        String language = user.get().getLanguage();

        String today = LocalDate.now().toString();
        List<Debate> todayDebateList = debateRepository.findAllByCreatedAtToday(today);

        List<DebateResDto> todayDebateResDtoList = new ArrayList<>();

        for (Debate debate : todayDebateList) {
            todayDebateResDtoList.add(buildDebateResDto(debate, language));
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        String start = LocalDateTime.now().minusDays(7).format(formatter);
        String end = LocalDateTime.now().format(formatter);

        Debate topDebate = debateRepository.findTopDebateInLastWeek(start, end);

        DebateResDto topDebateResDto = buildDebateResDto(topDebate, language);

        Debate balancedDebate = debateRepository.findMostBalancedDebateThisWeek(start, end);

        if(balancedDebate == null){
            return ResponseEntity.ok(Map.of(
                    "todayDebateList", todayDebateResDtoList,
                    "topDebate", topDebateResDto
            ));
        }

        DebateResDto balancedDebateResDto = buildDebateResDto(balancedDebate, language);

        return ResponseEntity.ok(Map.of(
                "todayDebateList", todayDebateResDtoList,
                "topDebate", topDebateResDto,
                "balancedDebate", balancedDebateResDto
        ));
    }

    public ResponseEntity<?> searchDebate(String token, int page, int size, String sort, String category,
                                          String keyword, String searchBy) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        String language = user.get().getLanguage();

        Sort sortOption;
        switch (sort) {
            case "view":
                sortOption = Sort.by(Sort.Direction.DESC, "debate.views");
                break;
            case "comment":
                sortOption = Sort.by(Sort.Direction.DESC, "debate.commentCnt");
                break;
            case "vote":
                sortOption = Sort.by(Sort.Direction.DESC, "debate.voteCnt");
                break;
            default:
                sortOption = Sort.by(Sort.Direction.DESC, "debate.createdAt");
        }
        Pageable pageable = PageRequest.of(page, size, sortOption);
        Page<TranslatedDebate> debateList;

        if(searchBy.equals("제목")) {
            debateList = translatedDebateRepository.findByCategoryAndTitle(category, keyword, language, pageable);
        }else if (searchBy.equals("내용")) {
            debateList = translatedDebateRepository.findByCategoryAndContent(category, keyword, language, pageable);
        } else {
            debateList = translatedDebateRepository.findByCategoryAndTitleOrContent(category, keyword, language, pageable);
        }

        long total = debateList.getTotalElements();
        List<DebateResDto> debateResDtoList = new ArrayList<>();
        for(TranslatedDebate translatedDebate : debateList) {
            Map<String, Double> percentMap =
                    calculateVotePercent(translatedDebate.getDebate().getAgreeCnt(),
                            translatedDebate.getDebate().getDisagreeCnt());

            DebateResDto debateResDto = DebateResDto.builder()
                    .title(translatedDebate.getTitle())
                    .debateId(translatedDebate.getDebate().getDebateId())
                    .views(translatedDebate.getDebate().getViews())
                    .createdAt(translatedDebate.getDebate().getCreatedAt())
                    .voteCnt(translatedDebate.getDebate().getVoteCnt())
                    .agreePercent(percentMap.get("agreePercent"))
                    .disagreePercent(percentMap.get("disagreePercent"))
                    .commentCnt(translatedDebate.getDebate().getCommentCnt())
                    .category(translatedDebate.getDebate().getCategory())
                    .nation(getTopNationByDebateId(translatedDebate.getDebate().getDebateId()))
                    .build();
            debateResDtoList.add(debateResDto);
        }
        return ResponseEntity.ok(Map.of(
                "debateList", debateResDtoList,
                "total", total
        ));
    }

    @Transactional
    public ResponseEntity<?> reactToDebate(String token, long debateId, DebateReqDto debateReqDto) {
        Optional<User> user = verifyToken(token);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }
        Debate debate = debateRepository.findById(debateId).get();

        DebateReaction debateReaction = debateReactionRepository
                .findByDebate_DebateIdAndUser_UserId(debateId, user.get().getUserId());

        if(debateReaction == null) {
            debateReaction = DebateReaction.builder()
                    .debate(debate)
                    .user(user.get())
                    .option(debateReqDto.getEmotion())
                    .build();
            debateReactionRepository.save(debateReaction);
        }
        else{
            if(debateReaction.getOption().equals(debateReqDto.getEmotion())) {
                debateReactionRepository.delete(debateReaction);
            }
            else{
                return ResponseEntity.badRequest().body("하나의 감정표현만 가능");
            }
        }
        long like = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "좋아요");
        long dislike = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "싫어요");
        long sad = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "슬퍼요");
        long angry = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "화나요");
        long hm = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "글쎄요");

        DebateResDto debateResDto = DebateResDto.builder()
                .like(like)
                .dislike(dislike)
                .sad(sad)
                .angry(angry)
                .hm(hm)
                .build();

        return ResponseEntity.ok(debateResDto);
    }

    public ResponseEntity<?> getDebate(String token, long debateId) {
        Optional<User> user = verifyToken(token);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }
        String language = user.get().getLanguage();

        Debate debate = debateRepository.findById(debateId).get();

        TranslatedDebate translatedDebate = translatedDebateRepository
                .findByDebate_DebateIdAndLanguage(debateId, language);

        long like = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "좋아요");
        long dislike = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "싫어요");
        long sad = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "슬퍼요");
        long angry = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "화나요");
        long hm = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "글쎄요");

        Map<String, Double> percentMap =
                calculateVotePercent(debate.getAgreeCnt(), debate.getDisagreeCnt());

        DebateResDto debateResDto = new DebateResDto(
                debateId, debate.getViews(), like, dislike, sad, angry, hm,
                debate.getVoteCnt(), debate.getCommentCnt(),
                percentMap.get("disagreePercent"), percentMap.get("agreePercent"),
                translatedDebate.getTitle(), translatedDebate.getContent(),
                debate.getCreatedAt(), debate.getCategory(),getTopNationByDebateId(debateId)
        );

        DebateReaction debateReaction = debateReactionRepository
                .findByDebate_DebateIdAndUser_UserId(debateId, user.get().getUserId());

        if(debateReaction != null) {
            debateResDto.setIsState(debateReaction.getOption());
        }

        Vote vote = voteRepository.findByDebate_DebateIdAndUser_UserId(debateId, user.get().getUserId());
        if(vote != null) {
            debateResDto.setIsVotedState(vote.getOption());
        }

        return ResponseEntity.ok(debateResDto);
    }

    public ResponseEntity<?> getVotedDebate(String token, long userId, int page, int size) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        String language = user.get().getLanguage();

        Pageable pageable = PageRequest.of(page, size);
        Page<Vote> voteList = voteRepository.findByUser_UserId(userId, pageable);

        long total = voteList.getTotalElements();

        List<DebateResDto> debateResDtoList = new ArrayList<>();

        for(Vote vote : voteList){
            debateResDtoList.add(buildDebateResDto(vote.getDebate(), language));
        }

        return ResponseEntity.ok(Map.of(
                "debateList", debateResDtoList,
                "total", total));
    }
}
