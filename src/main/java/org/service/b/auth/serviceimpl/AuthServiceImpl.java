package org.service.b.auth.serviceimpl;

import io.jsonwebtoken.impl.Base64Codec;
import org.service.b.auth.dto.UserDto;
import org.service.b.auth.dto.LoginDto;
import org.service.b.auth.dto.SignUpDto;
import org.service.b.auth.model.Role;
import org.service.b.auth.model.RoleName;
import org.service.b.auth.model.User;
import org.service.b.auth.model.UserConfirmation;
import org.service.b.auth.repository.RoleRepo;
import org.service.b.auth.repository.UserConfirmationRepo;
import org.service.b.auth.repository.UserRepo;
import org.service.b.auth.security.JwtProvider;
import org.service.b.auth.service.AuthService;
import org.modelmapper.ModelMapper;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

  private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

  @Autowired
  AuthenticationManager authenticationManager;

  @Autowired
  JwtProvider jwtProvider;

  @Autowired
  private UserRepo userRepo;

  @Autowired
  private ModelMapper modelMapper;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  RoleRepo roleRepo;

  @Autowired
  private UserConfirmationRepo userConfRepo;

  @Autowired
  private ServiceBOrgMailer serviceBOrgMailer;

  @Override
  public UserDto getUserDtoWithJwt(LoginDto loginDto) {
    Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = jwtProvider.generateJwtToken(authentication);
    User user = userRepo.findByUsername(loginDto.getUsername()).orElseThrow(() -> new UsernameNotFoundException("User not found with -> username or email: " + loginDto.getUsername()));
    UserDto userDto = modelMapper.map(user, UserDto.class);
    userDto.setAccessToken(jwt);
    return userDto;
  }

  @Override
  public Message createUser(SignUpDto signUpDto) {
    // create the new user:
    User user = new User(signUpDto.getUsername(), signUpDto.getEmail().toLowerCase(), encoder.encode(signUpDto.getPassword()));
    Role uRole = new Role(RoleName.ROLE_USER);
    Set<String> roleSet = new HashSet<>();
    roleSet.add(uRole.getName().toString());
    signUpDto.setRole(roleSet);
    Set<String> strRoles = signUpDto.getRole();
    Set<Role> roles = new HashSet<>();
    strRoles.forEach(role -> {
      switch (role) {
        case "admin":
          Role adminRole = roleRepo.findByName(RoleName.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("The Fucking Role couldn't be found, sorry"));
          roles.add(adminRole);
          break;
        default:
          Role userRole = roleRepo.findByName(RoleName.ROLE_USER).orElseThrow(() -> new RuntimeException("No Role Today my Love is gone away"));
          roles.add(userRole);
      }
    });
    user.setRoles(roles);
    userRepo.save(user);
    createUserConfirmation(user);
    return new Message("user created");
  }

  private Message createUserConfirmation(User user) {
    String token = UUID.randomUUID().toString();
    UserConfirmation userConfirmation = new UserConfirmation();
    userConfirmation.setConfirmationToken(token);
    userConfirmation.setUser(user);
    userConfirmation.setConfirmationExpiry(LocalDateTime.now());
    userConfRepo.save(userConfirmation);
    String base64token = Base64Codec.BASE64.encode(token);
    String url = EmailStuff.DOMAIN_FOR_URL + "/auth/" + base64token + "/confirm?email=" + user.getEmail();
    String subject = EmailStuff.SUBJECT_PREFIX + "confirm account";
    String text = "click the link below within the next 2 hours, after this it will expire";
    serviceBOrgMailer.getTheMailDetails(user.getEmail(), subject, text, user.getUsername(), url);
    return new Message("We've sent you a message confirming your", true);
  }

  @Override
  public Message confirmAccount(String base64Token, String email) {
    String token = Base64Codec.BASE64.decodeToString(base64Token);
    User user = userRepo.findByEmail(email);
    user.setConfirmed(Boolean.TRUE);
    userRepo.save(user);
    UserConfirmation uc = userConfRepo.findByConfirmationTokenAndUserId(token, user.getId());
    uc.setConfirmedAt(LocalDateTime.now());
    userConfRepo.save(uc);
    return new Message("User successfully confirmed");
//    try {
//      userRepo.save(user);
//      userConfRepo.save(uc);
//      return new Message("User successfully confirmed");
//    } catch (Exception e) {
//      return new Message(e.toString());
//    }
  }
}
