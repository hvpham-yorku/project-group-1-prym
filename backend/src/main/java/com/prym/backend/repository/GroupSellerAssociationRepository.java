package com.prym.backend.repository;

import com.prym.backend.model.AssociationStatus;
import com.prym.backend.model.GroupSellerAssociation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupSellerAssociationRepository extends JpaRepository<GroupSellerAssociation, Long> {

    // Find the active (non-terminal) record for a group — enforces one-at-a-time rule
    Optional<GroupSellerAssociation> findByGroupIdAndStatusIn(Long groupId, List<AssociationStatus> statuses);

    // All pending requests for a seller (they need to approve/deny)
    List<GroupSellerAssociation> findBySellerIdAndStatus(Long sellerId, AssociationStatus status);

    // All active associations for a seller (ASSOCIATED + PENDING_DISASSOCIATION)
    List<GroupSellerAssociation> findBySellerIdAndStatusIn(Long sellerId, List<AssociationStatus> statuses);

    // Most recent record for a specific group-seller pair (for 24-hr lockdown check)
    Optional<GroupSellerAssociation> findTopByGroupIdAndSellerIdOrderByRequestedAtDesc(Long groupId, Long sellerId);
}
