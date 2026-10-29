package com.web.motoridelab3api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.web.motoridelab3api.exception.TooManyRequestsException;
import com.web.motoridelab3api.model.User;
import com.web.motoridelab3api.repository.UserRepository;
import com.web.motoridelab3api.security.AuthProperties;
import com.web.motoridelab3api.security.AuthRateLimiter;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.crypto.spec.SecretKeySpec;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BackendIntegrationTest {
    @Container static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:16-alpine");
    static final String SECRET;

    static {
        byte[] value = new byte[32];
        new SecureRandom().nextBytes(value);
        SECRET = Base64.getEncoder().encodeToString(value);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", DB::getJdbcUrl);
        r.add("spring.datasource.username", DB::getUsername);
        r.add("spring.datasource.password", DB::getPassword);
        r.add("app.auth.jwt-secret", () -> SECRET);
        r.add("app.auth.cookie-secure", () -> false);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired UserRepository users;
    @Autowired JwtEncoder encoder;
    @Autowired AuthProperties properties;
    @Autowired AuthRateLimiter rateLimiter;
    private static final String PASSWORD = "A-long-test-password-42";

    record Session(String token, Cookie refresh, long id) {}

    @BeforeEach
    void reset() {
        inTransaction(
                () -> {
                    for (String entity :
                            List.of(
                                    "Reaction",
                                    "PostComment",
                                    "Post",
                                    "Favorite",
                                    "Review",
                                    "RefreshSession",
                                    "User",
                                    "AuthRateLimit"))
                        entityManager.createQuery("delete from " + entity).executeUpdate();
                    return null;
                });
    }

    private <T> T inTransaction(Supplier<T> operation) {
        return new TransactionTemplate(transactionManager).execute(status -> operation.get());
    }

    MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, Object value) {
        return request.contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(value));
    }

    ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request);
    }

    RequestPostProcessor csrfToken() {
        return request -> {
            try {
                var result = mvc.perform(get("/api/auth/csrf")).andReturn();
                var value = json.readTree(result.getResponse().getContentAsString());
                var cookies = new ArrayList<Cookie>();
                if (request.getCookies() != null)
                    cookies.addAll(Arrays.asList(request.getCookies()));
                cookies.add(result.getResponse().getCookie("XSRF-TOKEN"));
                request.setCookies(cookies.toArray(Cookie[]::new));
                request.addHeader(value.get("headerName").asText(), value.get("token").asText());
                return request;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
    }

    Session session(MvcResult result) throws Exception {
        var node = json.readTree(result.getResponse().getContentAsString());
        return new Session(
                node.get("accessToken").asText(),
                result.getResponse().getCookie("refresh_token"),
                node.get("user").get("id").asLong());
    }

    Session register(String email) throws Exception {
        return session(
                perform(
                                body(
                                        post("/api/auth/register").with(csrfToken()),
                                        Map.of(
                                                "email",
                                                email,
                                                "password",
                                                PASSWORD,
                                                "displayName",
                                                "Rider")))
                        .andExpect(status().isCreated())
                        .andExpect(header().string("Location", "/api/users/me"))
                        .andReturn());
    }

    Session admin() throws Exception {
        var admin = register("admin@example.org");
        var user = users.findById(admin.id()).orElseThrow();
        user.setRole(User.Role.ADMIN);
        users.saveAndFlush(user);
        return login("admin@example.org");
    }

    Session login(String email) throws Exception {
        return session(
                perform(
                                body(
                                        post("/api/auth/login").with(csrfToken()),
                                        Map.of("email", email, "password", PASSWORD)))
                        .andExpect(status().isOk())
                        .andReturn());
    }

    MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder r, Session s) {
        return r.header("Authorization", "Bearer " + s.token());
    }

    Map<String, Object> bike(String model) {
        return Map.of(
                "brand",
                "Test",
                "model",
                model,
                "engineCc",
                500,
                "horsepower",
                45,
                "description",
                "A test motorcycle",
                "imageUrl",
                "/images/test.png",
                "price",
                123.45,
                "categoryId",
                1,
                "technologyIds",
                List.of(1));
    }

    @Test
    void registrationLoginProfileAndNoSecrets() throws Exception {
        var user = register("Rider@Example.org");
        assertNotNull(user.refresh());
        assertTrue(user.refresh().isHttpOnly());
        assertEquals("/api/auth", user.refresh().getPath());
        perform(authorized(get("/api/users/me"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("rider@example.org"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertNotEquals(PASSWORD, users.findById(user.id()).orElseThrow().getPasswordHash());
        login("rider@example.org");
        perform(authorized(body(put("/api/users/me"), Map.of("displayName", "New name")), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("New name"));
        perform(
                        body(
                                post("/api/auth/login").with(csrfToken()),
                                Map.of("email", "rider@example.org", "password", "wrong")))
                .andExpect(status().isUnauthorized());
        perform(
                        body(
                                post("/api/auth/register").with(csrfToken()),
                                Map.of(
                                        "email",
                                        "rider@example.org",
                                        "password",
                                        PASSWORD,
                                        "displayName",
                                        "Other")))
                .andExpect(status().isConflict());
        perform(
                        body(
                                post("/api/auth/register").with(csrfToken()),
                                Map.of(
                                        "email",
                                        "evil@example.org",
                                        "password",
                                        PASSWORD,
                                        "displayName",
                                        "Evil",
                                        "role",
                                        "ADMIN")))
                .andExpect(status().isBadRequest());
        assertTrue(users.findByEmail("evil@example.org").isEmpty());
    }

    @Test
    void csrfAndCorsProtocol() throws Exception {
        perform(
                        body(
                                post("/api/auth/login"),
                                Map.of("email", "rider@example.org", "password", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        var result = perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var node = json.readTree(result.getResponse().getContentAsString());
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        perform(
                        body(
                                post("/api/auth/register")
                                        .cookie(cookie)
                                        .header(
                                                node.get("headerName").asText(),
                                                node.get("token").asText()),
                                Map.of(
                                        "email",
                                        "csrf@example.org",
                                        "password",
                                        PASSWORD,
                                        "displayName",
                                        "CSRF")))
                .andExpect(status().isCreated());
        perform(
                        options("/api/users/me")
                                .header("Origin", "http://localhost:5173")
                                .header("Access-Control-Request-Method", "PUT")
                                .header(
                                        "Access-Control-Request-Headers",
                                        "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        perform(
                        options("/api/users/me")
                                .header("Origin", "https://evil.example")
                                .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void realCrudSecurityValidationAndAllMethods() throws Exception {
        var admin = admin();
        var user = register("user@example.org");
        perform(body(post("/api/motorcycles"), bike("CB"))).andExpect(status().isUnauthorized());
        perform(authorized(body(post("/api/motorcycles"), bike("CB")), user))
                .andExpect(status().isForbidden());
        var bad = new HashMap<>(bike("CB"));
        bad.put("price", -1);
        perform(authorized(body(post("/api/motorcycles"), bad), admin))
                .andExpect(status().isBadRequest());
        var created =
                perform(authorized(body(post("/api/motorcycles"), bike("CB")), admin))
                        .andExpect(status().isCreated())
                        .andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        assertEquals("/api/motorcycles/" + id, created.getResponse().getHeader("Location"));
        perform(get("/api/motorcycles/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.technologies[0]").exists());
        perform(head("/api/motorcycles/" + id))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
        perform(authorized(body(put("/api/motorcycles/" + id), bike("Updated")), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("Updated"));
        perform(
                        get("/api/motorcycles")
                                .param("size", "1")
                                .param("search", "Test")
                                .param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(header().exists("X-Total-Count"));
        perform(authorized(delete("/api/categories/1"), admin)).andExpect(status().isConflict());
        perform(authorized(delete("/api/technologies/1"), admin)).andExpect(status().isConflict());
        perform(authorized(delete("/api/motorcycles/" + id), admin))
                .andExpect(status().isNoContent());
        perform(head("/api/motorcycles/" + id))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void expiredJwtAndUnauthenticatedAccess() throws Exception {
        var user = register("user@example.org");
        var claims =
                JwtClaimsSet.builder()
                        .issuer(properties.issuer())
                        .subject("" + user.id())
                        .claim("role", "USER")
                        .issuedAt(Instant.now().minusSeconds(600))
                        .expiresAt(Instant.now().minusSeconds(120))
                        .build();
        var expired =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                        .getTokenValue();
        perform(get("/api/users/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        perform(get("/api/favorites")).andExpect(status().isUnauthorized());
    }

    @Test
    void rotationReplayAndLogout() throws Exception {
        var user = register("user@example.org");
        var rotated =
                session(
                        perform(post("/api/auth/refresh").with(csrfToken()).cookie(user.refresh()))
                                .andExpect(status().isOk())
                                .andReturn());
        assertNotEquals(user.refresh().getValue(), rotated.refresh().getValue());
        assertEquals(
                64,
                inTransaction(
                        () ->
                                entityManager
                                        .createQuery(
                                                "select length(s.tokenHash) from RefreshSession s",
                                                Integer.class)
                                        .setMaxResults(1)
                                        .getSingleResult()));
        perform(post("/api/auth/refresh").with(csrfToken()).cookie(user.refresh()))
                .andExpect(status().isUnauthorized());
        perform(post("/api/auth/refresh").with(csrfToken()).cookie(rotated.refresh()))
                .andExpect(status().isUnauthorized());
        var another = login("user@example.org");
        perform(post("/api/auth/logout").with(csrfToken()).cookie(another.refresh()))
                .andExpect(status().isNoContent());
        perform(post("/api/auth/refresh").with(csrfToken()).cookie(another.refresh()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutAllAndPasswordChangeRevokeSessions() throws Exception {
        var first = register("user@example.org");
        var second = login("user@example.org");
        perform(authorized(post("/api/auth/logout-all").with(csrfToken()), first))
                .andExpect(status().isNoContent());
        for (var s : List.of(first, second))
            perform(post("/api/auth/refresh").with(csrfToken()).cookie(s.refresh()))
                    .andExpect(status().isUnauthorized());
        var third = login("user@example.org");
        perform(
                        authorized(
                                body(
                                        post("/api/auth/change-password").with(csrfToken()),
                                        Map.of(
                                                "currentPassword",
                                                "wrong",
                                                "newPassword",
                                                "Another-long-password-43")),
                                third))
                .andExpect(status().isUnauthorized());
        perform(
                        authorized(
                                body(
                                        post("/api/auth/change-password").with(csrfToken()),
                                        Map.of(
                                                "currentPassword",
                                                PASSWORD,
                                                "newPassword",
                                                "Another-long-password-43")),
                                third))
                .andExpect(status().isNoContent());
        perform(post("/api/auth/refresh").with(csrfToken()).cookie(third.refresh()))
                .andExpect(status().isUnauthorized());
        perform(
                        body(
                                post("/api/auth/login").with(csrfToken()),
                                Map.of("email", "user@example.org", "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentRefreshCannotCreateTwoLiveDescendants() throws Exception {
        var user = register("user@example.org");
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<MvcResult> task =
                    () -> {
                        start.await();
                        return perform(
                                        post("/api/auth/refresh")
                                                .with(csrfToken())
                                                .cookie(user.refresh()))
                                .andReturn();
                    };
            var a = pool.submit(task);
            var b = pool.submit(task);
            start.countDown();
            var results = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertEquals(
                    List.of(200, 401),
                    results.stream().map(r -> r.getResponse().getStatus()).sorted().toList());
            assertEquals(
                    0,
                    inTransaction(
                                    () ->
                                            entityManager
                                                    .createQuery(
                                                            "select count(s) from RefreshSession s"
                                                                    + " where s.revokedAt is null",
                                                            Long.class)
                                                    .getSingleResult())
                            .intValue());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void favoritesAndReviewsAreOwnedAndModerated() throws Exception {
        var first = register("first@example.org");
        var second = register("second@example.org");
        perform(authorized(post("/api/favorites/1"), first)).andExpect(status().isCreated());
        perform(authorized(get("/api/favorites"), second))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        perform(authorized(delete("/api/favorites/1"), second)).andExpect(status().isNotFound());
        perform(authorized(post("/api/favorites/1"), second)).andExpect(status().isCreated());
        perform(authorized(post("/api/favorites/1"), first)).andExpect(status().isConflict());
        perform(authorized(delete("/api/favorites/1"), first)).andExpect(status().isNoContent());
        perform(authorized(get("/api/favorites"), second))
                .andExpect(jsonPath("$.length()").value(1));
        perform(body(post("/api/motorcycles/1/reviews"), Map.of("rating", 5, "text", "Great")))
                .andExpect(status().isUnauthorized());
        perform(
                        authorized(
                                body(
                                        post("/api/motorcycles/1/reviews"),
                                        Map.of("rating", 6, "text", "Great")),
                                first))
                .andExpect(status().isBadRequest());
        var review =
                perform(
                                authorized(
                                        body(
                                                post("/api/motorcycles/1/reviews"),
                                                Map.of("rating", 5, "text", "Great")),
                                        first))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.authorId").value(first.id()))
                        .andReturn();
        long id = json.readTree(review.getResponse().getContentAsString()).get("id").asLong();
        perform(
                        authorized(
                                body(
                                        put("/api/reviews/" + id),
                                        Map.of("rating", 4, "text", "Changed")),
                                second))
                .andExpect(status().isForbidden());
        perform(authorized(delete("/api/reviews/" + id), second)).andExpect(status().isForbidden());
        perform(
                        authorized(
                                body(
                                        put("/api/reviews/" + id),
                                        Map.of("rating", 4, "text", "Changed")),
                                first))
                .andExpect(status().isOk());
        perform(authorized(delete("/api/reviews/" + id), admin()))
                .andExpect(status().isNoContent());
        perform(get("/api/motorcycles/999999/reviews")).andExpect(status().isNotFound());
    }

    @Test
    void throttlesRepeatedLoginAttempts() throws Exception {
        for (int i = 0; i < 10; i++)
            perform(
                            body(
                                    post("/api/auth/login").with(csrfToken()),
                                    Map.of("email", "unknown@example.org", "password", PASSWORD)))
                    .andExpect(status().isUnauthorized());
        perform(
                        body(
                                post("/api/auth/login").with(csrfToken()),
                                Map.of("email", "unknown@example.org", "password", PASSWORD)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void openApiAndSwaggerAvailable() throws Exception {
        perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/motorcycles/{id}'].head").exists())
                .andExpect(
                        jsonPath("$.paths['/api/motorcycles/{id}'].head.responses['200'].content")
                                .doesNotExist())
                .andExpect(jsonPath("$.paths['/api/motorcycles'].get.security").isEmpty())
                .andExpect(
                        jsonPath("$.paths['/api/motorcycles'].post.security[0].BearerAuth")
                                .exists())
                .andExpect(
                        jsonPath("$.paths['/api/posts/{id}/reaction'].get.responses['200']")
                                .exists())
                .andExpect(
                        jsonPath("$.paths['/api/posts/{id}/reaction'].get.security[0].BearerAuth")
                                .exists());
        perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void catalogReferenceCrudAndNews() throws Exception {
        var admin = admin();
        var category =
                perform(
                                authorized(
                                        body(
                                                post("/api/categories"),
                                                Map.of(
                                                        "name",
                                                        "Custom",
                                                        "slug",
                                                        "custom",
                                                        "description",
                                                        "Test")),
                                        admin))
                        .andExpect(status().isCreated())
                        .andReturn();
        long categoryId =
                json.readTree(category.getResponse().getContentAsString()).get("id").asLong();
        perform(get("/api/categories/custom")).andExpect(status().isOk());
        perform(
                        authorized(
                                body(
                                        post("/api/categories"),
                                        Map.of("name", "Other", "slug", "custom")),
                                admin))
                .andExpect(status().isConflict());
        perform(
                        authorized(
                                body(
                                        put("/api/categories/" + categoryId),
                                        Map.of("name", "Renamed", "slug", "custom-new")),
                                admin))
                .andExpect(status().isOk());
        perform(authorized(delete("/api/categories/" + categoryId), admin))
                .andExpect(status().isNoContent());
        var technology =
                perform(
                                authorized(
                                        body(
                                                post("/api/technologies"),
                                                Map.of("name", "Custom tech")),
                                        admin))
                        .andExpect(status().isCreated())
                        .andReturn();
        long techId =
                json.readTree(technology.getResponse().getContentAsString()).get("id").asLong();
        perform(get("/api/technologies/" + techId)).andExpect(status().isOk());
        perform(
                        authorized(
                                body(
                                        put("/api/technologies/" + techId),
                                        Map.of("name", "Updated tech")),
                                admin))
                .andExpect(status().isOk());
        perform(authorized(delete("/api/technologies/" + techId), admin))
                .andExpect(status().isNoContent());
        var news =
                Map.of(
                        "title",
                        "News",
                        "content",
                        "Article text",
                        "imageUrl",
                        "/images/news.png",
                        "publishedAt",
                        "2026-10-29T10:00:00");
        var article =
                perform(authorized(body(post("/api/news"), news), admin))
                        .andExpect(status().isCreated())
                        .andReturn();
        long newsId = json.readTree(article.getResponse().getContentAsString()).get("id").asLong();
        var update = new HashMap<>(news);
        update.put("publishedAt", "2026-10-30T10:00:00");
        perform(authorized(body(put("/api/news/" + newsId), update), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedAt").value("2026-10-30T10:00:00"));
        perform(get("/api/news/" + newsId)).andExpect(status().isOk());
        perform(authorized(delete("/api/news/" + newsId), admin)).andExpect(status().isNoContent());
    }

    @Test
    void feedCommentsReactionsOwnershipAndCascades() throws Exception {
        var first = register("first@example.org");
        var second = register("second@example.org");
        var input =
                Map.of(
                        "title",
                        "First ride",
                        "content",
                        "I rode a motorcycle today",
                        "motorcycleId",
                        1);
        perform(body(post("/api/posts"), input)).andExpect(status().isUnauthorized());
        var created =
                perform(authorized(body(post("/api/posts"), input), first))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.author.id").value(first.id()))
                        .andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        perform(get("/api/posts").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        perform(authorized(body(put("/api/posts/" + id), input), second))
                .andExpect(status().isForbidden());
        perform(authorized(body(put("/api/posts/" + id), input), first)).andExpect(status().isOk());
        var comment =
                perform(
                                authorized(
                                        body(
                                                post("/api/posts/" + id + "/comments"),
                                                Map.of("text", "Great ride")),
                                        second))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.author.id").value(second.id()))
                        .andReturn();
        long commentId =
                json.readTree(comment.getResponse().getContentAsString()).get("id").asLong();
        perform(get("/api/posts/" + id + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].text").value("Great ride"));
        perform(
                        authorized(
                                body(put("/api/comments/" + commentId), Map.of("text", "Changed")),
                                first))
                .andExpect(status().isForbidden());
        perform(
                        authorized(
                                body(put("/api/comments/" + commentId), Map.of("text", "Changed")),
                                second))
                .andExpect(status().isOk());
        for (var user : List.of(first, second))
            perform(
                            authorized(
                                    body(
                                            put("/api/posts/" + id + "/reaction"),
                                            Map.of("kind", "LIKE")),
                                    user))
                    .andExpect(status().isNoContent());
        perform(
                        authorized(
                                body(put("/api/posts/" + id + "/reaction"), Map.of("kind", "LIKE")),
                                first))
                .andExpect(status().isNoContent());
        perform(get("/api/posts/" + id + "/reactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.LIKE").value(2));
        perform(
                        authorized(
                                body(put("/api/posts/" + id + "/reaction"), Map.of("kind", "LOVE")),
                                first))
                .andExpect(status().isNoContent());
        perform(get("/api/posts/" + id + "/reactions"))
                .andExpect(jsonPath("$.LIKE").value(1))
                .andExpect(jsonPath("$.LOVE").value(1));
        perform(authorized(delete("/api/posts/" + id + "/reaction"), second))
                .andExpect(status().isNoContent());
        perform(
                        authorized(
                                body(
                                        put("/api/comments/" + commentId + "/reaction"),
                                        Map.of("kind", "HELPFUL")),
                                first))
                .andExpect(status().isNoContent());
        perform(get("/api/comments/" + commentId + "/reactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.HELPFUL").value(1));
        perform(authorized(delete("/api/comments/" + commentId), first))
                .andExpect(status().isForbidden());
        perform(authorized(delete("/api/posts/" + id), second)).andExpect(status().isForbidden());
        perform(authorized(delete("/api/posts/" + id), admin())).andExpect(status().isNoContent());
        perform(get("/api/comments/" + commentId)).andExpect(status().isNotFound());
        assertEquals(
                0,
                inTransaction(
                                () ->
                                        entityManager
                                                .createQuery(
                                                        "select count(r) from Reaction r",
                                                        Long.class)
                                                .getSingleResult())
                        .intValue());
    }

    @Test
    void ownReactionsArePrivateAndReviewReactionsCascade() throws Exception {
        var first = register("first@example.org");
        var second = register("second@example.org");
        var review =
                perform(
                                authorized(
                                        body(
                                                post("/api/motorcycles/1/reviews"),
                                                Map.of("rating", 5, "text", "Excellent")),
                                        first))
                        .andExpect(status().isCreated())
                        .andReturn();
        long id = json.readTree(review.getResponse().getContentAsString()).get("id").asLong();
        perform(get("/api/reviews/" + id + "/reaction")).andExpect(status().isUnauthorized());
        perform(
                        authorized(
                                body(
                                        put("/api/reviews/" + id + "/reaction"),
                                        Map.of("kind", "HELPFUL")),
                                first))
                .andExpect(status().isNoContent());
        perform(authorized(get("/api/reviews/" + id + "/reaction"), first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("HELPFUL"));
        perform(authorized(get("/api/reviews/" + id + "/reaction"), second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").isEmpty());
        perform(authorized(delete("/api/reviews/" + id + "/reaction"), second))
                .andExpect(status().isNoContent());
        perform(get("/api/reviews/" + id + "/reactions")).andExpect(jsonPath("$.HELPFUL").value(1));
        perform(authorized(delete("/api/reviews/" + id), first)).andExpect(status().isNoContent());
        assertEquals(
                0,
                inTransaction(
                                () ->
                                        entityManager
                                                .createQuery(
                                                        "select count(r) from Reaction r",
                                                        Long.class)
                                                .getSingleResult())
                        .intValue());
    }

    @Test
    void refreshExpiryDisabledAccountAndLogoutIsolation() throws Exception {
        var first = register("user@example.org");
        var second = login("user@example.org");
        perform(post("/api/auth/logout").with(csrfToken()).cookie(first.refresh()))
                .andExpect(status().isNoContent());
        var rotated =
                session(
                        perform(
                                        post("/api/auth/refresh")
                                                .with(csrfToken())
                                                .cookie(second.refresh()))
                                .andExpect(status().isOk())
                                .andReturn());
        inTransaction(
                () ->
                        entityManager
                                .createQuery(
                                        "update RefreshSession s set s.expiresAt=:expires where"
                                                + " s.revokedAt is null")
                                .setParameter("expires", Instant.now().minusSeconds(1))
                                .executeUpdate());
        perform(post("/api/auth/refresh").with(csrfToken()).cookie(rotated.refresh()))
                .andExpect(status().isUnauthorized());
        var third = login("user@example.org");
        inTransaction(
                () ->
                        entityManager
                                .createQuery("update User u set u.enabled=false where u.id=:id")
                                .setParameter("id", third.id())
                                .executeUpdate());
        perform(authorized(get("/api/users/me"), third)).andExpect(status().isUnauthorized());
        perform(post("/api/auth/refresh").with(csrfToken()).cookie(third.refresh()))
                .andExpect(status().isUnauthorized());
        perform(
                        body(
                                post("/api/auth/login").with(csrfToken()),
                                Map.of("email", "user@example.org", "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsUnicodePasswordsAndRejectsBcryptTruncation() throws Exception {
        String password = "МойМотоциклетныйПароль42";
        perform(
                        body(
                                post("/api/auth/register").with(csrfToken()),
                                Map.of(
                                        "email",
                                        "unicode@example.org",
                                        "password",
                                        password,
                                        "displayName",
                                        "Райдер")))
                .andExpect(status().isCreated());
        perform(
                        body(
                                post("/api/auth/login").with(csrfToken()),
                                Map.of("email", "unicode@example.org", "password", password)))
                .andExpect(status().isOk());
        perform(
                        body(
                                post("/api/auth/register").with(csrfToken()),
                                Map.of(
                                        "email",
                                        "long@example.org",
                                        "password",
                                        "Я".repeat(40),
                                        "displayName",
                                        "Райдер")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void csrfRequiresBothCorrectCookieAndHeader() throws Exception {
        var result = perform(get("/api/auth/csrf")).andReturn();
        var value = json.readTree(result.getResponse().getContentAsString());
        var credentials =
                Map.of("email", "user@example.org", "password", PASSWORD, "displayName", "Rider");
        perform(
                        body(
                                post("/api/auth/register")
                                        .cookie(result.getResponse().getCookie("XSRF-TOKEN"))
                                        .header("X-XSRF-TOKEN", "invalid"),
                                credentials))
                .andExpect(status().isForbidden());
        perform(
                        body(
                                post("/api/auth/register")
                                        .header("X-XSRF-TOKEN", value.get("token").asText()),
                                credentials))
                .andExpect(status().isForbidden());
        assertTrue(users.findByEmail("user@example.org").isEmpty());
    }

    @Test
    void invalidJwtIssuerAndSignatureAreRejected() throws Exception {
        var user = register("user@example.org");
        var claims =
                JwtClaimsSet.builder()
                        .issuer("wrong-issuer")
                        .subject("" + user.id())
                        .claim("role", "USER")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(900))
                        .build();
        String wrongIssuer =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                        .getTokenValue();
        perform(get("/api/users/me").header("Authorization", "Bearer " + wrongIssuer))
                .andExpect(status().isUnauthorized());
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        var otherEncoder =
                new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(key, "HmacSHA256")));
        claims =
                JwtClaimsSet.builder()
                        .issuer(properties.issuer())
                        .subject("" + user.id())
                        .claim("role", "USER")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(900))
                        .build();
        String wrongSignature =
                otherEncoder
                        .encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                        .getTokenValue();
        perform(get("/api/users/me").header("Authorization", "Bearer " + wrongSignature))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validatesPaginationRelationshipsAndAuthorInput() throws Exception {
        var admin = admin();
        var user = register("user@example.org");
        for (String invalid : List.of("0", "101", "-1"))
            perform(get("/api/motorcycles").param("size", invalid))
                    .andExpect(status().isBadRequest());
        perform(get("/api/motorcycles").param("page", "-1")).andExpect(status().isBadRequest());
        perform(get("/api/motorcycles").param("sort", "unknown"))
                .andExpect(status().isBadRequest());
        for (String field : List.of("engineCc", "horsepower", "price")) {
            var invalid = new HashMap<String, Object>(bike("Invalid"));
            invalid.put(field, -1);
            perform(authorized(body(post("/api/motorcycles"), invalid), admin))
                    .andExpect(status().isBadRequest());
        }
        var invalidImage = new HashMap<String, Object>(bike("Invalid"));
        invalidImage.put("imageUrl", "javascript:alert(1)");
        perform(authorized(body(post("/api/motorcycles"), invalidImage), admin))
                .andExpect(status().isBadRequest());
        var missing = new HashMap<String, Object>(bike("Missing"));
        missing.put("technologyIds", List.of(999999));
        perform(authorized(body(post("/api/motorcycles"), missing), admin))
                .andExpect(status().isNotFound());
        perform(
                        authorized(
                                body(
                                        post("/api/motorcycles/1/reviews"),
                                        Map.of(
                                                "rating",
                                                5,
                                                "text",
                                                "Great",
                                                "authorId",
                                                admin.id())),
                                user))
                .andExpect(status().isBadRequest());
        perform(
                        authorized(
                                body(
                                        post("/api/motorcycles/1/reviews"),
                                        Map.of("rating", 5, "text", " ")),
                                user))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentRateLimitsDoNotLoseAttempts() throws Exception {
        var pool = Executors.newFixedThreadPool(4);
        var start = new CountDownLatch(1);
        try {
            var calls = new ArrayList<Future<Boolean>>();
            for (int index = 0; index < 20; index++)
                calls.add(
                        pool.submit(
                                () -> {
                                    start.await();
                                    try {
                                        rateLimiter.check(
                                                "concurrent", "127.0.0.1", "rider@example.org");
                                        return true;
                                    } catch (TooManyRequestsException ignored) {
                                        return false;
                                    }
                                }));
            start.countDown();
            int accepted = 0;
            for (var call : calls) if (call.get(20, TimeUnit.SECONDS)) accepted++;
            assertEquals(10, accepted);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentReactionUpdatesRemainUnique() throws Exception {
        var user = register("rider@example.org");
        var post =
                perform(
                                authorized(
                                        body(
                                                post("/api/posts"),
                                                Map.of(
                                                        "title",
                                                        "Concurrent reactions",
                                                        "content",
                                                        "Test post")),
                                        user))
                        .andExpect(status().isCreated())
                        .andReturn();
        long id = json.readTree(post.getResponse().getContentAsString()).get("id").asLong();
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var calls = new ArrayList<Future<Integer>>();
            for (String kind : List.of("LIKE", "LOVE"))
                calls.add(
                        pool.submit(
                                () -> {
                                    start.await();
                                    return perform(
                                                    authorized(
                                                            body(
                                                                    put(
                                                                            "/api/posts/"
                                                                                    + id
                                                                                    + "/reaction"),
                                                                    Map.of("kind", kind)),
                                                            user))
                                            .andReturn()
                                            .getResponse()
                                            .getStatus();
                                }));
            start.countDown();
            for (var call : calls) assertEquals(204, call.get(20, TimeUnit.SECONDS));
            assertEquals(
                    1L,
                    inTransaction(
                            () ->
                                    entityManager
                                            .createQuery(
                                                    "select count(r) from Reaction r", Long.class)
                                            .getSingleResult()));
        } finally {
            pool.shutdownNow();
        }
    }
}
