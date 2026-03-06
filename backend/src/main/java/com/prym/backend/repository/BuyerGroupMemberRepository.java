package com.prym.backend.repository;

import com.prym.backend.model.BuyerGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuyerGroupMemberRepository extends JpaRepository<BuyerGroupMember, Long> {

    List<BuyerGroupMember> findByGroupId(Long groupId);

    List<BuyerGroupMember> findByBuyerId(Long buyerId);

    Optional<BuyerGroupMember> findByGroupIdAndBuyerId(Long groupId, Long buyerId);

    boolean existsByGroupIdAndBuyerId(Long groupId, Long buyerId);
}
