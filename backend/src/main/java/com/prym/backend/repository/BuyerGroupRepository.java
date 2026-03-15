package com.prym.backend.repository;

import com.prym.backend.model.BuyerGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuyerGroupRepository extends JpaRepository<BuyerGroup, Long> {
    Optional<BuyerGroup> findByInviteCode(String inviteCode);
}
