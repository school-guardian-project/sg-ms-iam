package com.school_guardian.ms_iam.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "GATEWAY_E2E_TESTS", matches = "true")
class GatewayAuthenticationIntegrationTest {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final JsonMapper json = JsonMapper.builder().build();
    private final String origin = "https://guardian-escolar.vercel.app";

    @Test
    void loginRefreshProtectedRoutesAndReuseRejectionWorkThroughKong() throws Exception {
        UUID personId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        String email = "e2e-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
        String password = UUID.randomUUID().toString();
        try (Connection db = DriverManager.getConnection(required("E2E_DATABASE_URL"),
            required("MSSQL_USER"), required("MSSQL_SA_PASSWORD"))) {
            try {
                execute(db, """
                    INSERT INTO UserManagement.Person
                    (Id,Name,LastName,IdentificationType,IdentificationNumber,Email,Phone,ResidenceAddress,DateBirth,Status)
                    VALUES (?,'Gateway','SmokeTest','CC',?,?,3000000000,'Test fixture','2000-01-01','Active')
                    """, personId, personId.toString().replace("-", "").substring(0, 20), email);
                execute(db, """
                    INSERT INTO Iam.Profile (Id,PersonId,CampuseId,PasswordHash,RoleId,Status)
                    VALUES (?,?,NULL,?,5,'Active')
                    """, profileId, personId, new BCryptPasswordEncoder().encode(password));

                String loginBody = json.writeValueAsString(Map.of("email", email, "password", password));
                var login = request("POST", "/api/v1/auth/login", loginBody, null, null);
                assertEquals(200, login.statusCode(), "Login must create a persisted session");
                String initialCookie = cookie(login);
                assertTrue(login.headers().firstValue("set-cookie").orElseThrow().contains("HttpOnly"));
                assertTrue(login.headers().firstValue("set-cookie").orElseThrow().contains("Secure"));
                assertTrue(login.headers().firstValue("set-cookie").orElseThrow().contains("SameSite=None"));
                assertTrue(login.headers().firstValue("set-cookie").orElseThrow().contains("Path=/api/v1/auth/refresh"));
                var firstRefresh = request("POST", "/api/v1/auth/refresh", "", initialCookie, null);
                assertEquals(200, firstRefresh.statusCode(), "The initial login refresh token must exist in the database");
                var nextRefresh = request("POST", "/api/v1/auth/refresh", "", cookie(firstRefresh), null);
                assertEquals(200, nextRefresh.statusCode(), "Consecutive refreshes must not collide on token hash");
                assertFalse(cookie(nextRefresh).equals(initialCookie), "Refresh must rotate the cookie");
                String token = json.readTree(nextRefresh.body()).path("accessToken").asText();
                assertFalse(token.isBlank(), "Refresh response must contain an access token");

                assertEquals(200, request("GET", "/api/v1/auth/profile", null, null, token).statusCode());
                var types = request("GET", "/notification/api/v1/notifications/alert-types", null, null, token);
                assertEquals(200, types.statusCode(), "The authenticated catalog must resolve through Kong");
                assertTrue(json.readTree(types.body()).isArray());
                var alerts = request("GET", "/notification/api/v1/notifications/alerts?page=0&size=10", null, null, token);
                assertEquals(200, alerts.statusCode(), "The authenticated alert list must resolve through Kong");
                JsonNode listing = json.readTree(alerts.body());
                assertTrue(listing.path("content").isArray());
                assertTrue(listing.path("content").size() <= 10);
                assertTrue(listing.path("totalElements").isNumber());
                for (String path : new String[]{"/api/v1/email/change/request", "/api/v1/phone/change/request"}) {
                    assertEquals(400, request("POST", path, "{}", null, token).statusCode(),
                        "Empty change request must reach DTO validation without sending a verification code");
                }

                assertEquals(401, request("POST", "/api/v1/auth/refresh", "", initialCookie, null).statusCode(),
                    "A rotated token cannot be reused");
                assertEquals(401, request("POST", "/api/v1/auth/refresh", "", cookie(nextRefresh), null).statusCode(),
                    "Reuse detection must commit revocation of the entire token family");
                var otherLogin = request("POST", "/api/v1/auth/login", loginBody, null, null);
                assertEquals(200, otherLogin.statusCode());
                assertEquals(200, request("POST", "/api/v1/auth/refresh", "", cookie(otherLogin), null).statusCode(),
                    "A separate login must still be able to refresh");
                verifySchoolScope(db, profileId, loginBody);
            } finally {
                db.setAutoCommit(false);
                execute(db, """
                    UPDATE Iam.RefreshToken SET ReplacedByTokenId=NULL
                    WHERE SessionProfileId IN (SELECT Id FROM Iam.SessionProfile WHERE ProfileId=?)
                    """, profileId);
                execute(db, """
                    DELETE FROM Iam.RefreshToken
                    WHERE SessionProfileId IN (SELECT Id FROM Iam.SessionProfile WHERE ProfileId=?)
                    """, profileId);
                execute(db, "DELETE FROM Iam.SessionProfile WHERE ProfileId=?", profileId);
                execute(db, "DELETE FROM School.SchoolAdmin WHERE ProfileId=?", profileId);
                execute(db, "DELETE FROM Iam.Profile WHERE Id=?", profileId);
                execute(db, "DELETE FROM UserManagement.Person WHERE Id=?", personId);
                db.commit();
            }
        }
    }

    private void verifySchoolScope(Connection db, UUID profileId, String loginBody) throws Exception {
        String schoolId;
        String otherSchoolId;
        long expectedTotal;
        try (var statement = db.prepareStatement("""
            SELECT c.SchoolId, COUNT_BIG(*) AS Total
            FROM Notification.Alert a
            JOIN Iam.Profile p ON p.Id=a.ProfileId
            JOIN School.SchoolCampus c ON c.Id=p.CampuseId
            GROUP BY c.SchoolId ORDER BY COUNT_BIG(*) DESC
            """); var rows = statement.executeQuery()) {
            assertTrue(rows.next(), "School-scope test needs existing school alerts");
            schoolId = rows.getString(1);
            expectedTotal = rows.getLong(2);
            assertTrue(rows.next(), "Cross-school isolation test needs alerts in a second school");
            otherSchoolId = rows.getString(1);
        }
        execute(db, "UPDATE Iam.Profile SET RoleId=1 WHERE Id=?", profileId);
        execute(db, """
            INSERT INTO School.SchoolAdmin (Id,SchoolId,ProfileId,Status) VALUES (?,?,?,'Active')
            """, UUID.randomUUID(), schoolId, profileId);
        var login = request("POST", "/api/v1/auth/login", loginBody, null, null);
        assertEquals(200, login.statusCode());
        String token = json.readTree(login.body()).path("accessToken").asText();
        var alerts = request("GET", "/notification/api/v1/notifications/alerts?page=0&size=10", null, null, token);
        assertEquals(200, alerts.statusCode(), "School admin must be able to list alerts");
        var listing = json.readTree(alerts.body());
        assertEquals(expectedTotal, listing.path("totalElements").asLong(),
            "The admin must see only their assigned school's alerts, scoped before pagination");
        var foreignSchool = request("GET",
            "/notification/api/v1/notifications/alerts?schoolId=" + otherSchoolId, null, null, token);
        assertEquals(200, foreignSchool.statusCode());
        assertEquals(0, json.readTree(foreignSchool.body()).path("totalElements").asLong(),
            "A query parameter must not widen the admin's school scope");
        var filtered = request("GET", "/notification/api/v1/notifications/alerts?alertTypeId=1", null, null, token);
        assertEquals(200, filtered.statusCode());
        for (var alert : json.readTree(filtered.body()).path("content")) {
            assertEquals(1, alert.path("alertTypeId").asInt());
        }
        var search = request("GET",
            "/notification/api/v1/notifications/alerts?search=" + UUID.randomUUID(), null, null, token);
        assertEquals(200, search.statusCode());
        assertEquals(0, json.readTree(search.body()).path("totalElements").asLong(),
            "Searching for absent text must return an empty result");
    }

    private HttpResponse<String> request(String method, String path, String body, String cookie, String token)
        throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(required("E2E_GATEWAY_URL") + path))
            .timeout(Duration.ofSeconds(30)).header("Origin", origin);
        if (cookie != null) builder.header("Cookie", cookie);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (body != null) builder.header("Content-Type", "application/json");
        var response = http.send(builder.method(method, body == null
            ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(origin, response.headers().firstValue("access-control-allow-origin").orElse(null));
        assertEquals("true", response.headers().firstValue("access-control-allow-credentials").orElse(null));
        return response;
    }

    private String cookie(HttpResponse<?> response) {
        return response.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
    }

    private void execute(Connection db, String sql, Object... args) throws Exception {
        try (var statement = db.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) statement.setString(i + 1, args[i].toString());
            statement.executeUpdate();
        }
    }

    private String required(String name) {
        String value = System.getenv(name);
        assertNotNull(value, name + " must be explicitly configured");
        assertFalse(value.isBlank(), name + " must not be blank");
        return value;
    }
}
