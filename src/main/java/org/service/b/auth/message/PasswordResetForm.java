package org.service.b.auth.message;

import jakarta.validation.constraints.NotBlank;

public class PasswordResetForm {

  @NotBlank
  private String email;

  private String password;

  private String password_confirmation;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getPassword_confirmation() {
    return password_confirmation;
  }

  public void setPassword_confirmation(String password_confirmation) {
    this.password_confirmation = password_confirmation;
  }
}
