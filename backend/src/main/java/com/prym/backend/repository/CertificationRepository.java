package com.prym.backend.repository;

import com.prym.backend.model.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//database access for seller certifications (Halal, Organic, etc)
@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {
    //get all certs for a seller
    List<Certification> findBySellerId(Long sellerId);
    //wipe all certs for a seller, used by the bulk-replace flow.
    //needs to be called inside a @Transactional method or it will complain
    @Modifying
    @Transactional
    void deleteBySellerId(Long sellerId);
}