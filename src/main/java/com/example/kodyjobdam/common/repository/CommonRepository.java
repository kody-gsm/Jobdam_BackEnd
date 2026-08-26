package com.example.kodyjobdam.common.repository;

import com.example.kodyjobdam.common.entity.CommonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CommonRepository extends JpaRepository<CommonEntity, Long> {

    List<CommonEntity> findAllByDateAndPeriod(LocalDate date, String period);

    List<CommonEntity> findByUser_id(Long userId);
}