package com.aiproject.scaffold.auth;

import com.aiproject.scaffold.config.DemoUserInitializer;
import com.aiproject.scaffold.config.SecurityProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 覆盖登录主流程与跨域来源白名单的集成回归测试。
 *
 * <p>对应错题本记录：</p>
 * <ul>
 *   <li>BUG-20261019-001：单例 UserDetails 凭据被擦除，登录仅首次成功</li>
 *   <li>BUG-20261019-002：CORS 仅支持精确来源匹配，局域网地址访问登录返回 403</li>
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // 本地无 RocketMQ 服务，消费者容器启动会失败，仅停用消息示例。
                "app.integration.rocketmq.enabled=false",
                // 认证改为数据库校验，依赖 workbench_user 表，因此保留 Flyway 迁移；
                // 仅停用 Flowable 自动建表，避免写入与认证无关的表。
                "flowable.database-schema-update=false"
        }
)
class AuthIntegrationTest {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String ME_PATH = "/api/v1/auth/me";
    private static final String DEMO_USERNAME = "admin";
    private static final String DCY_USERNAME = "dcy";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DemoUserInitializer demoUserInitializer;

    @Autowired
    private SecurityProperties securityProperties;

    @BeforeEach
    void seedDemoUsers() {
        // 认证已改为数据库校验，按正常启动路径补齐演示账号（幂等，重复调用无副作用）。
        demoUserInitializer.run(new DefaultApplicationArguments());
    }

    @Test
    @DisplayName("BUG-20261019-001 回归：连续五次登录全部成功")
    void repeatedLoginAlwaysSucceeds() {
        for (int i = 1; i <= 5; i++) {
            ResponseEntity<JsonNode> response = login(DEMO_USERNAME, demoPassword(DEMO_USERNAME), null);

            assertThat(response.getStatusCode())
                    .as("第 %d 次登录的 HTTP 状态", i)
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .as("第 %d 次登录的响应体", i)
                    .isNotNull();
            assertThat(response.getBody().get("code").asText())
                    .as("第 %d 次登录的业务码", i)
                    .isEqualTo("OK");
            assertThat(response.getBody().get("data").get("accessToken").asText())
                    .as("第 %d 次登录应返回令牌", i)
                    .isNotBlank();
        }
    }

    @Test
    @DisplayName("BUG-20261019-002 回归：白名单内来源放行并回显")
    void allowedOriginsAreAccepted() {
        List<String> allowed = List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://192.168.62.111:5173");

        for (String origin : allowed) {
            ResponseEntity<JsonNode> response = login(DEMO_USERNAME, demoPassword(DEMO_USERNAME), origin);

            assertThat(response.getStatusCode())
                    .as("来源 %s 应被放行", origin)
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getAccessControlAllowOrigin())
                    .as("来源 %s 应回显在响应头", origin)
                    .isEqualTo(origin);
        }
    }

    @Test
    @DisplayName("BUG-20261019-002 回归：白名单外来源返回 403")
    void unknownOriginIsRejected() {
        // CORS 拒绝发生在进入 Controller 之前，响应为无 Content-Type 的纯文本，
        // 不是统一响应结构，因此按原始字节断言。
        ResponseEntity<byte[]> response =
                post(LOGIN_PATH, credentialsBody(DEMO_USERNAME, demoPassword(DEMO_USERNAME)),
                        "http://evil.example.com", byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getHeaders().getAccessControlAllowOrigin())
                .as("被拒绝的来源不应回显")
                .isNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(new String(response.getBody(), StandardCharsets.UTF_8))
                .as("响应体应说明拒绝原因")
                .isEqualTo("Invalid CORS request");
    }

    @Test
    @DisplayName("端到端：登录后携带令牌可读取当前用户与角色")
    void loginThenReadCurrentUser() {
        ResponseEntity<JsonNode> loginResponse = login(DEMO_USERNAME, demoPassword(DEMO_USERNAME), "http://localhost:5173");
        String token = loginResponse.getBody().get("data").get("accessToken").asText();

        JsonNode user = readCurrentUser(token);
        assertThat(user.get("username").asText()).isEqualTo(DEMO_USERNAME);

        List<String> roles = new ArrayList<>();
        user.get("roles").forEach(node -> roles.add(node.asText()));
        assertThat(roles).containsExactlyInAnyOrder("ADMIN", "USER");
    }

    @Test
    @DisplayName("数据库校验：附加演示账号 dcy 登录成功且角色为 USER")
    void additionalDemoUserDcyCanLogin() {
        ResponseEntity<JsonNode> response = login(DCY_USERNAME, demoPassword(DCY_USERNAME), "http://localhost:5173");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code").asText()).isEqualTo("OK");

        JsonNode user = readCurrentUser(response.getBody().get("data").get("accessToken").asText());
        assertThat(user.get("username").asText()).isEqualTo(DCY_USERNAME);

        List<String> roles = new ArrayList<>();
        user.get("roles").forEach(node -> roles.add(node.asText()));
        assertThat(roles).containsExactly("USER");
    }

    @Test
    @DisplayName("数据库校验：库中不存在的账号登录返回 401")
    void unknownUsernameReturnsUnauthorized() {
        ResponseEntity<JsonNode> response = login("no-such-user", demoPassword(DEMO_USERNAME), "http://localhost:5173");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code").asText()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("数据库校验：多账号交替连续登录全部成功（BUG-20261019-001 数据库版回归）")
    void alternatingUsersLoginRepeatedly() {
        for (int i = 1; i <= 3; i++) {
            assertThat(login(DEMO_USERNAME, demoPassword(DEMO_USERNAME), null).getStatusCode())
                    .as("第 %d 轮 admin 登录", i)
                    .isEqualTo(HttpStatus.OK);
            assertThat(login(DCY_USERNAME, demoPassword(DCY_USERNAME), null).getStatusCode())
                    .as("第 %d 轮 dcy 登录", i)
                    .isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    @DisplayName("鉴权：密码错误返回 401 且不影响后续正确登录")
    void wrongPasswordReturnsUnauthorizedAndDoesNotBreakLaterLogin() {
        ResponseEntity<JsonNode> failed = login(DEMO_USERNAME, "wrong-password", "http://localhost:5173");

        assertThat(failed.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(failed.getBody()).isNotNull();
        assertThat(failed.getBody().get("code").asText()).isEqualTo("UNAUTHORIZED");

        assertThat(login(DEMO_USERNAME, demoPassword(DEMO_USERNAME), "http://localhost:5173").getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("鉴权：未携带令牌访问受保护接口返回 401")
    void protectedEndpointRequiresToken() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(ME_PATH, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code").asText()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("鉴权：伪造令牌访问受保护接口返回 401")
    void invalidTokenIsRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("http://localhost:5173");
        headers.setBearerAuth("forged.token.value");
        ResponseEntity<JsonNode> response =
                restTemplate.exchange(ME_PATH, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private JsonNode readCurrentUser(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("http://localhost:5173");
        headers.setBearerAuth(token);
        ResponseEntity<JsonNode> meResponse =
                restTemplate.exchange(ME_PATH, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).isNotNull();
        assertThat(meResponse.getBody().get("code").asText()).isEqualTo("OK");
        return meResponse.getBody().get("data");
    }

    /**
     * 从配置读取演示账号口令，避免在测试源码中写入明文（规则 8.7）。
     */
    private String demoPassword(String username) {
        SecurityProperties.DemoUser primary = securityProperties.demoUser();
        if (primary.username().equals(username)) {
            return primary.password();
        }
        return securityProperties.additionalDemoUsers().stream()
                .filter(user -> user.username().equals(username))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未在配置中声明的演示账号: " + username))
                .password();
    }

    private ResponseEntity<JsonNode> login(String username, String password, String origin) {
        return post(LOGIN_PATH, credentialsBody(username, password), origin, JsonNode.class);
    }

    private <T> ResponseEntity<T> post(String path, String body, String origin, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (origin != null) {
            headers.setOrigin(origin);
        }
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
    }

    private String credentialsBody(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }
}
