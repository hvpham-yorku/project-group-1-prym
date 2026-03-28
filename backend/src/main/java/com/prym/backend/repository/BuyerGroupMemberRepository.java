package com.prym.backend.repository;

import com.prym.backend.model.BuyerGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

//database access for group memberships, basically the join table between buyers and groups
@Repository
public interface BuyerGroupMemberRepository extends JpaRepository<BuyerGroupMember, Long> {

    //all members of a specific group
    List<BuyerGroupMember> findByGroupId(Long groupId);

    //all groups a buyer belongs to (should be 0 or 1 since we enforce one group per buyer)
    List<BuyerGroupMember> findByBuyerId(Long buyerId);

    //find a specific membership record
    Optional<BuyerGroupMember> findByGroupIdAndBuyerId(Long groupId, Long buyerId);

    //quick check if a buyer is already in a group, avoids loading the full object
    boolean existsByGroupIdAndBuyerId(Long groupId, Long buyerId);
}
