package com.aiproject.scaffold.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 应用启动时把配置项声明的演示账号以 BCrypt 散列形式写入 {@code workbench_user} 表。
 *
 * <p>只补充缺失账号，已存在的账号不会被覆盖，因此可重复执行。明文口令来自配置，
 * 不写入迁移脚本、源码或日志（规则 8.7）。把 {@code APP_DEMO_DATA_ENABLED} 设为
 * {@code false} 可关闭该初始化。</p>
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoUserInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;

    public DemoUserInitializer(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            SecurityProperties securityProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.securityProperties = securityProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SecurityProperties.DemoUser primary = securityProperties.demoUser();
        seed(primary.username(), primary.password(), primary.roles());

        for (SecurityProperties.DemoUser extra : securityProperties.additionalDemoUsers()) {
            seed(extra.username(), extra.password(), extra.roles());
        }
    }

    private void seed(String username, String rawPassword, List<String> roles) {
        if (exists(username)) {
            return;
        }
        jdbcTemplate.update(
                """
                        INSERT INTO workbench_user
                            (username, password_hash, roles, password_change_required, active)
                        VALUES (?, ?, ?, FALSE, TRUE)
                        """,
                username,
                passwordEncoder.encode(rawPassword),
                String.join(",", roles)
        );
    }

    private boolean exists(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workbench_user WHERE username = ?", Integer.class, username);
        return count != null && count > 0;
    }
}
