package com.aiproject.scaffold.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class WorkbenchDataInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;

    public WorkbenchDataInitializer(
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
        String primaryUser = securityProperties.demoUser().username();
        Set<String> usernames = new LinkedHashSet<>(List.of(primaryUser, "chyong", "liyan", "zhangwei", "wangqi"));
        usernames.forEach(username -> insertUser(
                username,
                username.equals(primaryUser) ? String.join(",", securityProperties.demoUser().roles()) : "USER"
        ));

        insertApplication("personal-data-standard", "数据标准助手", "面向业务对象的数据标准检索、映射与质量检查。",
                "数据分析", "personal", "shared", "running", primaryUser, null, "2026-10-10T02:42:00Z");
        insertApplication("personal-training-knowledge", "培训知识库", "聚合培训制度、课程材料和常见问题的知识检索应用。",
                "知识管理", "personal", "exclusive", "running", primaryUser, null, "2026-10-09T08:18:00Z");
        insertApplication("personal-meeting-minutes", "会议纪要生成器", "从会议材料中提取议题、结论和后续行动项。",
                "内容生成", "personal", "exclusive", "paused", primaryUser, null, "2026-10-08T06:06:00Z");
        insertApplication("personal-chyong-requirement-review", "需求评审助手", "整理需求变更、评审意见和待确认事项，形成可追踪清单。",
                "流程自动化", "personal", "shared", "running", "chyong", null, "2026-10-10T00:52:00Z");
        insertApplication("personal-chyong-api-guide", "接口文档助手", "根据接口定义生成调用示例、参数说明和联调检查项。",
                "内容生成", "personal", "shared", "running", "chyong", null, "2026-10-09T07:26:00Z");
        insertApplication("personal-liyan-operation-report", "运营周报生成器", "汇总运营数据和重点事项，生成结构化周报初稿。",
                "内容生成", "personal", "shared", "running", "liyan", null, "2026-10-09T09:40:00Z");
        insertApplication("personal-zhangwei-risk-inspection", "项目风险巡检", "识别项目计划中的延期风险、依赖冲突和资源缺口。",
                "数据分析", "personal", "shared", "paused", "zhangwei", null, "2026-10-08T03:12:00Z");

        insertApplication("collab-solution-review", "方案协同评审", "多人共同维护方案版本、评审意见和处理结论。",
                "流程自动化", "collaboration", "shared", "running", primaryUser, null, "2026-10-10T01:36:00Z");
        insertCollaborators("collab-solution-review", List.of("chyong", "zhangwei"));
        insertApplication("collab-operation-analysis", "经营分析助手", "汇总经营指标并协同生成月度分析材料。",
                "数据分析", "collaboration", "shared", "running", "chyong", null, "2026-10-09T10:24:00Z");
        insertCollaborators("collab-operation-analysis", List.of(primaryUser, "liyan"));
        insertApplication("collab-training-question-bank", "安全培训题库", "协同维护培训题目、答案解析和适用岗位。",
                "知识管理", "collaboration", "exclusive", "paused", primaryUser, null, "2026-10-07T03:20:00Z");
        insertCollaborators("collab-training-question-bank", List.of("chyong"));

        insertLog("log-6adca2f", primaryUser, "2026-10-10T03:30:48Z", "6adca2f", "项目文档",
                "新增提交日志目录、个人提交记录和 README 入口。");
        insertLog("log-5dc97a6", primaryUser, "2026-10-10T03:23:34Z", "5dc97a6", "框架底座",
                "新增通用前后端分离脚手架及持续集成配置。");
        insertLog("log-0e970e0", primaryUser, "2026-10-10T01:52:57Z", "0e970e0", "项目文档",
                "更新团队协作方式、基础框架范围和开发要求。");
        insertLog("log-4daf79a", "chyong", "2026-10-10T01:51:17Z", "4daf79a", "项目初始化", "创建项目 README。");
    }

    private void insertUser(String username, String roles) {
        if (exists("SELECT COUNT(*) FROM workbench_user WHERE username = ?", username)) return;
        jdbcTemplate.update(
                "INSERT INTO workbench_user (username, password_hash, roles) VALUES (?, ?, ?)",
                username,
                passwordEncoder.encode(securityProperties.demoUser().password()),
                roles
        );
    }

    private void insertApplication(
            String id,
            String name,
            String description,
            String category,
            String scope,
            String visibility,
            String status,
            String owner,
            String applicationUrl,
            String updatedAt
    ) {
        if (exists("SELECT COUNT(*) FROM workspace_application WHERE id = ?", id)) return;
        Timestamp timestamp = Timestamp.from(Instant.parse(updatedAt));
        jdbcTemplate.update(
                """
                        INSERT INTO workspace_application
                            (id, name, description, category, scope, visibility, status, owner_username,
                             application_url, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, name, description, category, scope, visibility, status, owner, applicationUrl, timestamp, timestamp
        );
    }

    private void insertCollaborators(String applicationId, List<String> usernames) {
        for (String username : usernames) {
            if (exists(
                    "SELECT COUNT(*) FROM workspace_application_collaborator WHERE application_id = ? AND username = ?",
                    applicationId,
                    username
            )) continue;
            jdbcTemplate.update(
                    "INSERT INTO workspace_application_collaborator (application_id, username) VALUES (?, ?)",
                    applicationId,
                    username
            );
        }
    }

    private void insertLog(String id, String username, String committedAt, String commit, String area, String summary) {
        if (exists("SELECT COUNT(*) FROM submission_log WHERE id = ?", id)) return;
        jdbcTemplate.update(
                """
                        INSERT INTO submission_log (id, username, committed_at, commit_hash, area, summary)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                id,
                username,
                Timestamp.from(Instant.parse(committedAt)),
                commit,
                area,
                summary
        );
    }

    private boolean exists(String sql, Object... arguments) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, arguments);
        return count != null && count > 0;
    }
}
