package org.itmo.testing.lab2.integration;

import io.javalin.Javalin;
import io.restassured.RestAssured;
import org.itmo.testing.lab2.controller.UserAnalyticsController;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mockStatic;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserAnalyticsIntegrationTest {

    private Javalin app;
    private int port = 7001;

    @BeforeAll
    void setUp() {
        app = UserAnalyticsController.createApp();
        app.start(port);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @AfterAll
    void tearDown() {
        app.stop();
    }

    // Register:
    // - single success
    // - no userId param
    // - no userName param
    // - two success
    // - two same error
    @Test
    @Order(1)
    @DisplayName("register: Тест регистрации пользователя (положительный тест)")
    void testUserRegistrationSingleSuccess() {
        given()
                .queryParam("userId", "user1")
                .queryParam("userName", "Alice")
                .when()
                .post("/register")
                .then()
                .statusCode(200)
                .body(equalTo("User registered: true"));
    }

    @Test
    @Order(2)
    @DisplayName("register: Тест регистрации пользователя (отсутствует параметр userId)")
    void testUserRegistrationNoUserId() {
        given()
                .queryParam("userName", "Alice")
                .when()
                .post("/register")
                .then()
                .statusCode(400)
                .body(equalTo("Missing parameters"));
    }

    @Test
    @Order(3)
    @DisplayName("register: Тест регистрации пользователя (отсутствует параметр userName)")
    void testUserRegistrationNoUserName() {
        given()
                .queryParam("userId", "user1")
                .when()
                .post("/register")
                .then()
                .statusCode(400)
                .body(equalTo("Missing parameters"));
    }

    @Test
    @Order(4)
    @DisplayName("register: Тест регистрации пользователя (Дублирование userId)")
    void testUserRegistrationDuplicateUserId() {
        given()
                .queryParam("userId", "user1")
                .queryParam("userName", "Alice")
                .when()
                .post("/register")
                .then()
                .statusCode(500);
    }

    @Test
    @Order(5)
    @DisplayName("register: Тест регистрации пользователя (Дублирование userId)")
    void testUserRegistrationNewUserId() {
        given()
                .queryParam("userId", "user2")
                .queryParam("userName", "Bob")
                .when()
                .post("/register")
                .then()
                .statusCode(200);
    }

    // RecordSession:
    // - single success (loginTime < logoutTime)
    // - no userId param
    // - no loginTime param
    // - no logoutTime param
    // - no user wth userId
    // - incorrect format loginTime
    // - incorrect format logoutTime
    // - loginTime > logoutTime
    // - loginTime == logoutTime
    @Test
    @Order(6)
    @DisplayName("recordSession: Тест записи сессии (успех)")
    void testRecordSessionSingleSuccess() {
        LocalDateTime now = LocalDateTime.now();
        given()
                .queryParam("userId", "user1")
                .queryParam("loginTime", now.minusHours(1).toString())
                .queryParam("logoutTime", now.toString())
                .when()
                .post("/recordSession")
                .then()
                .statusCode(200)
                .body(equalTo("Session recorded"));
    }

    @Test
    @Order(7)
    @DisplayName("recordSession: Тест записи сессии (отсутствует параметр userId)")
    void testRecordSessionNoUserIdParam() {
        LocalDateTime now = LocalDateTime.now();
        given()
                .queryParam("loginTime", now.minusHours(1).toString())
                .queryParam("logoutTime", now.toString())
                .when()
                .post("/recordSession")
                .then()
                .statusCode(400)
                .body(equalTo("Missing parameters"));
    }

    @Test
    @Order(8)
    @DisplayName("recordSession: Тест записи сессии (отсутствует параметр loginTime)")
    void testRecordSessionNoLoginTimeParam() {
        LocalDateTime now = LocalDateTime.now();
        given()
                .queryParam("userId", "user1")
                .queryParam("logoutTime", now.toString())
                .when()
                .post("/recordSession")
                .then()
                .statusCode(400)
                .body(equalTo("Missing parameters"));
    }

    @Test
    @Order(9)
    @DisplayName("recordSession: Тест записи сессии (отсутствует параметр logoutTime)")
    void testRecordSessionNoLogoutTimeParam() {
        LocalDateTime now = LocalDateTime.now();
        given()
                .queryParam("userId", "user1")
                .queryParam("loginTime", now.minusHours(1).toString())
                .when()
                .post("/recordSession")
                .then()
                .statusCode(400)
                .body(equalTo("Missing parameters"));
    }

    @Test
    @Order(10)
    @DisplayName("recordSession: Тест записи сессии (отсутствует пользователь с указанным userId)")
    void testRecordSessionNoUser() {
        LocalDateTime now = LocalDateTime.now();
        given()
                .queryParam("userId", "userWithUnknownId")
                .queryParam("loginTime", now.minusHours(1).toString())
                .queryParam("logoutTime", now.toString())
                .when()
                .post("/recordSession")
                .then()
                .statusCode(400)
                .body(equalTo("Invalid data: User not found"));
    }

    @Test
    @Order(11)
    @DisplayName("recordSession: Тест записи сессии (Некорректный формат loginTime)")
    void testRecordSessionIncorrectLoginTimeFormat() {
        LocalDateTime now = LocalDateTime.now();
        given()
                .queryParam("userId", "user1")
                .queryParam("loginTime", "Ну... Это, вчера короче")
                .queryParam("logoutTime", now.toString())
                .when()
                .post("/recordSession")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(12)
    @DisplayName("recordSession: Тест записи сессии (Некорректный формат logoutTime)")
    void testRecordSessionIncorrectLogoutTimeFormat() {
        LocalDateTime now = LocalDateTime.now();
        given()
                .queryParam("userId", "user1")
                .queryParam("loginTime", now.minusHours(1).toString())
                .queryParam("logoutTime", "Завтра")
                .when()
                .post("/recordSession")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(13)
    @DisplayName("recordSession: Тест записи сессии (loginTime > logoutTime)")
    void testRecordSessionLoginTimeMoreThanLogoutTime() {
        LocalDateTime now = LocalDateTime.now();
        given()
                .queryParam("userId", "user2")
                .queryParam("loginTime", now.toString())
                .queryParam("logoutTime", now.minusHours(1).toString())
                .when()
                .post("/recordSession")
                .then()
                .statusCode(500)
                .body(equalTo("Incorrect time ranges"));
    }

    @Test
    @Order(14)
    @DisplayName("recordSession: Тест записи сессии (loginTime == logoutTime)")
    void testRecordSessionLoginTimeSameLogoutTime() {
        LocalDateTime now = LocalDateTime.now();
        given()
                .queryParam("userId", "user2")
                .queryParam("loginTime", now.toString())
                .queryParam("logoutTime", now.toString())
                .when()
                .post("/recordSession")
                .then()
                .statusCode(500)
                .body(equalTo("Incorrect time ranges"));
    }


    // TotalActivity:
    // - single success
    // - no userId param
    // - no user with userId
    @Test
    @Order(15)
    @DisplayName("Тест получения общего времени активности (Успех)")
    void testGetTotalActivitySuccess() {
        given()
                .queryParam("userId", "user1")
                .when()
                .get("/totalActivity")
                .then()
                .statusCode(200)
                .body(containsString("Total activity:"))
                .body(containsString("minutes"));
    }

    @Test
    @Order(15)
    @DisplayName("Тест получения общего времени активности (Отсутствует параметр userId)")
    void testGetTotalActivityNoUserIdParam() {
        given()
                .when()
                .get("/totalActivity")
                .then()
                .statusCode(400)
                .body(equalTo("Missing userId"));
    }

    @Test
    @Order(16)
    @DisplayName("Тест получения общего времени активности (Отсутствует пользователь)")
    void testGetTotalActivityNoUserWithUserId() {
        given()
                .queryParam("userId", "userIncorrectUserId")
                .when()
                .get("/totalActivity")
                .then()
                .statusCode(400)
                .body(equalTo("Invalid data: User not found"));
    }

    // InactiveUsers:
    // - single success (not empty)
    // - no days param
    // - incorrect days param (double)
    // - incorrect days param (text)
    // - days negate
    // - days 0
    // - days 1
    // - days n

    @Test
    @Order(17)
    @DisplayName("inactiveUsers: Тест получения неактивных пользователей (Успех, 0 дней, [])")
    void testGetInactiveUsersZeroDaysSuccess() {
        given()
                .queryParam("days", "0")
                .when()
                .get("/inactiveUsers")
                .then()
                .statusCode(200)
                .body(containsString("user1"));
    }

    @Test
    @Order(18)
    @DisplayName("inactiveUsers: Тест получения неактивных пользователей (Success, 1 день, [])")
    void testGetInactiveUsersSuccessEmptyOneDay() {
        given()
                .queryParam("days", "1")
                .when()
                .get("/inactiveUsers")
                .then()
                .statusCode(200)
                .body(containsString("[]"));
    }

    @Test
    @Order(20)
    @DisplayName("inactiveUsers: Тест получения неактивных пользователей (Success, 10 дней, [...])")
    void testGetInactiveUsersSuccessNonEmptyFiveDay() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(10);
        try (MockedStatic<LocalDateTime> mockedTime = mockStatic(LocalDateTime.class)) {
            mockedTime.when(LocalDateTime::now).thenReturn(futureTime);

            given()
                    .queryParam("days", "9")
                    .when()
                    .get("/inactiveUsers")
                    .then()
                    .statusCode(200)
                    .body(containsString("user1"));
        }
    }

    @Test
    @Order(19)
    @DisplayName("inactiveUsers: Тест получения неактивных пользователей (Success, 5 дней, [])")
    void testGetInactiveUsersSuccessEmptyTenDay() {
        LocalDateTime now = LocalDateTime.now().minusDays(10);
        recordSession("user2", now.minusHours(2), now);
        given()
                .queryParam("days", "5")
                .when()
                .get("/inactiveUsers")
                .then()
                .statusCode(200)
                .body(containsString("user2"));
    }

    private void recordSession(String userId, LocalDateTime login, LocalDateTime logout) {
        given()
                .queryParam("userId", userId)
                .queryParam("loginTime", login.toString())
                .queryParam("logoutTime", logout.toString())
                .when()
                .post("/recordSession")
                .then()
                .statusCode(200)
                .body(equalTo("Session recorded"));

    }


    @Test
    @Order(20)
    @DisplayName("inactiveUsers: Тест получения неактивных пользователей (Отсутствует параметр days)")
    void testGetInactiveUsersNoDaysParameter() {
        given()
                .when()
                .get("/inactiveUsers")
                .then()
                .statusCode(400)
                .body(containsString("Missing days parameter"));
    }

    @Test
    @Order(21)
    @DisplayName("inactiveUsers: Тест получения неактивных пользователей (Некорректный параметр days)")
    void testGetInactiveUsersIncorrectDatParameter() {
        given()
                .queryParam("days", "Неделя")
                .when()
                .get("/inactiveUsers")
                .then()
                .statusCode(400)
                .body(containsString("Invalid number format for days"));
    }

    @Test
    @Order(22)
    @DisplayName("inactiveUsers: Тест получения неактивных пользователей (Отрицательынй параметр days)")
    void testGetInactiveUsersNegateDatParameter() {
        given()
                .queryParam("days", "-1")
                .when()
                .get("/inactiveUsers")
                .then()
                .statusCode(400)
                .body(containsString("Invalid number format for days"));
    }


    // MonthlyActivity:
    // - success (zero result)
    // - success (non-zero result)
    // - no userId param
    // - no month param
    // - no user with userId
    // - incorrect month param

    @Test
    @Order(23)
    @DisplayName("monthlyActivity: Тест получения статистики активности (Успех, не было активности)")
    void testGetMonthlyActivitySuccessEmpty() {
        var month = YearMonth.now().minusMonths(1);
        given()
                .queryParam("userId", "user1")
                .queryParam("month", month.toString())
                .when()
                .get("/monthlyActivity")
                .then()
                .statusCode(200)
                .body(containsString("{}"));
    }

    @Test
    @Order(23)
    @DisplayName("monthlyActivity: Тест получения статистики активности (Успех, не было активности)")
    void testGetMonthlyActivitySuccessNotEmpty() {
        var month = YearMonth.now();
        given()
                .queryParam("userId", "user1")
                .queryParam("month", month.toString())
                .when()
                .get("/monthlyActivity")
                .then()
                .statusCode(200)
                .body(containsString(String.valueOf(Duration.ofHours(1).toMinutes())));
    }

    @Test
    @Order(24)
    @DisplayName("monthlyActivity: Тест получения статистики активности (Отсутствует параметр userId)")
    void testGetMonthlyActivityNoUserIdParameter() {
        var month = YearMonth.now();
        given()
                .queryParam("month", month.toString())
                .when()
                .get("/monthlyActivity")
                .then()
                .statusCode(400)
                .body(containsString("Missing parameters"));
    }

    @Test
    @Order(25)
    @DisplayName("monthlyActivity: Тест получения статистики активности (Отсутствует параметр month)")
    void testGetMonthlyActivityNoMonthParameter() {
        var month = YearMonth.now();
        given()
                .queryParam("userId", "user1")
                .when()
                .get("/monthlyActivity")
                .then()
                .statusCode(400)
                .body(containsString("Missing parameters"));
    }


    @Test
    @Order(26)
    @DisplayName("monthlyActivity: Тест получения статистики активности (Отсутствует пользователь с указанными идентификатором)")
    void testGetMonthlyActivityNoUserWithSelectedId() {
        var month = YearMonth.now();
        given()
                .queryParam("userId", "userUnknownId")
                .queryParam("month", month.toString())
                .when()
                .get("/monthlyActivity")
                .then()
                .statusCode(400)
                .body(containsString("Invalid data: User not found"));
    }

    @Test
    @Order(27)
    @DisplayName("monthlyActivity: Тест получения статистики активности (Некорректный параметр month)")
    void testGetMonthlyActivityIncorrectMonthParameter() {
        var month = "Прошлый месяц";
        given()
                .queryParam("userId", "user1")
                .queryParam("month", month)
                .when()
                .get("/monthlyActivity")
                .then()
                .statusCode(400)
                .body(containsString("Invalid data: Text '" + month + "'"));
    }
}
