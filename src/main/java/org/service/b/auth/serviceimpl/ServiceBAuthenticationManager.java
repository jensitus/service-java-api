package org.service.b.auth.serviceimpl;

import org.service.b.auth.dto.LoginDto;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ServiceBAuthenticationManager {

    private final PasswordEncoder passwordEncoder;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthenticationManager springAuthenticationManager;

    public ServiceBAuthenticationManager(PasswordEncoder passwordEncoder, UserDetailsServiceImpl userDetailsService, AuthenticationManager springAuthenticationManager) {
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.springAuthenticationManager = springAuthenticationManager;
    }

    public Authentication authenticate(LoginDto loginDto) throws AuthenticationException {
        checkPW(loginDto);
        return springAuthenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword())
        );
    }

    private void checkPW(LoginDto loginDto) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(loginDto.getUsername());
        if (!passwordEncoder.matches(loginDto.getPassword(), userDetails.getPassword())) {
            throw new BadCredentialsException("wrong credentials");
        }
    }

}
