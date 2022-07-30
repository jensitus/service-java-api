package org.service.b.auth.controller;

import org.service.b.auth.dto.ChangePwDto;
import org.service.b.auth.dto.UserDto;
import org.service.b.auth.repository.UserRepo;
import org.service.b.auth.security.JwtProvider;
import org.service.b.auth.service.UserService;
import org.service.b.auth.serviceimpl.UserDetailsServiceImpl;
import org.service.b.auth.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/service/users")
public class UserRestApi {

  private static final Logger logger = LoggerFactory.getLogger(UserRestApi.class);

  @Autowired
  private UserRepo userRepo;

  @Autowired
  private UserDetailsServiceImpl userDetailsService;

  @Autowired
  private UserService userService;

  @Autowired
  JwtProvider jwtProvider;

  @GetMapping("/all")
  public List<UserDto> getAllUsers() {
    List<UserDto> userDtoList = userService.getAll();
    return userDtoList;
  }

  @GetMapping("/principle/{username}")
  public UserDetails getSpecialUser(@PathVariable String username) {
    return userDetailsService.loadUserByUsername(username);
  }

  @PostMapping("/auth/check_auth_token")
  public ResponseEntity<Message> checkTheAuthToken(@RequestBody String token) {
    Message message = jwtProvider.validateJwtToken(token);
    if (jwtProvider.validateJwtToken(token).getRedirect()) {
      return new ResponseEntity<>(message, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(message, HttpStatus.FORBIDDEN);
    }
  }

  @PostMapping("/auth/password_resets")
  public ResponseEntity<Message> password_resets(@RequestBody String email) {
    userService.createPasswordResetTokenForUser(email);
    return new ResponseEntity<>(new Message("jess god damn"), HttpStatus.OK);
  }

  @PostMapping("/changepw")
  public ResponseEntity changePw(@Valid @RequestBody ChangePwDto changePwDto) {
    Message m = userService.changePw(changePwDto);
    if (m.getRedirect()) {
      return new ResponseEntity<>(new Message(m.getText()), HttpStatus.OK);
    } else {
      return new ResponseEntity<>(m, HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }

}
