package com.twitter.social.service.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "follows")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long followId;

    private Long followerId;

    private Long followingId;

    @CreationTimestamp
    private LocalDateTime followedAt;
}
