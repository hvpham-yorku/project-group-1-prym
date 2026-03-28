package com.prym.backend.service;

import com.prym.backend.model.Cow;
import com.prym.backend.model.CowCut;
import com.prym.backend.model.CowType;
import com.prym.backend.repository.CowCutRepository;
import com.prym.backend.repository.CowRepository;
import com.prym.backend.repository.CowTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

//Manages individual cows and their cuts. This is where the 22-cut auto-generation
//happens when a new cow gets created, which is honestly the most important part.
@Service
public class CowService {

    private final CowRepository cowRepository;
    private final CowTypeRepository cowTypeRepository;
    private final CowCutRepository cowCutRepository;

    //spring injects all three repos here, we need all of them for cow + cut creation
    public CowService(CowRepository cowRepository, CowTypeRepository cowTypeRepository, CowCutRepository cowCutRepository) {
        this.cowRepository = cowRepository;
        this.cowTypeRepository = cowTypeRepository;
        this.cowCutRepository = cowCutRepository;
    }

    // Creates a specific cow and auto-generates all 22 CowCut records (11 cuts × 2 sides)
    @Transactional
    public Cow createCow(Long cowTypeId, String name, Double estimatedWeightLbs, LocalDate harvestDate) {
        CowType cowType = cowTypeRepository.findById(cowTypeId)
                .orElseThrow(() -> new RuntimeException("CowType not found"));

        Cow cow = new Cow();
        cow.setCowType(cowType);
        cow.setName(name);
        cow.setEstimatedWeightLbs(estimatedWeightLbs);
        cow.setHarvestDate(harvestDate);
        cow.setStatus(Cow.CowStatus.OPEN);

        Cow savedCow = cowRepository.save(cow);

        // Auto-generate one CowCut per cut per side (11 cuts × LEFT + RIGHT = 22 records)
        List<CowCut> cuts = new ArrayList<>();
        for (CowCut.CutName cutName : CowCut.CutName.values()) {
            for (CowCut.Side side : CowCut.Side.values()) {
                CowCut cut = new CowCut();
                cut.setCow(savedCow);
                cut.setCutName(cutName);
                cut.setSide(side);
                cut.setStatus(CowCut.CutStatus.AVAILABLE);
                cuts.add(cut);
            }
        }
        cowCutRepository.saveAll(cuts);

        return savedCow;
    }

    //gets all cows belonging to a seller, goes through the cowType relationship
    public List<Cow> getCowsBySeller(Long userId) {
        return cowRepository.findByCowTypeSellerId(userId);
    }

    //only returns cuts that havent been claimed yet for a specific cow
    public List<CowCut> getAvailableCuts(Long cowId) {
        return cowCutRepository.findByCowIdAndStatus(cowId, CowCut.CutStatus.AVAILABLE);
    }

    //returns every cut for a cow regardless of status, useful for the diagram display
    public List<CowCut> getAllCuts(Long cowId) {
        return cowCutRepository.findByCowId(cowId);
    }
}
