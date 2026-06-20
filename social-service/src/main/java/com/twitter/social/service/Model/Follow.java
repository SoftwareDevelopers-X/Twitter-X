package com.twitter.social.service.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "follows", uniqueConstraints = {@UniqueConstraint(columnNames = {"followerId", "followingId"})})
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long followId;

    private Long followerId;

    private Long followingId;

    @CreationTimestamp
    private LocalDateTime followedAt;
}
