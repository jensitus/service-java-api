package org.service.b.auth.serviceimpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.service.b.auth.message.Message;
import org.service.b.auth.message.PasswordResetForm;
import org.service.b.auth.model.PasswordResetToken;
import org.service.b.auth.model.User;
import org.service.b.auth.repository.PasswordResetTokenRepo;
import org.service.b.auth.repository.UserRepo;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the password-reset fix: the account whose password gets changed must be
 * derived from the reset token itself, never from a client-supplied email. Without
 * this, a valid token for one's own account could reset any other user's password.
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceImplResetPasswordTest {

  @Mock
  private PasswordResetTokenRepo passwordResetTokenRepo;

  @Mock
  private UserRepo userRepo;

  @Mock
  private PasswordEncoder encoder;

  @InjectMocks
  private UserServiceImpl userService;

  private static final String RAW_TOKEN = "reset-token-123";

  private static String base64(String token) {
    return Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
  }

  private User user(long id, String email, String password) {
    User user = new User("user" + id, email, password);
    user.setId(id);
    return user;
  }

  private PasswordResetForm form(String bodyEmail, String password, String confirmation) {
    PasswordResetForm form = new PasswordResetForm();
    form.setEmail(bodyEmail);
    form.setPassword(password);
    form.setPassword_confirmation(confirmation);
    return form;
  }

  @Test
  public void tokenOwnerPasswordIsChanged_notTheBodyEmailAccount() {
    User attacker = user(1L, "attacker@x.com", "old-attacker-hash");
    User victim = user(2L, "victim@x.com", "old-victim-hash");

    // The token was issued for the attacker's own account.
    PasswordResetToken token = new PasswordResetToken(attacker, RAW_TOKEN,
        LocalDateTime.now().plusHours(2));

    when(userRepo.findByEmail("attacker@x.com")).thenReturn(attacker);
    when(passwordResetTokenRepo.findByTokenAndUserId(RAW_TOKEN, 1L)).thenReturn(token);
    when(encoder.encode("newpass")).thenReturn("new-hash");

    // Attacker owns the token (query email = attacker) but tries to pivot to the
    // victim via the request body email.
    Message result = userService.resetPassword(
        form("victim@x.com", "newpass", "newpass"), base64(RAW_TOKEN), "attacker@x.com");

    assertTrue(result.getRedirect());

    // Only the token owner's password is updated...
    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepo).save(saved.capture());
    assertEquals(1L, saved.getValue().getId());
    assertEquals("new-hash", attacker.getPassword());

    // ...the victim is never touched, and the token is consumed.
    assertEquals("old-victim-hash", victim.getPassword());
    verify(userRepo, never()).findByEmail("victim@x.com");
    verify(passwordResetTokenRepo).delete(token);
  }

  @Test
  public void expiredTokenIsRejectedAndNoPasswordChange() {
    User user = user(1L, "user@x.com", "old-hash");
    PasswordResetToken token = new PasswordResetToken(user, RAW_TOKEN,
        LocalDateTime.now().minusMinutes(1));

    when(userRepo.findByEmail("user@x.com")).thenReturn(user);
    when(passwordResetTokenRepo.findByTokenAndUserId(RAW_TOKEN, 1L)).thenReturn(token);

    Message result = userService.resetPassword(
        form("user@x.com", "newpass", "newpass"), base64(RAW_TOKEN), "user@x.com");

    assertFalse(result.getRedirect());
    assertEquals("old-hash", user.getPassword());
    verify(userRepo, never()).save(any());
    verify(passwordResetTokenRepo, never()).delete(any());
  }

  @Test
  public void mismatchedConfirmationIsRejectedBeforeAnyLookup() {
    Message result = userService.resetPassword(
        form("user@x.com", "newpass", "different"), base64(RAW_TOKEN), "user@x.com");

    assertFalse(result.getRedirect());
    verify(userRepo, never()).save(any());
    verify(passwordResetTokenRepo, never()).findByTokenAndUserId(any(), eq(1L));
  }
}
