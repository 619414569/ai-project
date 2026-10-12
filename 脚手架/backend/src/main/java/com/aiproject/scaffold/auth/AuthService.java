package com.aiproject.scaffold.auth;

import com.aiproject.scaffold.auth.dto.LoginRequest;
import com.aiproject.scaffold.auth.dto.LoginResponse;
import com.aiproject.scaffold.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password())
        );
        JwtService.IssuedToken token = jwtService.issue(authentication);
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds());
    }
}
