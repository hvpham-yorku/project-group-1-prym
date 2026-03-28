package com.prym.backend.repository;

import com.prym.backend.model.AssociationStatus;
import com.prym.backend.model.GroupSellerAssociation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupSellerAssociationRepository extends JpaRepository<GroupSellerAssociation, Long> {

    // Read-only lookup — safe to call inside read-only transactions.
    Optional<GroupSellerAssociation> findByGroupIdAndStatusIn(Long groupId, List<AssociationStatus> statuses);

    // Write-path lookup with pessimistic lock — prevents concurrent insert race.
    // Only call this inside a @Transactional (non-read-only) method.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM GroupSellerAssociation a WHERE a.group.id = :groupId AND a.status IN :statuses")
    Optional<GroupSellerAssociation> findByGroupIdAndStatusInForUpdate(
            @Param("groupId") Long groupId,
            @Param("statuses") List<AssociationStatus> statuses);

    // All pending requests for a seller (they need to approve/deny)
    List<GroupSellerAssociation> findBySellerIdAndStatus(Long sellerId, AssociationStatus status);

    // All active associations for a seller (ASSOCIATED + PENDING_DISASSOCIATION)
    List<GroupSellerAssociation> findBySellerIdAndStatusIn(Long sellerId, List<AssociationStatus> statuses);

    // Most recent record for a specific group-seller pair (for 24-hr lockdown check)
    Optional<GroupSellerAssociation> findTopByGroupIdAndSellerIdOrderByRequestedAtDesc(Long groupId, Long sellerId);
}
