package org.service.b.auth.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_confirmation")
public class UserConfirmation {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "id", unique = true, nullable = false)
  private UUID id;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  private User user;

  @Column(name = "confirmation_token")
  private String confirmationToken;

  @Column(name = "confirmation_expiry")
  private LocalDateTime confirmationExpiry;

  @Column(name = "confirmed_at")
  private LocalDateTime confirmedAt;

  public UserConfirmation() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getConfirmationToken() {
    return confirmationToken;
  }

  public void setConfirmationToken(String confirmationToken) {
    this.confirmationToken = confirmationToken;
  }

  public LocalDateTime getConfirmationExpiry() {
    return confirmationExpiry;
  }

  public void setConfirmationExpiry(LocalDateTime confirmationExpiry) {
    this.confirmationExpiry = confirmationExpiry;
  }

  public LocalDateTime getConfirmedAt() {
    return confirmedAt;
  }

  public void setConfirmedAt(LocalDateTime confirmedAt) {
    this.confirmedAt = confirmedAt;
  }

}
