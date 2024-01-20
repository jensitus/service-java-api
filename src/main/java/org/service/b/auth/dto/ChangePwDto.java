package org.service.b.auth.dto;

import org.service.b.auth.validation.NewPasswordMatch;
import org.service.b.auth.validation.annotation.PasswordMatches;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@PasswordMatches
public class ChangePwDto implements NewPasswordMatch {
  private Long userId;

  @NotBlank
  @Size(min = 6, max = 70)
  private String password;

  private String passwordConfirmation;

  private String oldPassword;

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getPasswordConfirmation() {
    return passwordConfirmation;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setPasswordConfirmation(String passwordConfirmation) {
    this.passwordConfirmation = passwordConfirmation;
  }

  public String getOldPassword() {
    return oldPassword;
  }

  public void setOldPassword(String oldPassword) {
    this.oldPassword = oldPassword;
  }

  @Override
  public String toString() {
    return "ChangePwDto{" +
            "userId=" + userId +
            ", password='" + password + '\'' +
            ", passwordConfirmation='" + passwordConfirmation + '\'' +
            ", oldPassword='" + oldPassword + '\'' +
            '}';
  }
}
