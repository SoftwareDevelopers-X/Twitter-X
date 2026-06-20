package com.twitter.auth.service.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
<<<<<<< Updated upstream
    private Long tokenId;
=======
    private Long id;
>>>>>>> Stashed changes

    @Column(nullable = false, unique = true)
    private String token;

<<<<<<< Updated upstream
    private LocalDateTime expiryDate;

=======
>>>>>>> Stashed changes
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

<<<<<<< Updated upstream

=======
    @Column(nullable = false)
    private Instant expiryDate;
>>>>>>> Stashed changes
}