package com.prym.backend.repository;

import com.prym.backend.model.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {
    List<Certification> findBySellerId(Long sellerId);
    void deleteBySellerId(Long sellerId);
}
