package com.prym.backend.unit.service;
import com.prym.backend.service.CowService;

import com.prym.backend.model.Cow;
import com.prym.backend.model.CowCut;
import com.prym.backend.model.CowType;
import com.prym.backend.model.Seller;
import com.prym.backend.model.User;
import com.prym.backend.repository.CowCutRepository;
import com.prym.backend.repository.CowRepository;
import com.prym.backend.repository.CowTypeRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CowServiceTest {
    @Mock
    private CowRepository cowRepository;

    @Mock
    private CowTypeRepository cowTypeRepository;

    @Mock
    private CowCutRepository cowCutRepository;

    @InjectMocks
    private CowService cowService;

    private CowType testCowType;
    private Cow testCow;

    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setRole(User.Role.SELLER);

        Seller testSeller = new Seller();
        testSeller.setId(1L);
        testSeller.setUser(testUser);

        testCowType = new CowType();
        testCowType.setId(1L);
        testCowType.setSeller(testSeller);
        testCowType.setBreed(CowType.Breed.ANGUS);
        testCowType.setPricePerPound(5.99);

        testCow = new Cow();
        testCow.setId(1L);
        testCow.setCowType(testCowType);
        testCow.setName("Lot #1");
        testCow.setEstimatedWeightLbs(800.0);
        testCow.setHarvestDate(LocalDate.of(2026, 6, 3));
        testCow.setStatus(Cow.CowStatus.OPEN);
    }

    // Test 1: createCow_Success
    @Test
    public void createCow_Success() {
        when(cowTypeRepository.findById(1L)).thenReturn(Optional.of(testCowType));
        when(cowRepository.save(any(Cow.class))).thenReturn(testCow);
        when(cowCutRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        Cow result = cowService.createCow(1L, "Lot #1", 800.0, LocalDate.of(2026, 6, 1));

        assertNotNull(result);
        assertEquals("Lot #1", result.getName());
        assertEquals(800.0, result.getEstimatedWeightLbs());
        assertEquals(Cow.CowStatus.OPEN, result.getStatus());
        verify(cowRepository).save(any(Cow.class));
    }

    // Test 2: createCow_CowTypeNotFound
    @Test
    public void createCow_CowTypeNotFound() {
        when(cowTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> cowService.createCow(999L, "Lot #1", 800.0, LocalDate.of(2026, 6, 1)));

        verify(cowRepository, never()).save(any());
    }

    // Test 3: createCow_Generates22Cuts
    @Test
    public void createCow_Generates22Cuts() {
        when(cowTypeRepository.findById(1L)).thenReturn(Optional.of(testCowType));
        when(cowRepository.save(any(Cow.class))).thenReturn(testCow);

        when(cowCutRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        cowService.createCow(1L, "Lot #1", 800.0, LocalDate.of(2026, 6, 1));

        // Verify saveAll was called with exactly 22 cuts (11 cuts × 2 sides)
        verify(cowCutRepository).saveAll(argThat(list -> ((List<?>) list).size() == 22));
    }

    // Test 4: createCow_AllCutsAreAvailable
    @Test
    public void createCow_AllCutsAreAvailable() {
        when(cowTypeRepository.findById(1L)).thenReturn(Optional.of(testCowType));
        when(cowRepository.save(any(Cow.class))).thenReturn(testCow);
        when(cowCutRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        cowService.createCow(1L, "Lot #1", 800.0, LocalDate.of(2026, 6, 1));

        // Verify every cut in the saved list has AVAILABLE status
        verify(cowCutRepository).saveAll(argThat(list -> ((List<CowCut>) list).stream()
                .allMatch(cut -> cut.getStatus() == CowCut.CutStatus.AVAILABLE)));
    }

    // Test 5: getCowsBySeller_Success
    @Test
    public void getCowsBySeller_Success() {
        when(cowRepository.findByCowTypeSellerId(1L)).thenReturn(List.of(testCow));

        List<Cow> result = cowService.getCowsBySeller(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Lot #1", result.get(0).getName());
    }

    // Test 6: getAvailableCuts_Success
    @Test
    public void getAvailableCuts_Success() {
        CowCut availableCut = new CowCut();
        availableCut.setCow(testCow);
        availableCut.setCutName(CowCut.CutName.CHUCK);
        availableCut.setSide(CowCut.Side.LEFT);
        availableCut.setStatus(CowCut.CutStatus.AVAILABLE);

        when(cowCutRepository.findByCowIdAndStatus(1L, CowCut.CutStatus.AVAILABLE))
                .thenReturn(List.of(availableCut));

        List<CowCut> result = cowService.getAvailableCuts(1L);

        assertEquals(1, result.size());
        assertEquals(CowCut.CutStatus.AVAILABLE, result.get(0).getStatus());
        assertEquals(CowCut.CutName.CHUCK, result.get(0).getCutName());
    }

    // Test 7: getAllCuts_Success
    @Test
    public void getAllCuts_Success() {
        CowCut availableCut = new CowCut();
        availableCut.setStatus(CowCut.CutStatus.AVAILABLE);

        CowCut claimedCut = new CowCut();
        claimedCut.setStatus(CowCut.CutStatus.CLAIMED);

        when(cowCutRepository.findByCowId(1L)).thenReturn(List.of(availableCut, claimedCut));

        List<CowCut> result = cowService.getAllCuts(1L);

        assertEquals(2, result.size()); // both available and claimed returned
    }
}
