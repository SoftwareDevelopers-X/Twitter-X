package com.twitter.auth.service.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceName;

    private String ipAddress;

    private LocalDateTime loginTime;

    private boolean active;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String refreshToken;
}