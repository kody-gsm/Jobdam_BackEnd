package com.example.kodyjobdam.user;

import com.example.kodyjobdam.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByDataGsmStudentId(Long dataGsmStudentId);

    Optional<User> findByRefreshToken(String refreshToken);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    List<User> findByRoleOrderByNameAsc(UserRole role);
}
