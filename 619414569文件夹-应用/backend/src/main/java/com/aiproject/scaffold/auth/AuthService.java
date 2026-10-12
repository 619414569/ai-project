package com.aiproject.scaffold.auth;

import com.aiproject.scaffold.auth.dto.LoginRequest;
import com.aiproject.scaffold.auth.dto.LoginResponse;
import com.aiproject.scaffold.auth.dto.ChangePasswordRequest;
import com.aiproject.scaffold.common.api.ErrorCode;
import com.aiproject.scaffold.common.exception.BusinessException;
import com.aiproject.scaffold.security.JwtService;
import com.aiproject.scaffold.user.DatabaseUserDetailsService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final DatabaseUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            DatabaseUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password())
        );
        JwtService.IssuedToken token = jwtService.issue(authentication);
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds());
    }

    public void changePassword(String username, ChangePasswordRequest request) {
        UserDetails currentUser = userDetailsService.loadUserByUsername(username);
        if (!passwordEncoder.matches(request.currentPassword(), currentUser.getPassword())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), currentUser.getPassword())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "New password must be different from current password");
        }

        userDetailsService.updatePassword(username, passwordEncoder.encode(request.newPassword()));
    }
}
