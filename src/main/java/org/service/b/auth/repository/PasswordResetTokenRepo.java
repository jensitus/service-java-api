package org.service.b.auth.repository;

import org.service.b.auth.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepo extends JpaRepository<PasswordResetToken, Long> {
  PasswordResetToken findByTokenAndUserId(String token, Long user_id);
}
