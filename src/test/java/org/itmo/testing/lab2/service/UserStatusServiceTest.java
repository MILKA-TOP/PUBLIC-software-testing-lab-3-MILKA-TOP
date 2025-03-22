package org.itmo.testing.lab2.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserStatusServiceTest {

    private UserAnalyticsService userAnalyticsService;
    private UserStatusService userStatusService;

    @BeforeAll
    void setUp() {
        userAnalyticsService = mock(UserAnalyticsService.class);
        userStatusService = new UserStatusService(userAnalyticsService);
    }

    @BeforeEach
    void resetMock() {
        reset(userAnalyticsService);
    }

    // GetUserStatus
    // -1..0..30..59
    // 60
    // 61..90..119
    // 120..121..150...INT_MAX_VALUE
    // Throw No session handle
    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 30, 59})
    @DisplayName("getUserStatus: Отсутствует активность (-1, 0, 30, 59, Inactive)")
    public void testGetUserStatus_Inactive(long minutes) {
        testGetUserStatus(minutes, "Inactive");
    }

    @ParameterizedTest
    @ValueSource(longs = {60, 61, 90, 119})
    @DisplayName("getUserStatus: Отсутствует активность (60, 61, 90, 119, Active)")
    public void testGetUserStatus_Active(long minutes) {
        testGetUserStatus(minutes, "Active");
    }

    @ParameterizedTest
    @ValueSource(longs = {120, 150, Long.MAX_VALUE})
    @DisplayName("getUserStatus: Отсутствует активность (120, 150, LONG.MAX_VALUE, Highly active)")
    public void testGetUserStatus_HighlyActive(long minutes) {
        testGetUserStatus(minutes, "Highly active");
    }


    @Test
    @DisplayName("getUserStatus: Исключение в userAnalyticsService)")
    public void testGetUserStatus_ThrowException() {
        var userId = "user123";
        var exception = new IllegalArgumentException("Any exception");
        when(userAnalyticsService.getTotalActivityTime(userId)).thenThrow(exception);
        assertThrows(exception.getClass(), () -> userStatusService.getUserStatus(userId));
        verify(userAnalyticsService).getTotalActivityTime(userId);
    }

    private void testGetUserStatus(Long minutes, String resultStatus) {
        var userId = "user123";
        when(userAnalyticsService.getTotalActivityTime(userId)).thenReturn(minutes);
        String status = userStatusService.getUserStatus(userId);

        assertEquals(resultStatus, status);
        verify(userAnalyticsService).getTotalActivityTime(userId);
    }

    @Test
    @DisplayName("getUserLastSessionDate: Получить отсутствие сессии")
    public void testGetUserLastSessionDate_EmptyList() {
        var userId = "userId";
        when(userAnalyticsService.getUserSessions(userId)).thenReturn(List.of());

        var optionalResult = userStatusService.getUserLastSessionDate(userId);
        assertTrue(optionalResult.isEmpty());
    }

    @Test
    @DisplayName("getUserLastSessionDate: Получить сессию (Успех, 1 шт.)")
    public void testGetUserLastSessionDate_SingleList() {
        var userId = "userId";
        var logoutTime = LocalDateTime.now();
        var session = new UserAnalyticsService.Session(LocalDateTime.now().minusDays(1), logoutTime);
        when(userAnalyticsService.getUserSessions(userId)).thenReturn(List.of(session));

        var optionalResult = userStatusService.getUserLastSessionDate(userId);
        assertAll("Проверка сессии (единственной, не пустой)",
                () -> assertTrue(optionalResult.isPresent()),
                () -> assertEquals(optionalResult.get(), logoutTime.toLocalDate().toString())
        );
    }

    @Test
    @DisplayName("getUserLastSessionDate: Получить сессию (Успех, 2 шт., корректный порядок)")
    public void testGetUserLastSessionDate_TwoListCorrectOrder() {
        var userId = "userId";
        var firstTime = LocalDateTime.now().minusDays(5);
        var secondTime = LocalDateTime.now();
        var session1 = new UserAnalyticsService.Session(firstTime.minusHours(2), firstTime);
        var session2 = new UserAnalyticsService.Session(secondTime.minusHours(3), secondTime);
        when(userAnalyticsService.getUserSessions(userId)).thenReturn(List.of(session1, session2));

        var optionalResult = userStatusService.getUserLastSessionDate(userId);
        assertAll("Проверка сессии (последней, корректный  порядок)",
                () -> assertTrue(optionalResult.isPresent()),
                () -> assertEquals(optionalResult.get(), secondTime.toLocalDate().toString())
        );
    }

    @Test
    @DisplayName("getUserLastSessionDate: Получить сессию (Успех, 2 шт., некорректный порядок)")
    public void testGetUserLastSessionDate_TwoListIncorrectOrder() {
        var userId = "userId";
        var firstTime = LocalDateTime.now().minusDays(5);
        var secondTime = LocalDateTime.now();
        var session1 = new UserAnalyticsService.Session(firstTime.minusHours(2), firstTime);
        var session2 = new UserAnalyticsService.Session(secondTime.minusHours(3), secondTime);
        when(userAnalyticsService.getUserSessions(userId)).thenReturn(List.of(session2, session1));

        var optionalResult = userStatusService.getUserLastSessionDate(userId);
        assertAll("Проверка сессии (последней, корректный  порядок)",
                () -> assertTrue(optionalResult.isPresent()),
                () -> assertEquals(optionalResult.get(), secondTime.toLocalDate().toString())
        );
    }

    @Test
    @DisplayName("getUserLastSessionDate: Получить сессию (Пустой результат, 1 шт., отсутствует время)")
    public void testGetUserLastSessionDate_SessionWithoutTime() {
        var userId = "userId";
        var session = new UserAnalyticsService.Session(LocalDateTime.now(), null);
        when(userAnalyticsService.getUserSessions(userId)).thenReturn(List.of(session));

        var optionalResult = userStatusService.getUserLastSessionDate(userId);
        assertAll("Проверка сессии (единственной, пустой)",
                () -> assertTrue(optionalResult.isEmpty())
        );
    }

}
