package com.example.kodyjobdam.banner.repository;

import com.example.kodyjobdam.banner.entity.BannerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BannerRepository extends JpaRepository<BannerEntity, Long> {

    Optional<BannerEntity> findFirstByActiveTrueOrderByUpdatedAtDesc();

    List<BannerEntity> findByActiveTrue();
}
