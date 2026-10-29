package com.example.kodyjobdam.recruit.service;

import com.example.kodyjobdam.common.dto.response.PublicationStatus;
import com.example.kodyjobdam.common.exception.RecruitException;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormStatus;
import com.example.kodyjobdam.form.event.FormPublishedEvent;
import com.example.kodyjobdam.form.service.FormService;
import com.example.kodyjobdam.notice.service.DiscordNoticeService;
import com.example.kodyjobdam.recruit.client.GeminiAnalysisResult;
import com.example.kodyjobdam.recruit.client.GeminiClient;
import com.example.kodyjobdam.recruit.client.GeminiImage;
import com.example.kodyjobdam.recruit.dto.request.RecruitCreateDTO;
import com.example.kodyjobdam.recruit.dto.request.RecruitUpdateDTO;
import com.example.kodyjobdam.recruit.dto.response.RecruitImageResponseDTO;
import com.example.kodyjobdam.recruit.dto.response.RecruitResponseDTO;
import com.example.kodyjobdam.recruit.entity.RecruitEntity;
import com.example.kodyjobdam.recruit.entity.RecruitField;
import com.example.kodyjobdam.recruit.entity.RecruitImageEntity;
import com.example.kodyjobdam.recruit.entity.RecruitPeriod;
import com.example.kodyjobdam.recruit.entity.RecruitStatus;
import com.example.kodyjobdam.recruit.repository.RecruitRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.AdditionalAnswers.returnsFirstArg;

@ExtendWith(MockitoExtension.class)
class RecruitServiceTest {

    @Mock
    private RecruitRepository recruitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private RecruitImageStorage recruitImageStorage;

    @Mock
    private FormService formService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private com.example.kodyjobdam.notification.service.NotificationExpirationService notificationExpirationService;

    @Mock
    private DiscordNoticeService discordNoticeService;

    @Mock
    private com.example.kodyjobdam.notice.service.NoticeAnnouncer noticeAnnouncer;

    @InjectMocks
    private RecruitService recruitService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    /**
     * 마감 판단 기준 날짜를 고정한다.
     * 고정하지 않으면 테스트에 적어둔 2026-09 전형 일정이 실제 날짜에 추월당해 깨진다.
     */
    @BeforeEach
    void fixClock() {
        ReflectionTestUtils.setField(recruitService, "clock",
                Clock.fixed(LocalDate.of(2026, 9, 1).atTime(9, 0).atZone(SEOUL).toInstant(), SEOUL));
    }

    @Test
    void analyzeCreatesDefaultApplicationForm() {
        User teacher = user(2L);
        RecruitPeriod documentPeriod = new RecruitPeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10));
        FormEntity form = FormEntity.builder().id(7L).title("잡담 지원서").status(FormStatus.DRAFT).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(geminiClient.analyze(any())).thenReturn(new GeminiAnalysisResult(
                "잡담", documentPeriod, null, null, null, null,
                java.util.Set.of(RecruitField.BACKEND), "요약"));
        when(formService.createForRecruit(
                eq(teacher), eq("잡담"), eq(LocalDate.of(2026, 9, 10).atTime(LocalTime.of(23, 59, 59)))))
                .thenReturn(form);
        when(recruitImageStorage.store(any(byte[].class), eq("png")))
                .thenReturn("/uploads/recruit/2026/09/uuid.png");
        when(recruitRepository.save(any(RecruitEntity.class))).thenAnswer(returnsFirstArg());

        RecruitResponseDTO response = recruitService.analyze(List.of(png("recruit.png")), true, 2L);

        assertThat(response.getFormId()).isEqualTo(7L);
        assertThat(response.getImageUrl()).isEqualTo("/uploads/recruit/2026/09/uuid.png");
        assertThat(response.getImages())
                .extracting(RecruitImageResponseDTO::getUrl)
                .containsExactly("/uploads/recruit/2026/09/uuid.png");
        assertThat(response.getFields()).containsExactly(RecruitField.BACKEND);
    }

    @Test
    void 분석할_때_지원_폼을_끄면_폼_없이_공고만_만든다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(geminiClient.analyze(any())).thenReturn(new GeminiAnalysisResult(
                "잡담", null, null, null, null, null, java.util.Set.of(), "요약"));
        when(recruitImageStorage.store(any(byte[].class), eq("png"))).thenReturn("/uploads/recruit/2026/09/a.png");
        when(recruitRepository.save(any(RecruitEntity.class))).thenAnswer(returnsFirstArg());

        RecruitResponseDTO response = recruitService.analyze(List.of(png("recruit.png")), false, 2L);

        assertThat(response.getFormId()).isNull();
        verify(formService, never()).createForRecruit(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void 여러_장을_올리면_함께_분석하고_올린_순서대로_저장한다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(geminiClient.analyze(any())).thenReturn(new GeminiAnalysisResult(
                "잡담", null, null, null, null, null, java.util.Set.of(), "요약"));
        when(recruitImageStorage.store(any(byte[].class), eq("png"))).thenReturn("/uploads/recruit/2026/09/a.png");
        when(recruitImageStorage.store(any(byte[].class), eq("jpg"))).thenReturn("/uploads/recruit/2026/09/b.jpg");
        when(recruitRepository.save(any(RecruitEntity.class))).thenAnswer(returnsFirstArg());

        RecruitResponseDTO response = recruitService.analyze(List.of(
                png("page1.png"),
                new MockMultipartFile("image", "page2.jpg", "image/jpeg", new byte[]{3})), false, 2L);

        ArgumentCaptor<List<GeminiImage>> analyzed = ArgumentCaptor.forClass(List.class);
        verify(geminiClient).analyze(analyzed.capture());
        assertThat(analyzed.getValue())
                .extracting(GeminiImage::mimeType)
                .containsExactly("image/png", "image/jpeg");
        assertThat(response.getImages())
                .extracting(RecruitImageResponseDTO::getUrl)
                .containsExactly("/uploads/recruit/2026/09/a.png", "/uploads/recruit/2026/09/b.jpg");
        assertThat(response.getImageUrl()).isEqualTo("/uploads/recruit/2026/09/a.png");
    }

    @Test
    void 이미지가_아닌_파일이_섞여_있으면_분석도_저장도_하지_않는다() {
        assertThatThrownBy(() -> recruitService.analyze(List.of(
                png("page1.png"),
                new MockMultipartFile("image", "memo.txt", "text/plain", new byte[]{1})), true, 2L))
                .isInstanceOf(RecruitException.class)
                .hasMessage("이미지 파일만 업로드할 수 있습니다.");
        verify(geminiClient, never()).analyze(any());
        verify(recruitImageStorage, never()).store(any(), any());
    }

    @Test
    void 공고_이미지는_10장까지만_올릴_수_있다() {
        List<MultipartFile> images = java.util.stream.IntStream.rangeClosed(1, 11)
                .mapToObj(i -> (MultipartFile) png("page" + i + ".png"))
                .toList();

        assertThatThrownBy(() -> recruitService.analyze(images, true, 2L))
                .isInstanceOf(RecruitException.class)
                .hasMessage("공고 이미지는 최대 10장까지 올릴 수 있습니다.");
        verify(geminiClient, never()).analyze(any());
    }

    @Test
    void 공고에_이미지를_추가하면_기존_이미지_뒤에_붙는다() {
        RecruitEntity recruit = draftRecruitWithImages("/uploads/recruit/2026/09/a.png");
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));
        when(recruitImageStorage.store(any(byte[].class), eq("png")))
                .thenReturn("/uploads/recruit/2026/09/b.png", "/uploads/recruit/2026/09/c.png");

        RecruitResponseDTO response = recruitService.addImages(
                10L, List.of(png("b.png"), png("c.png")), 2L);

        assertThat(response.getImages())
                .extracting(RecruitImageResponseDTO::getUrl)
                .containsExactly(
                        "/uploads/recruit/2026/09/a.png",
                        "/uploads/recruit/2026/09/b.png",
                        "/uploads/recruit/2026/09/c.png");
        assertThat(recruit.getImages())
                .extracting(RecruitImageEntity::getOrderIndex)
                .containsExactly(1, 2, 3);
        verify(recruitRepository).flush();
    }

    @Test
    void 이미지를_추가해_10장을_넘기면_거부한다() {
        RecruitEntity recruit = draftRecruitWithImages(java.util.stream.IntStream.rangeClosed(1, 9)
                .mapToObj(i -> "/uploads/recruit/2026/09/" + i + ".png")
                .toArray(String[]::new));
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        assertThatThrownBy(() -> recruitService.addImages(10L, List.of(png("a.png"), png("b.png")), 2L))
                .isInstanceOf(RecruitException.class)
                .hasMessage("공고 이미지는 최대 10장까지 올릴 수 있습니다.");
        assertThat(recruit.getImages()).hasSize(9);
        verify(recruitImageStorage, never()).store(any(), any());
    }

    @Test
    void 다른_선생님의_공고에는_이미지를_추가할_수_없다() {
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(draftRecruitWithImages()));

        assertThatThrownBy(() -> recruitService.addImages(10L, List.of(png("a.png")), 3L))
                .isInstanceOf(RecruitException.class)
                .hasMessage("채용 공고를 관리할 권한이 없습니다.");
        verify(recruitImageStorage, never()).store(any(), any());
    }

    @Test
    void 공고_이미지를_삭제하면_목록에서_빠지고_파일도_지운다() {
        RecruitEntity recruit = draftRecruitWithImages(
                "/uploads/recruit/2026/09/a.png", "/uploads/recruit/2026/09/b.png");
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        RecruitResponseDTO response = recruitService.deleteImage(10L, 101L, 2L);

        assertThat(response.getImages())
                .extracting(RecruitImageResponseDTO::getUrl)
                .containsExactly("/uploads/recruit/2026/09/b.png");
        verify(recruitImageStorage).delete("/uploads/recruit/2026/09/a.png");
    }

    @Test
    void 공고에_없는_이미지는_삭제할_수_없다() {
        when(recruitRepository.findById(10L))
                .thenReturn(Optional.of(draftRecruitWithImages("/uploads/recruit/2026/09/a.png")));

        assertThatThrownBy(() -> recruitService.deleteImage(10L, 999L, 2L))
                .isInstanceOf(RecruitException.class)
                .hasMessage("공고 이미지를 찾을 수 없습니다.");
        verify(recruitImageStorage, never()).delete(any());
    }

    @Test
    void createWithoutImageSavesDraftWithApplicationForm() throws Exception {
        User teacher = user(2L);
        FormEntity form = FormEntity.builder().id(7L).title("잡담 지원서").status(FormStatus.DRAFT).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(formService.createForRecruit(
                eq(teacher), eq("잡담"), eq(LocalDate.of(2026, 9, 12).atTime(LocalTime.of(23, 59, 59)))))
                .thenReturn(form);
        when(recruitRepository.save(any(RecruitEntity.class))).thenAnswer(returnsFirstArg());

        RecruitCreateDTO dto = objectMapper.readValue(
                "{\"companyName\":\" 잡담 \",\"deadline\":\"2026-09-12\","
                        + "\"interviewDate\":\"2026-09-21 ~ 2026-09-22\",\"summary\":\"요약\"}",
                RecruitCreateDTO.class);

        RecruitResponseDTO response = recruitService.create(dto, 2L);

        assertThat(response.getCompanyName()).isEqualTo("잡담");
        assertThat(response.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(response.getFormId()).isEqualTo(7L);
        assertThat(response.getImageUrl()).isNull();
        assertThat(response.getImages()).isEmpty();
        assertThat(response.getDeadline()).isEqualTo("2026-09-12");
        assertThat(response.getInterviewDate()).isEqualTo("2026-09-21 ~ 2026-09-22");
        assertThat(response.getSummary()).isEqualTo("요약");
        verify(geminiClient, never()).analyze(any());
        verify(recruitImageStorage, never()).store(any(), any());
    }

    @Test
    void 직접_입력할_때_지원_폼을_끄면_폼_없이_공고만_만든다() throws Exception {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(recruitRepository.save(any(RecruitEntity.class))).thenAnswer(returnsFirstArg());

        RecruitCreateDTO dto = objectMapper.readValue(
                "{\"companyName\":\"잡담\",\"createForm\":false}", RecruitCreateDTO.class);

        RecruitResponseDTO response = recruitService.create(dto, 2L);

        assertThat(response.getFormId()).isNull();
        verify(formService, never()).createForRecruit(any(), any(), any());
    }

    @Test
    void createWithOnlyCompanyNameLeavesScheduleUndecided() throws Exception {
        User teacher = user(2L);
        FormEntity form = FormEntity.builder().id(7L).title("잡담 지원서").status(FormStatus.DRAFT).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(formService.createForRecruit(eq(teacher), eq("잡담"), eq(null))).thenReturn(form);
        when(recruitRepository.save(any(RecruitEntity.class))).thenAnswer(returnsFirstArg());

        RecruitCreateDTO dto = objectMapper.readValue("{\"companyName\":\"잡담\"}", RecruitCreateDTO.class);

        RecruitResponseDTO response = recruitService.create(dto, 2L);

        assertThat(response.getDeadline()).isEqualTo(RecruitPeriod.UNDECIDED);
        assertThat(response.getInterviewDate()).isEqualTo(RecruitPeriod.UNDECIDED);
        assertThat(response.getDocumentPeriod()).isNull();
    }

    @Test
    void publishAlsoPublishesApplicationForm() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .form(FormEntity.builder().id(7L).status(FormStatus.DRAFT).build())
                .status(RecruitStatus.DRAFT)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));
        when(noticeAnnouncer.announce("잡담 공고", "새로운 취업 공고가 등록되었습니다.", "미정", "/recruit/10"))
                .thenReturn("1234567890");

        recruitService.publish(10L, 2L);

        verify(formService).publishForRecruit(7L);
        assertThat(recruit.getDiscordMessageId()).isEqualTo("1234567890");
    }

    @Test
    void publishingRecruitFormAlsoPublishesRecruit() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .form(FormEntity.builder().id(7L).status(FormStatus.PUBLISHED).build())
                .status(RecruitStatus.DRAFT)
                .build();
        when(recruitRepository.findByForm_Id(7L)).thenReturn(Optional.of(recruit));
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        recruitService.publishForPublishedForm(new FormPublishedEvent(7L));

        assertThat(recruit.getStatus()).isEqualTo(RecruitStatus.PUBLISHED);
        verify(notificationService).notifyAllStudents(
                eq(NotificationType.RECRUIT_PUBLISHED), any(), any(), eq(10L), any(), any());
        verify(noticeAnnouncer).announce("잡담 공고", "새로운 취업 공고가 등록되었습니다.", "미정", "/recruit/10");
    }

    @Test
    void publishingFormOfAlreadyPublishedRecruitDoesNothing() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .form(FormEntity.builder().id(7L).status(FormStatus.PUBLISHED).build())
                .status(RecruitStatus.PUBLISHED)
                .build();
        when(recruitRepository.findByForm_Id(7L)).thenReturn(Optional.of(recruit));

        recruitService.publishForPublishedForm(new FormPublishedEvent(7L));

        verify(notificationService, never()).notifyAllStudents(any(), any(), any(), any(), any(), any());
    }

    @Test
    void deleteAlsoRemovesApplicationForm() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .form(FormEntity.builder().id(7L).status(FormStatus.DRAFT).build())
                .status(RecruitStatus.DRAFT)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        recruitService.delete(10L, 2L);

        verify(recruitRepository).delete(recruit);
        verify(formService).deleteForRecruit(7L);
    }

    @Test
    void 공고를_삭제하면_이미지_파일도_모두_지운다() {
        RecruitEntity recruit = draftRecruitWithImages(
                "/uploads/recruit/2026/09/a.png", "/uploads/recruit/2026/09/b.png");
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        recruitService.delete(10L, 2L);

        verify(recruitImageStorage).delete("/uploads/recruit/2026/09/a.png");
        verify(recruitImageStorage).delete("/uploads/recruit/2026/09/b.png");
        verify(formService, never()).deleteForRecruit(any());
    }

    @Test
    void publishCreatesStudentNotifications() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .documentPeriod(new RecruitPeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10)))
                .status(RecruitStatus.DRAFT)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));
        when(notificationExpirationService.recruitExpiresAt("2026-09-10"))
                .thenReturn(LocalDateTime.of(2026, 10, 10, 23, 59, 59));

        recruitService.publish(10L, 2L);

        verify(notificationService).notifyAllStudents(
                eq(NotificationType.RECRUIT_PUBLISHED),
                eq("새로운 취업 공지"),
                eq("잡담 취업 공지가 게시되었습니다."),
                eq(10L),
                eq("/recruit/10"),
                eq(LocalDateTime.of(2026, 10, 10, 23, 59, 59))
        );
    }

    @Test
    void updatePublishedRecruitUpdatesDiscordMessage() throws Exception {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .summary("기존 요약")
                .documentPeriod(new RecruitPeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10)))
                .discordMessageId("1234567890")
                .status(RecruitStatus.PUBLISHED)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        RecruitUpdateDTO dto = objectMapper.readValue("{\"summary\":\"수정된 요약\"}", RecruitUpdateDTO.class);

        recruitService.update(10L, dto, 2L);

        verify(noticeAnnouncer).update("1234567890", "잡담 공고", "수정된 요약", "미정", "/recruit/10");
    }

    @Test
    void publishAlreadyPublishedRecruitDoesNotCreateDuplicateNotification() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .status(RecruitStatus.PUBLISHED)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        assertThatThrownBy(() -> recruitService.publish(10L, 2L))
                .isInstanceOf(RecruitException.class);
        verify(notificationService, never()).notifyAllStudents(any(), any(), any(), any(), any(), any());
    }

    @Test
    void publishOtherTeachersRecruitThrowsForbidden() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .status(RecruitStatus.DRAFT)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        assertThatThrownBy(() -> recruitService.publish(10L, 3L))
                .isInstanceOf(RecruitException.class)
                .hasMessage("채용 공고를 관리할 권한이 없습니다.");
        verify(notificationService, never()).notifyAllStudents(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateWithDeadlineAndInterviewDateKeepsOtherPeriods() throws Exception {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .form(FormEntity.builder().id(7L).status(FormStatus.DRAFT).build())
                .documentPeriod(new RecruitPeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10)))
                .codingTestPeriod(new RecruitPeriod(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 15)))
                .interviewPeriod(new RecruitPeriod(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 20)))
                .summary("요약")
                .status(RecruitStatus.DRAFT)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        RecruitUpdateDTO dto = objectMapper.readValue(
                "{\"companyName\":\"잡담\",\"deadline\":\"2026-09-12\","
                        + "\"interviewDate\":\"2026-09-21 ~ 2026-09-22\",\"summary\":\"요약\"}",
                RecruitUpdateDTO.class);

        RecruitResponseDTO response = recruitService.update(10L, dto, 2L);

        assertThat(response.getDeadline()).isEqualTo("2026-09-12");
        assertThat(response.getInterviewDate()).isEqualTo("2026-09-21 ~ 2026-09-22");
        assertThat(response.getDocumentPeriod().startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.getDocumentPeriod().endDate()).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThat(response.getCodingTestPeriod().startDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        // 공고 종료일과 폼 마감일이 같은 날짜를 가리켜야 한다. LocalTime.MAX는 MySQL에서 다음 날로 반올림된다.
        verify(formService).updateDeadlineForRecruit(
                7L, LocalDate.of(2026, 9, 12).atTime(LocalTime.of(23, 59, 59)));
    }

    @Test
    void updateWithBlankInterviewDateClearsInterviewPeriod() throws Exception {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .interviewPeriod(new RecruitPeriod(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 20)))
                .status(RecruitStatus.DRAFT)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        RecruitUpdateDTO dto = objectMapper.readValue("{\"interviewDate\":\"\"}", RecruitUpdateDTO.class);

        RecruitResponseDTO response = recruitService.update(10L, dto, 2L);

        assertThat(response.getInterviewDate()).isEqualTo(RecruitPeriod.UNDECIDED);
        assertThat(response.getInterviewPeriod()).isNull();
        assertThat(response.getCompanyName()).isEqualTo("잡담");
    }

    @Test
    void 서류_접수_종료일이_지난_공고는_공개할_수_없다() {
        RecruitEntity recruit = publishedRecruit(10L, LocalDate.of(2026, 8, 31));
        ReflectionTestUtils.setField(recruit, "status", RecruitStatus.DRAFT);
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        assertThatThrownBy(() -> recruitService.publish(10L, 2L))
                .isInstanceOf(RecruitException.class)
                .hasMessage("서류 접수 종료일이 이미 지난 공고는 공개할 수 없습니다. 접수 기간을 먼저 수정해주세요.");
        assertThat(recruit.getStatus()).isEqualTo(RecruitStatus.DRAFT);
        verify(notificationService, never()).notifyAllStudents(any(), any(), any(), any(), any(), any());
    }

    @Test
    void 마감된_공고_조회는_마감으로_구분된다() {
        when(recruitRepository.findById(10L))
                .thenReturn(Optional.of(publishedRecruit(10L, LocalDate.of(2026, 8, 31))));

        assertThatThrownBy(() -> recruitService.getPublished(10L))
                .isInstanceOf(RecruitException.class)
                .hasMessage("서류 접수가 마감된 공고입니다.")
                .hasFieldOrPropertyWithValue("code", "RECRUIT_CLOSED")
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void 초안_공고_조회는_비공개로_구분된다() {
        RecruitEntity recruit = publishedRecruit(10L, LocalDate.of(2026, 9, 30));
        ReflectionTestUtils.setField(recruit, "status", RecruitStatus.DRAFT);
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        assertThatThrownBy(() -> recruitService.getPublished(10L))
                .isInstanceOf(RecruitException.class)
                .hasMessage("아직 공개되지 않은 공고입니다.")
                .hasFieldOrPropertyWithValue("code", "RECRUIT_NOT_PUBLISHED")
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void 접수_종료일_당일까지는_공개_상태로_본다() {
        when(recruitRepository.findById(10L))
                .thenReturn(Optional.of(publishedRecruit(10L, LocalDate.of(2026, 9, 1))));

        assertThat(recruitService.getPublished(10L).getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
    }

    @Test
    void 학생_공고_목록에서_마감된_공고는_빠진다() {
        when(recruitRepository.findByStatusOrderByCreatedAtDesc(RecruitStatus.PUBLISHED)).thenReturn(List.of(
                publishedRecruit(11L, LocalDate.of(2026, 9, 30)),
                publishedRecruit(12L, LocalDate.of(2026, 8, 31))));

        assertThat(recruitService.listPublished())
                .extracting(RecruitResponseDTO::getId)
                .containsExactly(11L);
    }

    @Test
    void 선생님_공고_목록에는_마감된_공고도_CLOSED로_보인다() {
        when(recruitRepository.findByUserIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(publishedRecruit(12L, LocalDate.of(2026, 8, 31))));

        assertThat(recruitService.listForTeacher(2L))
                .extracting(RecruitResponseDTO::getStatus)
                .containsExactly(PublicationStatus.CLOSED);
    }

    @Test
    void 직접_입력한_공고도_직무_분야를_저장한다() throws Exception {
        User teacher = user(2L);
        FormEntity form = FormEntity.builder().id(7L).title("잡담 지원서").status(FormStatus.DRAFT).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(formService.createForRecruit(eq(teacher), eq("잡담"), eq(null))).thenReturn(form);
        when(recruitRepository.save(any(RecruitEntity.class))).thenAnswer(returnsFirstArg());

        RecruitCreateDTO dto = objectMapper.readValue(
                "{\"companyName\":\"잡담\",\"fields\":[\"BACKEND\",\"FRONTEND\",\"BACKEND\"]}",
                RecruitCreateDTO.class);

        RecruitResponseDTO response = recruitService.create(dto, 2L);

        // 중복은 걷어내고 enum 선언 순서로 내려간다.
        assertThat(response.getFields()).containsExactly(RecruitField.FRONTEND, RecruitField.BACKEND);
    }

    @Test
    void 직무_분야를_보내지_않은_수정은_기존_분야를_유지한다() throws Exception {
        RecruitEntity recruit = draftRecruitWithFields(RecruitField.IOT);
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        RecruitUpdateDTO dto = objectMapper.readValue("{\"summary\":\"수정된 요약\"}", RecruitUpdateDTO.class);

        RecruitResponseDTO response = recruitService.update(10L, dto, 2L);

        assertThat(response.getFields()).containsExactly(RecruitField.IOT);
    }

    @Test
    void 직무_분야를_빈_배열로_보내면_비운다() throws Exception {
        RecruitEntity recruit = draftRecruitWithFields(RecruitField.IOT);
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        RecruitUpdateDTO dto = objectMapper.readValue("{\"fields\":[]}", RecruitUpdateDTO.class);

        RecruitResponseDTO response = recruitService.update(10L, dto, 2L);

        assertThat(response.getFields()).isEmpty();
    }

    @Test
    void 직무_분야를_다른_값으로_바꿀_수_있다() throws Exception {
        RecruitEntity recruit = draftRecruitWithFields(RecruitField.IOT);
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        RecruitUpdateDTO dto = objectMapper.readValue("{\"fields\":[\"AI\"]}", RecruitUpdateDTO.class);

        RecruitResponseDTO response = recruitService.update(10L, dto, 2L);

        assertThat(response.getFields()).containsExactly(RecruitField.AI);
    }

    private RecruitEntity draftRecruitWithFields(RecruitField... fields) {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .summary("기존 요약")
                .status(RecruitStatus.DRAFT)
                .build();
        recruit.replaceFields(List.of(fields));
        return recruit;
    }

    /** 이미지를 순서대로 붙인 초안 공고. 이미지 식별자는 101부터 차례로 매긴다. */
    private RecruitEntity draftRecruitWithImages(String... imageUrls) {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .status(RecruitStatus.DRAFT)
                .build();
        for (String imageUrl : imageUrls) {
            recruit.addImage(imageUrl);
        }
        for (int i = 0; i < recruit.getImages().size(); i++) {
            ReflectionTestUtils.setField(recruit.getImages().get(i), "id", 101L + i);
        }
        return recruit;
    }

    private MockMultipartFile png(String name) {
        return new MockMultipartFile("image", name, "image/png", new byte[]{1, 2});
    }

    private RecruitEntity publishedRecruit(Long id, LocalDate documentEndDate) {
        return RecruitEntity.builder()
                .id(id)
                .user(user(2L))
                .companyName("잡담")
                .documentPeriod(new RecruitPeriod(LocalDate.of(2026, 8, 1), documentEndDate))
                .status(RecruitStatus.PUBLISHED)
                .build();
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .name("선생님")
                .student_number("T" + id)
                .email("teacher" + id + "@test.com")
                .role(UserRole.TEACHER)
                .emailVerified(true)
                .build();
    }
}
