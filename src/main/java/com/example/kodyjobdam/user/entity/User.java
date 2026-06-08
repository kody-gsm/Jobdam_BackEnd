package com.example.kodyjobdam.user.entity;

import com.example.kodyjobdam.user.UserRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor//기본생성자 생성
@AllArgsConstructor//필드를 받는 생성자 생성
@Builder//build() -> 자동 생성
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String student_number;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;



}
