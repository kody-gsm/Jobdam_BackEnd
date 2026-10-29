package com.example.kodyjobdam.recruit.entity;

import com.example.kodyjobdam.common.dto.response.PublicationStatus;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recruit")
public class RecruitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String companyName;

    /** 서류 접수 기간 */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "document_start_date")),
            @AttributeOverride(name = "endDate", column = @Column(name = "document_end_date"))
    })
    private RecruitPeriod documentPeriod;

    /** 필기 전형 기간 */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "written_exam_start_date")),
            @AttributeOverride(name = "endDate", column = @Column(name = "written_exam_end_date"))
    })
    private RecruitPeriod writtenExamPeriod;

    /** 실기 전형 기간 */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "practical_exam_start_date")),
            @AttributeOverride(name = "endDate", column = @Column(name = "practical_exam_end_date"))
    })
    private RecruitPeriod practicalExamPeriod;

    /** 코딩테스트 전형 기간 */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "coding_test_start_date")),
            @AttributeOverride(name = "endDate", column = @Column(name = "coding_test_end_date"))
    })
    private RecruitPeriod codingTestPeriod;

    /** 면접 전형 기간 */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "interview_start_date")),
            @AttributeOverride(name = "endDate", column = @Column(name = "interview_end_date"))
    })
    private RecruitPeriod interviewPeriod;

    /** 공고와 함께 자동으로 만들어지는 지원 폼 */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id")
    private FormEntity form;

    /** 공개된 공고와 연결된 디스코드 메시지 ID. 공고 수정 시 같은 메시지를 수정하는 데 사용한다. */
    private String discordMessageId;

    /**
     * 이 공고가 뽑는 직무 분야. 한 공고가 여러 직무를 함께 뽑을 수 있어 목록으로 둔다.
     * 분석에서 판단하지 못했으면 비어 있고, 선생님이 검토 단계에서 채운다.
     */
    @Convert(converter = RecruitFieldConverter.class)
    @Column(name = "fields", length = 100)
    @Builder.Default
    private Set<RecruitField> fields = EnumSet.noneOf(RecruitField.class);

    @Column(length = 1000)
    private String summary;

    /** 선생님이 올린 공고 원본 이미지. 올린 순서대로 보여준다. */
    @OneToMany(mappedBy = "recruit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<RecruitImageEntity> images = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RecruitStatus status = RecruitStatus.DRAFT;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** 선생님 검토 후 내용 수정 */
    public void update(String companyName,
                       RecruitPeriod documentPeriod,
                       RecruitPeriod writtenExamPeriod,
                       RecruitPeriod practicalExamPeriod,
                       RecruitPeriod codingTestPeriod,
                       RecruitPeriod interviewPeriod,
                       Collection<RecruitField> fields,
                       String summary) {
        this.companyName = companyName;
        this.documentPeriod = documentPeriod;
        this.writtenExamPeriod = writtenExamPeriod;
        this.practicalExamPeriod = practicalExamPeriod;
        this.codingTestPeriod = codingTestPeriod;
        this.interviewPeriod = interviewPeriod;
        replaceFields(fields);
        this.summary = summary;
    }

    /** 직무 분야를 통째로 갈아끼운다. null을 주면 기존 값을 그대로 둔다. */
    public void replaceFields(Collection<RecruitField> fields) {
        if (fields == null) {
            return;
        }
        this.fields.clear();
        this.fields.addAll(fields);
    }

    /** 직무 분야를 enum 선언 순서로 돌려준다. 응답 순서가 조회마다 달라지지 않게 한다. */
    public List<RecruitField> getSortedFields() {
        return fields.stream().sorted().toList();
    }

    /**
     * 서류 접수 종료일. 접수 기간을 못 읽었으면 null.
     * 공고와 딸린 지원 폼의 마감일은 모두 이 날짜에서 나오는 단일 기준이다.
     */
    public LocalDate getDocumentEndDate() {
        return documentPeriod == null ? null : documentPeriod.getEndDate();
    }

    /**
     * 지원 마감일(서류 접수 종료일)을 yyyy-MM-dd 문자열로 돌려준다.
     * 알림 만료 계산이 문자열 마감일을 받으므로 그 형식에 맞춘 파생값이다.
     */
    public String getDeadline() {
        LocalDate endDate = getDocumentEndDate();
        return endDate == null ? null : endDate.toString();
    }

    /** 서류 접수가 마감됐는지. 종료일 당일까지는 접수 중으로 본다. 종료일이 없으면 마감되지 않은 것으로 본다. */
    public boolean isPastDeadline(LocalDate today) {
        LocalDate endDate = getDocumentEndDate();
        return endDate != null && today.isAfter(endDate);
    }

    /**
     * 학생 화면용 공개 상태. 저장된 상태와 서류 접수 종료일을 함께 본다.
     *
     * <p>RecruitStatus에는 CLOSED가 없고 마감된 공고도 PUBLISHED로 남으므로
     * 마감 판단은 항상 이 메서드를 거친다. 딸린 지원 폼과 같은 날짜를 기준으로 삼는다.</p>
     */
    public PublicationStatus publicationStatus(LocalDate today) {
        if (this.status == RecruitStatus.DRAFT) {
            return PublicationStatus.DRAFT;
        }
        return isPastDeadline(today) ? PublicationStatus.CLOSED : PublicationStatus.PUBLISHED;
    }

    /** 면접 일정을 화면 표기 문자열로 돌려준다. 하루짜리 면접은 날짜 하나로 표기한다. */
    public String getInterviewDate() {
        return interviewPeriod == null ? null : interviewPeriod.toDisplay();
    }

    /** 지원 폼 식별자. 폼이 없으면 null. */
    public Long getFormId() {
        return form == null ? null : form.getId();
    }

    /** 첫 번째 이미지의 공개 URL. 이미지가 없으면 null. */
    public String getImageUrl() {
        return images.isEmpty() ? null : images.get(0).getImageUrl();
    }

    /** 이미지 공개 URL을 올린 순서대로 돌려준다. */
    public List<String> getImageUrls() {
        return images.stream()
                .map(RecruitImageEntity::getImageUrl)
                .toList();
    }

    /** 이미지 추가 (양방향 연관관계 동기화). 지금 붙어 있는 이미지 뒤에 붙인다. */
    public void addImage(String imageUrl) {
        int nextOrderIndex = images.stream()
                .mapToInt(RecruitImageEntity::getOrderIndex)
                .max()
                .orElse(0) + 1;

        RecruitImageEntity image = RecruitImageEntity.builder()
                .orderIndex(nextOrderIndex)
                .imageUrl(imageUrl)
                .build();
        image.assignRecruit(this);
        images.add(image);
    }

    /** 이미지 하나를 뗀다. 뗀 이미지의 URL을 돌려주고, 이 공고의 이미지가 아니면 빈 값을 돌려준다. */
    public Optional<String> removeImage(Long imageId) {
        Optional<RecruitImageEntity> target = images.stream()
                .filter(image -> Objects.equals(image.getId(), imageId))
                .findFirst();
        target.ifPresent(images::remove);
        return target.map(RecruitImageEntity::getImageUrl);
    }

    /** 학생에게 공개 */
    public void publish() {
        this.status = RecruitStatus.PUBLISHED;
    }

    /** 디스코드에 발송된 공고 메시지와 연결한다. */
    public void linkDiscordMessage(String discordMessageId) {
        this.discordMessageId = discordMessageId;
    }
}
