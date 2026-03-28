package com.prym.backend.repository;

import com.prym.backend.model.BuyerGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

//database access for buyer groups
@Repository
public interface BuyerGroupRepository extends JpaRepository<BuyerGroup, Long> {
    //look up a group by its invite code, used when a buyer enters a code to join
    Optional<BuyerGroup> findByInviteCode(String inviteCode);
}
