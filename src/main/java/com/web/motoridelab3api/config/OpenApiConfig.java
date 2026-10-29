package com.web.motoridelab3api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class OpenApiConfig {
    @Value("${app.bootstrap.email:}")
    private String bootstrapAdminEmail;

    private static final Set<String> ANONYMOUS_AUTH =
            Set.of(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/api/auth/logout",
                    "/api/auth/csrf");
    private static final List<String> PUBLIC_RESOURCES =
            List.of(
                    "/api/motorcycles",
                    "/api/categories",
                    "/api/technologies",
                    "/api/news",
                    "/api/posts",
                    "/api/comments",
                    "/api/reviews");

    @Bean
    public OpenAPI motoRideOpenApi() {
        var components =
                new Components()
                        .addSecuritySchemes(
                                "BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Access JWT; TTL по умолчанию 15 минут. В Authorize"
                                                        + " вставьте токен без Bearer."))
                        .addSecuritySchemes(
                                "CsrfHeader",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-XSRF-TOKEN")
                                        .description(
                                                "Маскированный token из GET /api/auth/csrf; браузер"
                                                    + " отправляет HttpOnly cookie XSRF-TOKEN."))
                        .addSecuritySchemes(
                                "RefreshCookie",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name("refresh_token")
                                        .description(
                                                "HttpOnly cookie, устанавливается сервером,"
                                                        + " ротируется при каждом refresh."))
                        .addSchemas(
                                "ErrorResponse",
                                new ObjectSchema()
                                        .addProperty("status", new IntegerSchema())
                                        .addProperty("message", new StringSchema())
                                        .addProperty("details", new StringSchema())
                                        .addProperty("path", new StringSchema())
                                        .addProperty("timestamp", new DateTimeSchema())
                                        .addProperty(
                                                "fieldErrors",
                                                new ObjectSchema().additionalProperties(true)));
        return new OpenAPI()
                .components(components)
                .info(
                        new Info()
                                .title("MotoRide API")
                                .version("2.0.0")
                                .description(
                                        "Лабораторная №5: HEAD, GET, POST, PUT, DELETE. Чтение"
                                            + " каталога/ленты публично; ADMIN управляет каталогом."
                                            + " USER создаёт отзывы, посты и комментарии от своего"
                                            + " имени; автор или ADMIN меняет контент. Перед POST"
                                            + " /api/auth/** получите GET /api/auth/csrf и"
                                            + " передайте token в X-XSRF-TOKEN вместе с cookies."
                                            + " Access JWT возвращается в JSON, refresh — только в"
                                            + " HttpOnly cookie Path=/api/auth. При refresh не"
                                            + " передавайте истёкший Bearer."));
    }

    @Bean
    OpenApiCustomizer endpointContracts() {
        return api ->
                api.getPaths()
                        .forEach(
                                (path, item) ->
                                        item.readOperationsMap()
                                                .forEach(
                                                        (method, operation) ->
                                                                customize(
                                                                        path, method, operation)));
    }

    private void customize(String path, PathItem.HttpMethod method, Operation operation) {
        boolean read = method == PathItem.HttpMethod.GET || method == PathItem.HttpMethod.HEAD;
        boolean authWrite = path.startsWith("/api/auth/") && !read;
        boolean publicRead =
                read
                        && !path.endsWith("/reaction")
                        && PUBLIC_RESOURCES.stream().anyMatch(path::startsWith);
        boolean protectedOperation = !(publicRead || ANONYMOUS_AUTH.contains(path));
        documentAccess(path, read, authWrite, protectedOperation, operation);
        documentResponses(path, method, authWrite, protectedOperation, operation);
        if (operation.getSummary() == null) operation.setSummary(method + " " + path);
        if (operation.getParameters() != null)
            operation
                    .getParameters()
                    .forEach(
                            parameter -> {
                                if (parameter.getName().equals("page"))
                                    parameter.setDescription("Номер страницы с 0");
                                if (parameter.getName().equals("size"))
                                    parameter.setDescription(
                                            "Размер страницы 1–100; по умолчанию 20");
                            });
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            Object example = example(path);
            if (example != null)
                operation
                        .getRequestBody()
                        .getContent()
                        .values()
                        .forEach(media -> media.setExample(example));
        }
    }

    private void documentAccess(
            String path,
            boolean read,
            boolean authWrite,
            boolean protectedOperation,
            Operation operation) {
        boolean admin =
                !read
                        && (path.startsWith("/api/categories")
                                || path.startsWith("/api/technologies")
                                || path.startsWith("/api/news")
                                || (path.startsWith("/api/motorcycles")
                                        && !path.endsWith("/reviews")));
        var requirement = new SecurityRequirement();
        if (protectedOperation) requirement.addList("BearerAuth");
        if (authWrite) requirement.addList("CsrfHeader");
        if (path.equals("/api/auth/refresh") || path.equals("/api/auth/logout"))
            requirement.addList("RefreshCookie");
        operation.setSecurity(requirement.isEmpty() ? List.of() : List.of(requirement));
        String access =
                admin
                        ? "ADMIN."
                        : protectedOperation
                                ? "Требуется Bearer JWT. Пользовательский контент меняет автор или"
                                        + " ADMIN; избранное и реакции личные."
                                : "Публичный endpoint, Bearer JWT не требуется.";
        operation.setDescription(
                (operation.getDescription() == null ? "" : operation.getDescription() + "\n\n")
                        + access
                        + (authWrite
                                ? " Требуется CSRF header и cookie; credentials: include."
                                : ""));
    }

    private void documentResponses(
            String path,
            PathItem.HttpMethod method,
            boolean authWrite,
            boolean protectedOperation,
            Operation operation) {
        boolean read = method == PathItem.HttpMethod.GET || method == PathItem.HttpMethod.HEAD;
        String success = successCode(path, method);
        if (operation.getResponses() == null) operation.setResponses(new ApiResponses());
        if (!success.equals("200")) {
            var previous = operation.getResponses().remove("200");
            if (!operation.getResponses().containsKey(success))
                operation
                        .getResponses()
                        .addApiResponse(
                                success,
                                previous == null
                                        ? new ApiResponse().description("Success")
                                        : previous);
        }
        if (!operation.getResponses().containsKey(success))
            operation
                    .getResponses()
                    .addApiResponse(success, new ApiResponse().description("Success"));
        var response = operation.getResponses().get(success);
        if (success.equals("201"))
            response.addHeaderObject(
                    "Location",
                    new Header().description("URI созданного ресурса").schema(new StringSchema()));
        if (success.equals("204")) response.setContent(null);
        if (path.equals("/api/motorcycles") && read) {
            response.addHeaderObject(
                    "X-Total-Count",
                    new Header()
                            .schema(new IntegerSchema())
                            .description("Всего записей после фильтрации"));
            response.addHeaderObject("X-Total-Pages", new Header().schema(new IntegerSchema()));
        }
        addError(operation, "400", "Некорректный JSON, параметры или validation");
        addError(operation, "404", "Ресурс не найден");
        if (!read) addError(operation, "409", "Конфликт уникальности или связей");
        if (protectedOperation
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/refresh"))
            addError(
                    operation,
                    "401",
                    "Нет аутентификации, неверные credentials или истёкший токен");
        if (protectedOperation || authWrite)
            addError(operation, "403", "Недостаточно прав или CSRF token отсутствует/неверен");
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register"))
            addError(
                    operation,
                    "429",
                    "10 попыток на email и 30 на IP за 15 минут; Retry-After: 900");
        if (method == PathItem.HttpMethod.HEAD)
            operation.getResponses().values().forEach(item -> item.setContent(null));
    }

    private String successCode(String path, PathItem.HttpMethod method) {
        if (method == PathItem.HttpMethod.DELETE
                || (path.endsWith("/reaction") && method == PathItem.HttpMethod.PUT)
                || Set.of("/api/auth/logout", "/api/auth/logout-all", "/api/auth/change-password")
                        .contains(path)) return "204";
        if (method == PathItem.HttpMethod.POST
                && !Set.of("/api/auth/login", "/api/auth/refresh").contains(path)) return "201";
        return "200";
    }

    private void addError(Operation operation, String code, String description) {
        operation
                .getResponses()
                .addApiResponse(
                        code,
                        new ApiResponse()
                                .description(description)
                                .content(
                                        new Content()
                                                .addMediaType(
                                                        "application/json",
                                                        new MediaType()
                                                                .schema(
                                                                        new Schema<>()
                                                                                .$ref(
                                                                                        "#/components/schemas/ErrorResponse")))));
    }

    private Object example(String path) {
        if (path.endsWith("/reaction")) return Map.of("kind", "LIKE");
        if (path.equals("/api/auth/register"))
            return Map.of(
                    "email",
                    "rider@example.org",
                    "displayName",
                    "Rider",
                    "password",
                    "<your-password-12-to-72-bytes>");
        if (path.equals("/api/auth/login"))
            return Map.of(
                    "email",
                    bootstrapAdminEmail.isBlank() ? "admin@motoride.local" : bootstrapAdminEmail,
                    "password",
                    "<your-password>");
        if (path.equals("/api/auth/change-password"))
            return Map.of(
                    "currentPassword",
                    "<current-password>",
                    "newPassword",
                    "<new-password-12-to-72-bytes>");
        if (path.equals("/api/users/me")) return Map.of("displayName", "Rider");
        if (path.startsWith("/api/reviews") || path.endsWith("/reviews"))
            return Map.of("rating", 5, "text", "Удобный городской мотоцикл");
        if (path.startsWith("/api/comments") || path.endsWith("/comments"))
            return Map.of("text", "Спасибо за рассказ!");
        if (path.startsWith("/api/posts"))
            return Map.of(
                    "title",
                    "Первая поездка",
                    "content",
                    "Сегодня проехал первые 100 км",
                    "motorcycleId",
                    1);
        if (path.startsWith("/api/motorcycles"))
            return Map.of(
                    "brand",
                    "Honda",
                    "model",
                    "CB500F",
                    "engineCc",
                    471,
                    "horsepower",
                    47,
                    "description",
                    "Городской мотоцикл",
                    "imageUrl",
                    "/images/cb500f.png",
                    "price",
                    800000,
                    "categoryId",
                    2,
                    "technologyIds",
                    List.of(1));
        if (path.startsWith("/api/categories"))
            return Map.of(
                    "name",
                    "Круизеры",
                    "slug",
                    "cruisers",
                    "description",
                    "Для спокойных путешествий");
        if (path.startsWith("/api/technologies"))
            return Map.of("name", "ABS", "description", "Антиблокировочная система");
        if (path.startsWith("/api/news"))
            return Map.of(
                    "title",
                    "Новая модель",
                    "content",
                    "Текст новости",
                    "publishedAt",
                    "2026-10-29T12:00:00",
                    "imageUrl",
                    "/images/news/new.png");
        return null;
    }
}
