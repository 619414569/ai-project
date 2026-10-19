package com.aiproject.scaffold.user;

import com.aiproject.scaffold.config.DemoUserInitializer;
import com.aiproject.scaffold.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 覆盖数据库账号加载逻辑：账号来自 {@code workbench_user} 表，口令以 BCrypt 散列保存。
 *
 * <p>所有用例在事务中执行，结束自动回滚，不残留临时账号。断言口令时一律从
 * {@link SecurityProperties} 读取配置值，不在测试源码中写入明文口令（规则 8.7）。</p>
 */
@SpringBootTest(
        // 使用 MOCK 而非 NONE：SecurityConfig#securityFilterChain 依赖 HttpSecurity，
        // 只有模拟 Web 环境才会提供该 Bean；MOCK 不会占用端口。
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "app.integration.rocketmq.enabled=false",
                "flowable.database-schema-update=false"
        }
)
@Transactional
class DatabaseUserDetailsServiceTest {

    @Autowired
    private DatabaseUserDetailsService userDetailsService;

    @Autowired
    private DemoUserInitializer demoUserInitializer;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedDemoUsers() {
        // 与真实启动路径保持一致：由初始化器补齐缺失的演示账号（幂等，重复调用无副作用）。
        demoUserInitializer.run(new DefaultApplicationArguments());
    }

    @Test
    @DisplayName("数据库账号：主演示账号从 workbench_user 加载且散列匹配配置口令")
    void loadsPrimaryDemoUserFromDatabase() {
        SecurityProperties.DemoUser configured = securityProperties.demoUser();

        UserDetails user = userDetailsService.loadUserByUsername(configured.username());

        assertThat(user.getUsername()).isEqualTo(configured.username());
        assertThat(passwordEncoder.matches(configured.password(), user.getPassword())).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("数据库账号：附加演示账号 dcy 可加载且角色为 USER")
    void loadsAdditionalDemoUserDcy() {
        Optional<SecurityProperties.DemoUser> dcy = securityProperties.additionalDemoUsers().stream()
                .filter(user -> "dcy".equals(user.username()))
                .findFirst();
        assertThat(dcy).as("配置中应声明 dcy 演示账号").isPresent();

        UserDetails user = userDetailsService.loadUserByUsername("dcy");

        assertThat(user.getUsername()).isEqualTo("dcy");
        assertThat(passwordEncoder.matches(dcy.get().password(), user.getPassword())).isTrue();
        assertThat(user.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("数据库账号：库中只保存口令散列，不保存明文")
    void storesPasswordHashOnly() {
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM workbench_user WHERE username = ?", String.class, "dcy");

        assertThat(storedHash).isNotNull().isNotBlank();
        assertThat(storedHash).isNotEqualTo("admin123");
        assertThat(storedHash).startsWith("$2");
    }

    @Test
    @DisplayName("数据库账号：用户名不存在时抛出 UsernameNotFoundException")
    void rejectsUnknownUsername() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("no-such-user"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("数据库账号：active 为 false 时账号被禁用")
    void disablesInactiveUser() {
        insertUser("probe-inactive", "secret123", "USER", false, false);

        UserDetails user = userDetailsService.loadUserByUsername("probe-inactive");

        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("数据库账号：角色字段按逗号拆分并忽略空白项")
    void parsesRolesFromCommaSeparatedColumn() {
        insertUser("probe-roles", "secret123", " ADMIN , , USER ", true, true);

        UserDetails user = userDetailsService.loadUserByUsername("probe-roles");

        assertThat(user.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("数据库账号：updatePassword 替换散列并清除改密标记")
    void updatePasswordReplacesHashAndClearsFlag() {
        insertUser("probe-change", "secret123", "USER", true, true);
        assertThat(userDetailsService.isPasswordChangeRequired("probe-change")).isTrue();

        userDetailsService.updatePassword("probe-change", passwordEncoder.encode("newSecret456"));

        UserDetails updated = userDetailsService.loadUserByUsername("probe-change");
        assertThat(passwordEncoder.matches("newSecret456", updated.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("secret123", updated.getPassword())).isFalse();
        assertThat(userDetailsService.isPasswordChangeRequired("probe-change")).isFalse();
    }

    @Test
    @DisplayName("数据库账号：updatePassword 对不存在的账号抛出 UsernameNotFoundException")
    void updatePasswordRejectsUnknownUser() {
        assertThatThrownBy(() -> userDetailsService.updatePassword("no-such-user", "irrelevant"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("数据库账号：listActiveUsernames 只包含启用账号")
    void listsOnlyActiveUsernames() {
        insertUser("probe-inactive", "secret123", "USER", false, false);

        List<String> usernames = userDetailsService.listActiveUsernames();

        assertThat(usernames).contains(securityProperties.demoUser().username(), "dcy");
        assertThat(usernames).doesNotContain("probe-inactive");
    }

    private void insertUser(
            String username,
            String rawPassword,
            String roles,
            boolean active,
            boolean passwordChangeRequired
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO workbench_user (username, password_hash, roles, password_change_required, active)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                username,
                passwordEncoder.encode(rawPassword),
                roles,
                passwordChangeRequired,
                active
        );
    }
}
