package com.prym.backend.service;

import com.prym.backend.model.Certification;
import com.prym.backend.model.Seller;
import com.prym.backend.repository.CertificationRepository;
import com.prym.backend.repository.SellerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CertificationService {

    private final CertificationRepository certificationRepository;
    private final SellerRepository sellerRepository;

    public CertificationService(CertificationRepository certificationRepository, SellerRepository sellerRepository) {
        this.certificationRepository = certificationRepository;
        this.sellerRepository = sellerRepository;
    }

    public Certification addCertification(Long userId, String name, String issuingBody, LocalDate expiryDate) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        Certification cert = new Certification();
        cert.setSeller(seller);
        cert.setName(Certification.CertificationType.valueOf(name));
        cert.setIssuingBody(issuingBody);
        cert.setExpiryDate(expiryDate);

        return certificationRepository.save(cert);
    }

    public List<Certification> getCertificationsBySeller(Long userId) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        return certificationRepository.findBySellerId(seller.getId());
    }

    public void deleteCertification(Long certId) {
        certificationRepository.deleteById(certId);
    }
}
