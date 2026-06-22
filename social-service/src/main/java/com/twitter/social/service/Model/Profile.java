package com.twitter.social.service.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "profiles", uniqueConstraints = {@UniqueConstraint(columnNames = {"userId"})})
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The only foreign key to auth-service's User table.
    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(length = 160)
    private String bio;

    @Column(length = 100)
    private String location;

    @Column(length = 100)
    private String website;

    // URLs returned by media-service after upload (MinIO object URL)
    private String avatarUrl;

    private String bannerUrl;

    private Long avatarMediaId;

    private Long bannerMediaId;

    private LocalDate dateOfBirth;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isVerified = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isPrivate = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
