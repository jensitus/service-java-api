package org.service.b.auth.serviceimpl;

import org.modelmapper.ModelMapper;
import org.service.b.auth.dto.ChangePwDto;
import org.service.b.auth.dto.UserDto;
import org.service.b.auth.message.PasswordResetForm;
import org.service.b.auth.model.PasswordResetToken;
import org.service.b.auth.model.User;
import org.service.b.auth.model.UserConfirmation;
import org.service.b.auth.repository.PasswordResetTokenRepo;
import org.service.b.auth.repository.UserConfirmationRepo;
import org.service.b.auth.repository.UserRepo;
import org.service.b.auth.service.UserService;
import org.service.b.auth.security.JwtProvider;
import org.service.b.common.mailer.service.ServiceBOrgMailer;
import org.service.b.auth.message.Message;
import org.service.b.common.util.EmailStuff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;


@Service
public class UserServiceImpl implements UserService {

  private static final int TOKEN_EXPIRY_HOURS = 2;
  private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

  @Autowired
  private PasswordResetTokenRepo passwordResetTokenRepo;

  @Autowired
  private ServiceBOrgMailer serviceBOrgMailer;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private UserRepo userRepo;

  @Autowired
  private ModelMapper modelMapper;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  private UserConfirmationRepo userConfirmationRepo;

  @Autowired
  private AuthenticationManager authenticationManager;

  @Override
  public Message createPasswordResetTokenForUser(String email) {
    User user = userRepo.findByEmail(email.toLowerCase());
    if (user == null) {
      logger.info("no user with email: " + email + " found");
      return new Message("Die Emailadresse gibt es nicht", false);
    }
    String token = UUID.randomUUID().toString();
    String base64token = encodeBase64Token(token);
    LocalDateTime expiryDate = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);
    PasswordResetToken passwordResetToken = new PasswordResetToken(user, token, expiryDate);
    passwordResetTokenRepo.save(passwordResetToken);
    String url = EmailStuff.DOMAIN_FOR_URL + "/auth/reset_password/" + base64token + "/edit?email=" + user.getEmail();
    String subject = EmailStuff.SUBJECT_PREFIX + " reset instructions";
    String text = "click the link below within the next 2 hours, after this it will expire";
    serviceBOrgMailer.getTheMailDetails(user.getEmail(), subject, text, user.getUsername(), url);
    return new Message("We've sent you a message with reset instructions", true);
  }

  @Override
  public boolean checkIfTokenExpired(String base64Token, String email, String confirm) {
    if ("confirm".equals(confirm)) {
      return isConfirmationTokenValid(base64Token, email);
    } else {
      return checkIfResetTokenExpired(base64Token, email);
    }
  }

  @Override
  public Message resetPassword(PasswordResetForm passwordResetForm, String base64Token, String email) {
    if (!passwordResetForm.getPassword().equals(passwordResetForm.getPassword_confirmation())) {
      return new Message("password and confirmation does not match", false);
    }
    PasswordResetToken passwordResetToken = getPasswordResetToken(base64Token, email);
    if (passwordResetToken == null || isResetTokenExpired(passwordResetToken)) {
      return new Message("Die Zeit ist abgelaufen", false);
    }
    // Bind the reset to the account the token was issued for — never to a
    // client-supplied email — so a valid token cannot reset another user's password.
    User user = passwordResetToken.getUser();
    user.setPassword(encoder.encode(passwordResetForm.getPassword()));
    userRepo.save(user);
    // Single use: consume the token so it cannot be replayed.
    passwordResetTokenRepo.delete(passwordResetToken);
    return new Message("toll", true);
  }

  @Override
  public List<UserDto> getAll() {
    List<User> users = userRepo.findAll();
    List<UserDto> userDtoList = new ArrayList<>();
    for (User user : users) {
      userDtoList.add(modelMapper.map(user, UserDto.class));
    }
    return userDtoList;
  }

  @Override
  public UserDto getCurrentUser() {
    UserDto userDto;
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    UserPrinciple userPrinciple = (UserPrinciple) auth.getPrincipal();
    userDto = modelMapper.map(userPrinciple, UserDto.class);
    return userDto;
  }

  @Override
  public UserDto getById(Long user_id) {
    User user = userRepo.getOne(user_id);
    return modelMapper.map(user, UserDto.class);
  }

  @Override
  public Message changePw(ChangePwDto changePwDto) {
    UserDto userDto = getById(changePwDto.getUserId());
    try {
      Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userDto.getUsername(), changePwDto.getOldPassword()));
      User user = userRepo.getOne(changePwDto.getUserId());
      user.setPassword(encoder.encode(changePwDto.getPassword()));
      userRepo.save(user);
      return new Message("Bravo, Password successfully changed!", true);
    } catch (Exception e) {
      return new Message(e.getLocalizedMessage(), false);
    }
  }

  // true = the token exists and is still valid (not expired)
  private boolean checkIfResetTokenExpired(String base64Token, String email) {
    PasswordResetToken passwordResetToken = getPasswordResetToken(base64Token, email);
    return passwordResetToken != null && !isResetTokenExpired(passwordResetToken);
  }

  private boolean isResetTokenExpired(PasswordResetToken passwordResetToken) {
    return passwordResetToken.getExpiryDate().isBefore(LocalDateTime.now());
  }

  private PasswordResetToken getPasswordResetToken(String base64Token, String email) {
    String token = decodeBase64Token(base64Token);
    User user = userRepo.findByEmail(email);
    if (user == null) {
      return null;
    }
    return passwordResetTokenRepo.findByTokenAndUserId(token, user.getId());
  }

  private boolean isConfirmationTokenValid(String base64Token, String email) {
    String token = decodeBase64Token(base64Token);
    User user = userRepo.findByEmail(email);
    if (user == null) {
      return false;
    }
    UserConfirmation uc = userConfirmationRepo.findByConfirmationTokenAndUserId(token, user.getId());
    // Reject unknown tokens and already-consumed confirmations (single use).
    if (uc == null || uc.getConfirmedAt() != null) {
      return false;
    }
    return !isTokenExpired(uc.getConfirmationExpiry());
  }

  private String decodeBase64Token(String encodedToken) {
    return new String(Base64.getDecoder().decode(encodedToken));
  }

  private String encodeBase64Token(String token) {
    return Base64.getEncoder().encodeToString(token.getBytes());
  }

  private boolean isTokenExpired(LocalDateTime expiryTime) {
    return expiryTime.isBefore(LocalDateTime.now());
  }

  public Message encodePw(String pw) {
    return new Message(encoder.encode(pw));
  }

}
