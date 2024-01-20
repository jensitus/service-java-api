package org.service.b.auth.dto;

import org.service.b.auth.validation.NewPasswordMatch;
import org.service.b.auth.validation.annotation.PasswordMatches;
import org.service.b.auth.validation.annotation.ValidEmail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

@PasswordMatches
public class SignUpDto implements NewPasswordMatch {

  @NotBlank
  @Size(min = 3, max = 50)
  private String username;

  @NotBlank
  @Size(max = 60)
  @NotNull
  @ValidEmail
  private String email;

  private Set<String> role;

  @NotBlank
  @Size(min = 6, max = 70)
  private String password;

  private String passwordConfirmation;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Set<String> getRole() {
    return this.role;
  }

  public void setRole(Set<String> role) {
    this.role = role;
  }

  @Override
  public String getPasswordConfirmation() {
    return passwordConfirmation;
  }

  public void setPasswordConfirmation(String passwordConfirmation) {
    this.passwordConfirmation = passwordConfirmation;
  }

}
