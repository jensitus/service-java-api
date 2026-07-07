package org.service.b.user;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.service.b.auth.security.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertFalse;

@SpringBootTest
public class UserTest {

  @Autowired
  private JwtProvider jwtProvider;

  @Test
  public void rejectsMalformedToken() {
    // Smoke test: the provider is wired up and its signing key rejects a bad token.
    assertFalse(jwtProvider.validateJwtToken("not-a-real-token").getRedirect());
  }

}
