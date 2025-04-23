package com.example.debate.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "vote")
@NoArgsConstructor
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long voteId;

    @ManyToOne
    @JoinColumn(name = "debate_id")
    private Debate debate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "`option`")
    private String option;

    @Builder
    public Vote(Long voteId, Debate debate, User user, String option) {
        this.voteId = voteId;
        this.debate = debate;
        this.user = user;
        this.option = option;
    }
}
