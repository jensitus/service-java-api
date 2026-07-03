package org.service.b.auth.service;

import org.service.b.auth.dto.ChangePwDto;
import org.service.b.auth.dto.UserDto;
import org.service.b.auth.message.PasswordResetForm;
import org.service.b.auth.message.Message;

import java.util.List;

public interface UserService {

  Message createPasswordResetTokenForUser(String email);

  boolean checkIfTokenExpired(String base64Token, String email, String confirm);

  Message resetPassword(PasswordResetForm passwordResetForm, String base64Token, String email);

  List<UserDto> getAll();

  UserDto getCurrentUser();

  UserDto getById(Long user_id);

  Message changePw(ChangePwDto changePwDto);

  Message encodePw(String pw);

}
