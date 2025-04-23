package com.example.debate.repository;

import com.example.debate.entity.DebateReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DebateReactionRepository extends JpaRepository<DebateReaction, Long> {
    DebateReaction findByDebate_DebateIdAndUser_UserId(long debateId, Long userId);

    long countByDebate_DebateIdAndOption(long debateId, String option);
}
