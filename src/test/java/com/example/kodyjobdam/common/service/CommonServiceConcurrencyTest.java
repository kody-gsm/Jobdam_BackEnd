package com.example.kodyjobdam.common.service;

import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.StateEnum;
import com.example.kodyjobdam.common.exception.ReservationException;
import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.service.NotificationExpirationService;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.notification.service.ReservationRealtimeService;
import com.example.kodyjobdam.schedule.service.ScheduleService;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 같은 상담 신청을 두 트랜잭션이 동시에 처리하는 상황을 실제 트랜잭션과 DB 잠금으로 재현한다.
 * 먼저 잠근 쪽이 커밋할 때까지 다른 쪽은 잠금에서 기다려야 하고, 기다린 쪽은 커밋된 결과를 보고 판단해야 한다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:common-concurrency-test;MODE=MySQL;NON_KEYWORDS=USER;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CommonService.class)
// 두 스레드의 트랜잭션이 각자 커밋돼야 하므로 테스트 메서드를 트랜잭션으로 감싸지 않는다
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CommonServiceConcurrencyTest {

    private static final LocalDate DATE = LocalDate.of(2026, 12, 10);
    private static final String PERIOD = "3교시";
    /** 3교시(10:40)가 막 시작된 시각. 만료 스케줄러가 이 시각에 돌면 3교시 대기 신청을 취소한다. */
    private static final LocalDateTime PERIOD_STARTED = DATE.atTime(10, 41);

    @Autowired
    private CommonService commonService;

    @Autowired
    private CommonRepository commonRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationExpirationService notificationExpirationService;

    @MockBean
    private ReservationRealtimeService reservationRealtimeService;

    @MockBean
    private CounselingReservationCryptoService cryptoService;

    @MockBean
    private ScheduleService scheduleService;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private User teacher;

    @BeforeEach
    void setUp() {
        teacher = saveUser("teacher@test.com", UserRole.WEE_TEACHER);

        // 학생 ID를 그대로 암호문 칸에 넣어 두었으므로 복호화는 받은 값을 돌려준다
        when(cryptoService.decrypt(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        // 만료 알림은 보관 기한이 지금보다 뒤일 때만 나간다
        when(notificationExpirationService.now()).thenReturn(PERIOD_STARTED);
        when(notificationExpirationService.counselingExpiresAt(any())).thenReturn(PERIOD_STARTED.plusDays(90));
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        commonRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void concurrentAllowOnSameSlotConfirmsOnlyOne() throws Exception {
        CommonEntity first = saveWaiting(saveUser("first@test.com", UserRole.STUDENT));
        CommonEntity second = saveWaiting(saveUser("second@test.com", UserRole.STUDENT));
        Pause firstApproval = pauseAtFirst(NotificationType.COUNSELING_APPROVED);

        Future<?> firstAllow = executor.submit(() -> commonService.allow(first.getReservation_id(), teacher.getId()));
        firstApproval.awaitReached();
        Future<?> secondAllow = executor.submit(() -> commonService.allow(second.getReservation_id(), teacher.getId()));

        assertWaitingForLock(secondAllow);
        firstApproval.resume();
        firstAllow.get(5, TimeUnit.SECONDS);

        // 두 번째는 첫 번째가 커밋한 결과를 읽는다. 자기 신청이 자동 취소돼 있으므로 거절된다.
        assertRejectedAsCanceled(secondAllow);
        assertThat(stateOf(first)).isEqualTo(StateEnum.RESERVED);
        assertThat(stateOf(second)).isEqualTo(StateEnum.CANCEL);
        verify(notificationService, times(1)).notifyUser(any(), eq(NotificationType.COUNSELING_APPROVED),
                any(), any(), any(), any(), any());
    }

    @Test
    void expireWaitsForAllowAndSkipsAcceptedRequest() throws Exception {
        CommonEntity reservation = saveWaiting(saveUser("student@test.com", UserRole.STUDENT));
        Pause approval = pauseAtFirst(NotificationType.COUNSELING_APPROVED);

        Future<?> allow = executor.submit(() -> commonService.allow(reservation.getReservation_id(), teacher.getId()));
        approval.awaitReached();
        Future<Integer> expire = executor.submit(() -> commonService.expireWaitingReservations(PERIOD_STARTED));

        assertWaitingForLock(expire);
        approval.resume();
        allow.get(5, TimeUnit.SECONDS);

        // 스케줄러는 잠금을 얻은 뒤 다시 읽을 때 이미 수락된 신청을 대상에서 뺀다
        assertThat(expire.get(5, TimeUnit.SECONDS)).isZero();
        assertThat(stateOf(reservation)).isEqualTo(StateEnum.RESERVED);
        verify(notificationService, never()).notifyUser(any(), eq(NotificationType.COUNSELING_EXPIRED),
                any(), any(), any(), any(), any());
    }

    @Test
    void allowWaitsForExpireAndRejectsCanceledRequest() throws Exception {
        CommonEntity reservation = saveWaiting(saveUser("student@test.com", UserRole.STUDENT));
        Pause expiry = pauseAtFirst(NotificationType.COUNSELING_EXPIRED);

        Future<Integer> expire = executor.submit(() -> commonService.expireWaitingReservations(PERIOD_STARTED));
        expiry.awaitReached();
        Future<?> allow = executor.submit(() -> commonService.allow(reservation.getReservation_id(), teacher.getId()));

        assertWaitingForLock(allow);
        expiry.resume();
        assertThat(expire.get(5, TimeUnit.SECONDS)).isEqualTo(1);

        // 수락은 스케줄러가 커밋한 취소를 읽고 거절된다
        assertRejectedAsCanceled(allow);
        assertThat(stateOf(reservation)).isEqualTo(StateEnum.CANCEL);
        verify(notificationService, never()).notifyUser(any(), eq(NotificationType.COUNSELING_APPROVED),
                any(), any(), any(), any(), any());
    }

    /**
     * 처음 나가는 {@code type} 알림에서 그 트랜잭션을 멈춘다.
     * 알림은 잠금을 잡고 상태를 바꾼 뒤, 커밋하기 전에 나가므로 멈춘 동안 잠금은 계속 잡혀 있다.
     */
    private Pause pauseAtFirst(NotificationType type) {
        Pause pause = new Pause(new CountDownLatch(1), new CountDownLatch(1));
        AtomicBoolean first = new AtomicBoolean(true);
        when(notificationService.notifyUser(any(), eq(type), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    if (first.compareAndSet(true, false)) {
                        pause.reached().countDown();
                        pause.resumed().await(10, TimeUnit.SECONDS);
                    }
                    return null;
                });
        return pause;
    }

    /** 상대 트랜잭션이 커밋하지 않았으므로 잠금에서 기다리고 있어야 한다. */
    private void assertWaitingForLock(Future<?> future) {
        assertThatThrownBy(() -> future.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);
    }

    private void assertRejectedAsCanceled(Future<?> allow) {
        assertThatThrownBy(() -> allow.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .cause()
                .isInstanceOfSatisfying(ReservationException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private StateEnum stateOf(CommonEntity reservation) {
        return commonRepository.findById(reservation.getReservation_id()).orElseThrow().getState();
    }

    private CommonEntity saveWaiting(User student) {
        return commonRepository.save(CommonEntity.builder()
                .teacher(teacher)
                .date(DATE)
                .period(PERIOD)
                .submitterHash("hash-" + student.getId())
                .encryptedUserId(String.valueOf(student.getId()))
                .state(StateEnum.WAITING)
                .build());
    }

    private User saveUser(String email, UserRole role) {
        return userRepository.save(User.builder()
                .name(email)
                .student_number(email)
                .email(email)
                .role(role)
                .emailVerified(true)
                .build());
    }

    private record Pause(CountDownLatch reached, CountDownLatch resumed) {

        void awaitReached() throws InterruptedException {
            assertThat(reached.await(5, TimeUnit.SECONDS)).isTrue();
        }

        void resume() {
            resumed.countDown();
        }
    }
}
