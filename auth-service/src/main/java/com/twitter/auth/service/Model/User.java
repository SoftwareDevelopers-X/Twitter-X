package com.twitter.auth.service.Model;
import com.twitter.auth.service.Enum.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collection;
import java.util.List;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    private String username;

    @Column(unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

<<<<<<< Updated upstream
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RefreshToken> refreshTokens;
=======
    @Column(nullable = false)
    private int failedAttempts;

    @Column(nullable = false)
    private boolean accountLocked;

    private LocalDateTime lockTime;

    private boolean accountNonLocked = true;

    private boolean banned = false;
>>>>>>> Stashed changes
}
