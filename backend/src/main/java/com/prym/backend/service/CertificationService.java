package com.prym.backend.service;

import com.prym.backend.model.Certification;
import com.prym.backend.model.Seller;
import com.prym.backend.repository.CertificationRepository;
import com.prym.backend.repository.SellerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

//Manages seller certifications like Halal, Organic, etc.
//Sellers can have multiple certs and we need to be able to replace them all at once too.
@Service
public class CertificationService {

    private final CertificationRepository certificationRepository;
    private final SellerRepository sellerRepository;

    public CertificationService(CertificationRepository certificationRepository, SellerRepository sellerRepository) {
        this.certificationRepository = certificationRepository;
        this.sellerRepository = sellerRepository;
    }

    //adds a single certification to a seller, used mainly by the data initializer
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

    //gets all certifications for a seller, pretty self explanatory
    public List<Certification> getCertificationsBySeller(Long userId) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        return certificationRepository.findBySellerId(seller.getId());
    }

    //removes a single certification by its id, verifies ownership first
    public void deleteCertification(Long userId, Long certId) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        Certification cert = certificationRepository.findById(certId)
                .orElseThrow(() -> new RuntimeException("Certification not found"));
        if (!cert.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("Certification does not belong to this seller");
        }
        certificationRepository.deleteById(certId);
    }

    //replaces ALL of a seller's certifications in one shot. Nukes the old ones
    //and creates fresh records from the list provided. Needs @Transactional because
    //if any cert name is invalid we want to roll back the delete too.
    @Transactional
    public void setCertifications(Long userId, List<String> certNames) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        //wipe existing certs first, then re-add from the new list
        certificationRepository.deleteBySellerId(seller.getId());
        for (String name : certNames) {
            Certification cert = new Certification();
            cert.setSeller(seller);
            try {
                cert.setName(Certification.CertificationType.valueOf(name));
            } catch (IllegalArgumentException e) {
                String valid = Arrays.stream(Certification.CertificationType.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "));
                throw new RuntimeException("Invalid certification: " + name + ". Valid options: " + valid);
            }
            certificationRepository.save(cert);
        }
    }
}
