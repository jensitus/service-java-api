package org.service.b.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class PasswordResetToken {

  private static final int EXPIRATION = 60 * 24;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String token;

  @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
  @JoinColumn(nullable = false, name = "user_id")
  private User user;

  private LocalDateTime expiryDate;

  public PasswordResetToken(){}

  public PasswordResetToken(User user, String token, LocalDateTime expiryDate) {
    this.token = token;
    this.user = user;
    this.expiryDate = expiryDate;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public LocalDateTime getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(LocalDateTime expiryDate) {
    this.expiryDate = expiryDate;
  }
}
