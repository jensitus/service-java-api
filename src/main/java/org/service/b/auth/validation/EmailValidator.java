package org.service.b.auth.validation;

import org.service.b.auth.validation.annotation.ValidEmail;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

  private static final String EMAIL_PATTERN = "[\\w\\.\\-_]+@[\\w\\-_]+\\.\\w{2,8}";

  private Pattern pattern;

  private Matcher matcher;

  @Override
  public boolean isValid(String email, ConstraintValidatorContext context) {
    return validateEmail(email);
  }

  @Override
  public void initialize(ValidEmail constraintAnnotation) {
  }

  private boolean validateEmail(String email) {
    pattern = Pattern.compile(EMAIL_PATTERN);
    matcher = pattern.matcher(email);
    return matcher.matches();
  }

}
