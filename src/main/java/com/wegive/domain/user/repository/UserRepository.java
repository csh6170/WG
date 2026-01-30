package com.wegive.domain.user.repository;

import com.wegive.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * [Repository] User 테이블에 SQL을 날리는 도구 (JPA)
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // SELECT * FROM USERS WHERE EMAIL = ?
    Optional<User> findByEmail(String email);

    // SELECT * FROM USERS WHERE REFRESH_TOKEN = ?
    Optional<User> findByRefreshToken(String refreshToken);

    // SELECT COUNT(*) > 0 FROM USERS WHERE NICKNAME = ? (중복확인)
    boolean existsByNickname(String nickname);
}