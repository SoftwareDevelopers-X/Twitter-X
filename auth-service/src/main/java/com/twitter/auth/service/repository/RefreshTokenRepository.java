package com.twitter.auth.service.repository;

import com.twitter.auth.service.Model.RefreshToken;
import com.twitter.auth.service.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByUser(User user);

    List<RefreshToken> findAllByUser(User user);
}