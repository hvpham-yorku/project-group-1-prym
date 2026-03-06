package com.prym.backend.service;

import com.prym.backend.model.Certification;
import com.prym.backend.model.Seller;
import com.prym.backend.model.User;
import com.prym.backend.repository.CertificationRepository;
import com.prym.backend.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CertificationServiceTest {

    @Mock
    private CertificationRepository certificationRepository;

    @Mock
    private SellerRepository sellerRepository;

    @InjectMocks
    private CertificationService certificationService;

    private User testUser;
    private Seller testSeller;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("seller@example.com");
        testUser.setRole(User.Role.SELLER);

        testSeller = new Seller();
        testSeller.setId(10L);
        testSeller.setUser(testUser);
        testSeller.setShopName("Test Farm");
    }

    // Test 1: addCertification_Success
    @Test
    public void addCertification_Success() {
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(testSeller));
        when(certificationRepository.save(any(Certification.class))).thenAnswer(i -> i.getArgument(0));

        Certification result = certificationService.addCertification(1L, "HALAL", "USDA", LocalDate.of(2027, 1, 1));

        assertNotNull(result);
        assertEquals(Certification.CertificationType.HALAL, result.getName());
        assertEquals("USDA", result.getIssuingBody());
        assertEquals(LocalDate.of(2027, 1, 1), result.getExpiryDate());
        verify(certificationRepository).save(any(Certification.class));
    }

    // Test 2: addCertification_SellerNotFound
    @Test
    public void addCertification_SellerNotFound() {
        when(sellerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> certificationService.addCertification(999L, "HALAL", "USDA", null));

        assertEquals("Seller not found", ex.getMessage());
        verify(certificationRepository, never()).save(any());
    }

    // Test 3: addCertification_NoExpiryDate
    @Test
    public void addCertification_NoExpiryDate() {
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(testSeller));
        when(certificationRepository.save(any(Certification.class))).thenAnswer(i -> i.getArgument(0));

        Certification result = certificationService.addCertification(1L, "ORGANIC", "PCO", null);

        assertNull(result.getExpiryDate());
        assertEquals(Certification.CertificationType.ORGANIC, result.getName());
    }

    // Test 4: addCertification_InvalidType
    @Test
    public void addCertification_InvalidType() {
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(testSeller));

        // valueOf() on an invalid enum name throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> certificationService.addCertification(1L, "INVALID_TYPE", "USDA", null));
    }

    // Test 5: getCertificationsBySeller_Success
    @Test
    public void getCertificationsBySeller_Success() {
        Certification cert1 = new Certification();
        cert1.setName(Certification.CertificationType.HALAL);
        Certification cert2 = new Certification();
        cert2.setName(Certification.CertificationType.ORGANIC);

        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(testSeller));
        when(certificationRepository.findBySellerId(10L)).thenReturn(List.of(cert1, cert2));

        List<Certification> result = certificationService.getCertificationsBySeller(1L);

        assertEquals(2, result.size());
        assertEquals(Certification.CertificationType.HALAL, result.get(0).getName());
        assertEquals(Certification.CertificationType.ORGANIC, result.get(1).getName());
    }

    // Test 6: getCertificationsBySeller_SellerNotFound
    @Test
    public void getCertificationsBySeller_SellerNotFound() {
        when(sellerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> certificationService.getCertificationsBySeller(999L));

        assertEquals("Seller not found", ex.getMessage());
    }

    // Test 7: getCertificationsBySeller_Empty
    @Test
    public void getCertificationsBySeller_Empty() {
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(testSeller));
        when(certificationRepository.findBySellerId(10L)).thenReturn(List.of());

        List<Certification> result = certificationService.getCertificationsBySeller(1L);

        assertTrue(result.isEmpty());
    }

    // Test 8: deleteCertification_Success
    @Test
    public void deleteCertification_Success() {
        certificationService.deleteCertification(5L);

        verify(certificationRepository).deleteById(5L);
    }
}
