package com.example.kodyjobdam.form.entity;

import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 폼 답변에 첨부된 파일.
 * 제출 전에 먼저 올려두고, 응답을 제출할 때 답변이 이 파일을 가리킨다.
 */
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "form_file")
public class FormFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어느 폼에 올린 파일인지 (다른 폼 답변에 돌려쓰지 못하게 한다) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id")
    private FormEntity form;

    /** 파일을 올린 학생 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** 학생이 올린 원래 파일 이름 (내려받을 때 쓴다) */
    private String originalName;

    /** 저장소에 실제로 저장된 상대 경로. 이름이 겹치지 않도록 서버가 새로 만든다. */
    @Column(unique = true)
    private String storedName;

    private String contentType;

    private long size;

    @CreationTimestamp
    private LocalDateTime uploadedAt;
}
