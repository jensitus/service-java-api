package org.service.b.user;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.service.b.auth.security.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
public class UserTest {

  @Autowired
  private JwtProvider jwtProvider;

  @Test
  public void getSecretKey() {
    jwtProvider.getSigningBraveKey();
  }

}
