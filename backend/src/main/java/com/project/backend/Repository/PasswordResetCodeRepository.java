package com.project.backend.Repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.Model.PasswordResetCodeModel;
import com.project.backend.Model.UserModel;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCodeModel, Long> {

    long countByUserAndCreatedAtAfter(UserModel user, Instant createdAtAfter);

    void deleteByUserAndUsedAtIsNull(UserModel user);

    Optional<PasswordResetCodeModel> findTopByUserAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            UserModel user,
            Instant now);
}
