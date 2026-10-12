package com.aiproject.scaffold.auth;

import com.aiproject.scaffold.auth.dto.ChangePasswordRequest;
import com.aiproject.scaffold.common.exception.BusinessException;
import com.aiproject.scaffold.user.DatabaseUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void changesPasswordWhenCurrentPasswordMatches() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        DatabaseUserDetailsService users = mock(DatabaseUserDetailsService.class);
        when(users.loadUserByUsername("admin")).thenReturn(
                User.withUsername("admin").password(encoder.encode("admin123")).roles("ADMIN").build()
        );
        AuthService service = new AuthService(null, null, users, encoder);

        service.changePassword("admin", new ChangePasswordRequest("admin123", "newAdmin456"));

        verify(users).updatePassword(
                eq("admin"),
                argThat(encoded -> encoder.matches("newAdmin456", encoded))
        );
    }

    @Test
    void rejectsIncorrectCurrentPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        DatabaseUserDetailsService users = mock(DatabaseUserDetailsService.class);
        when(users.loadUserByUsername("admin")).thenReturn(
                User.withUsername("admin").password(encoder.encode("admin123")).roles("ADMIN").build()
        );
        AuthService service = new AuthService(null, null, users, encoder);

        assertThrows(
                BusinessException.class,
                () -> service.changePassword("admin", new ChangePasswordRequest("wrong-password", "newAdmin456"))
        );
        verify(users, never()).updatePassword(eq("admin"), org.mockito.ArgumentMatchers.anyString());
    }
}
