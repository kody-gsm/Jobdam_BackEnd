package com.example.kodyjobdam.user;

import com.example.kodyjobdam.user.entity.User;
import com.example.kodyjobdam.user.dto.StudentSimpleResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByDataGsmStudentId(Long dataGsmStudentId);

    Optional<User> findByRefreshToken(String refreshToken);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    List<User> findByRoleOrderByNameAsc(UserRole role);

    List<User> findByRoleInOrderByNameAsc(Collection<UserRole> roles);

    @Query("select new com.example.kodyjobdam.user.dto.StudentSimpleResponse(u.id, u.name, u.student_number) "
            + "from User u where u.role = com.example.kodyjobdam.user.UserRole.STUDENT "
            + "order by u.student_number asc, u.name asc")
    List<StudentSimpleResponse> findStudentSimpleResponses();
}
