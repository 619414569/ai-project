package com.aiproject.scaffold.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 从数据库 {@code workbench_user} 表加载登录账号。
 *
 * <p>每次认证都重新查询并返回新的 {@link UserDetails} 实例，避免复用实例导致
 * Spring Security 凭据擦除后无法再次登录（见错题本 BUG-20261019-001）。</p>
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        List<UserRecord> users = jdbcTemplate.query(
                """
                        SELECT username, password_hash, roles, active
                        FROM workbench_user
                        WHERE username = ?
                        """,
                (resultSet, rowNum) -> new UserRecord(
                        resultSet.getString("username"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("roles"),
                        resultSet.getBoolean("active")
                ),
                username
        );
        if (users.isEmpty()) {
            throw new UsernameNotFoundException(username);
        }
        UserRecord user = users.get(0);
        return User.withUsername(user.username())
                .password(user.passwordHash())
                .roles(parseRoles(user.roles()).toArray(String[]::new))
                .disabled(!user.active())
                .build();
    }

    public boolean isPasswordChangeRequired(String username) {
        Boolean required = jdbcTemplate.queryForObject(
                "SELECT password_change_required FROM workbench_user WHERE username = ?",
                Boolean.class,
                username
        );
        return Boolean.TRUE.equals(required);
    }

    public void updatePassword(String username, String encodedPassword) {
        int updated = jdbcTemplate.update(
                """
                        UPDATE workbench_user
                        SET password_hash = ?, password_change_required = FALSE, updated_at = CURRENT_TIMESTAMP
                        WHERE username = ?
                        """,
                encodedPassword,
                username
        );
        if (updated != 1) {
            throw new UsernameNotFoundException(username);
        }
    }

    public List<String> listActiveUsernames() {
        return jdbcTemplate.queryForList(
                "SELECT username FROM workbench_user WHERE active = TRUE ORDER BY username",
                String.class
        );
    }

    private List<String> parseRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .toList();
    }

    private record UserRecord(String username, String passwordHash, String roles, boolean active) {
    }
}
