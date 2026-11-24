package com.example.mediacmslab;

import com.example.mediacmslab.dto.ContentRequest;
import com.example.mediacmslab.entity.Content;
import com.example.mediacmslab.service.ContentService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "jwt.secret=local-integration-test-signing-key-for-mediacms",
        "demo.editor.password=editor-password"
})
class MediaCmsLabApplicationTests {
    @Container
    static final GenericContainer<?> postgres = new GenericContainer<>("postgres:16-alpine")
            .withEnv("POSTGRES_DB", "mediacms").withEnv("POSTGRES_USER", "mediacms")
            .withEnv("POSTGRES_PASSWORD", "mediacms").withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));
    @Container
    static final GenericContainer<?> redisServer = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry props) {
        props.add("spring.datasource.url", () -> "jdbc:postgresql://" + postgres.getHost()
                + ":" + postgres.getMappedPort(5432) + "/mediacms");
        props.add("spring.data.redis.host", redisServer::getHost);
        props.add("spring.data.redis.port", () -> redisServer.getMappedPort(6379));
    }

    @LocalServerPort int port;
    @Autowired ContentService contents;
    @Autowired TransactionTemplate transaction;
    @Autowired ObjectMapper json;
    @Autowired StringRedisTemplate redis;
    private final HttpClient client = HttpClient.newHttpClient();

    private String request(String method, String path, String token, Object body, int expected) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        var publisher = HttpRequest.BodyPublishers.noBody();
        if (body != null) {
            builder.header("Content-Type", "application/json");
            publisher = HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body));
        }
        var result = client.send(builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(result.statusCode()).as("%s %s -> %s", method, path, result.body()).isEqualTo(expected);
        return result.body();
    }

    private JsonNode data(String method, String path, String token, Object body, int status) throws Exception {
        return json.readTree(request(method, path, token, body, status));
    }

    private String editor() throws Exception {
        return data("POST", "/api/auth/login", null, Map.of("email", "editor@mediacms.local",
                "password", "editor-password"), 200).get("token").asString();
    }

    private double creations() throws Exception {
        String scrape = request("GET", "/actuator/prometheus", null, null, 200);
        assertThat(scrape.lines().filter(line -> line.contains("messages")).toList())
                .anyMatch(line -> line.startsWith("messages_created_total "));
        return scrape.lines()
                .filter(s -> s.startsWith("messages_created_total ")).map(s -> s.split(" ")[1])
                .mapToDouble(Double::parseDouble).findFirst().orElseThrow();
    }

    @Test
    void editorialWorkflowCountsOnlyCommittedCreationsAndInvalidatesCachedDrafts() throws Exception {
        String token = editor();
        double before = creations();
        var article = Map.of("type", "ARTICLE", "status", "PUBLISHED", "title", "Редакция",
                "body", "Первый выпуск", "tags", List.of("медиа"));
        String root = "/api/content";
        request("POST", root, null, article, 401);
        request("POST", root, token, Map.of("type", "VIDEO", "title", "V", "status", "PUBLISHED"), 400);
        var saved = data("POST", root, token, article, 201);
        assertThat(saved.get("publishedAt").isTextual()).isTrue();
        assertThat(saved.get("authorEmail").asString()).isEqualTo("editor@mediacms.local");
        String id = saved.get("id").asString();
        String path = root + "/" + id;
        assertThat(data("GET", path, null, null, 200).get("body").asString()).isEqualTo("Первый выпуск");
        assertThat(redis.getExpire("content::" + id)).isBetween(1L, 300L);
        var video = Map.of("type", "VIDEO", "status", "DRAFT", "title", "Интервью",
                "mediaUrl", "https://example.org/v.mp4", "durationSeconds", 90);
        data("PUT", path, token, video, 200);
        assertThat(redis.hasKey("content::" + id)).isFalse();
        request("GET", path, null, null, 404);
        request("POST", path + "/view", null, null, 404);
        assertThat(data("GET", path, token, null, 200).get("durationSeconds").asInt()).isEqualTo(90);
        assertThat(request("GET", root, null, null, 200)).doesNotContain("Интервью");
        assertThat(request("GET", root, token, null, 200)).contains("Интервью");
        var podcast = Map.of("type", "PODCAST", "status", "PUBLISHED", "title", "Голоса",
                "mediaUrl", "https://example.org/p.mp3", "episodes", List.of("Начало", "Финал"));
        data("PUT", path, token, podcast, 200);
        assertThat(data("GET", path, null, null, 200).get("episodes").get(1).asString()).isEqualTo("Финал");
        request("DELETE", path, token, null, 204);
        request("GET", path, null, null, 404);
        assertThat(creations()).isEqualTo(before + 1);
    }

    @Test
    void readerRegistrationLoginAndRoleRestrictions() throws Exception {
        var reader = Map.of("email", "reader@test.org", "password", "reader-password", "role", "ADMIN");
        var created = data("POST", "/api/auth/register", null, reader, 200);
        assertThat(created.get("role").asString()).isEqualTo("USER");
        String token = created.get("token").asString();
        request("GET", "/api/analytics/top", token, null, 403);
        request("POST", "/api/content", token, Map.of(), 403);
        request("POST", "/api/auth/register", null, reader, 409);
        request("POST", "/api/auth/login", null, Map.of("email", "reader@test.org", "password", "wrong"), 401);
        request("POST", "/api/auth/login", null, reader, 200);
        request("GET", "/api/analytics/top", "malformed", null, 401);
    }

    @Test
    void realViewsRankPublicationsAndExpireDailyKeys() throws Exception {
        String token = editor();
        String first = createArticle(token, "Утро");
        String second = createArticle(token, "День");
        request("POST", "/api/content/" + second + "/view", null, null, 200);
        String path = "/api/content/" + first + "/view";
        assertThat(request("POST", path, token, null, 200)).isEqualTo("1");
        assertThat(request("POST", path, token, null, 200)).isEqualTo("2");
        assertThat(data("GET", "/api/recommendations/top", null, null, 200).get(0).get("id").asString()).isEqualTo(first);
        assertThat(data("GET", "/api/analytics/top", token, null, 200).get(1).asString()).isEqualTo(second);
        assertThat(request("GET", "/api/analytics/dau", token, null, 200)).isEqualTo("1");
        String key = "analytics:top:views:" + LocalDate.now(ZoneOffset.UTC);
        assertThat(redis.opsForZSet().score(key, first)).isEqualTo(2);
        assertThat(redis.getExpire(key)).isBetween(1L, 172800L);
        request("DELETE", "/api/content/" + first, token, null, 204);
        assertThat(redis.hasKey("analytics:views:content:" + first)).isFalse();
        assertThat(data("GET", "/api/recommendations/top", null, null, 200).size()).isEqualTo(1);
    }

    private String createArticle(String token, String title) throws Exception {
        return data("POST", "/api/content", token, Map.of("type", "ARTICLE", "title", title,
                "status", "PUBLISHED", "body", "Текст"), 201).get("id").asString();
    }

    @Test
    void rolledBackArticleDoesNotIncrementPrometheus() throws Exception {
        double before = creations();
        transaction.executeWithoutResult(tx -> {
            var draft = ContentRequest.builder().title("Отмена").body("Текст").type(Content.ContentType.ARTICLE)
                    .status(Content.Status.DRAFT).build();
            contents.create(draft, "editor@mediacms.local");
            tx.setRollbackOnly();
        });
        assertThat(creations()).isEqualTo(before);
    }

    @Test
    void servesDemoOpenApiAndHealth() throws Exception {
        assertThat(request("GET", "/", null, null, 200)).contains("MediaCmsLab");
        assertThat(data("GET", "/actuator/health", null, null, 200).get("status").asString()).isEqualTo("UP");
        assertThat(request("GET", "/actuator/info", null, null, 200)).contains("MediaCmsLab");
        assertThat(data("GET", "/v3/api-docs", null, null, 200).get("paths").has("/api/content/{id}")).isTrue();
        request("GET", "/swagger-ui/index.html", null, null, 200);
    }
}
