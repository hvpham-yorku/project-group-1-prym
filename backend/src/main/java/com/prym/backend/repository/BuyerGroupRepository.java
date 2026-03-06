package com.prym.backend.repository;

import com.prym.backend.model.BuyerGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuyerGroupRepository extends JpaRepository<BuyerGroup, Long> {
    // findAll() from JpaRepository is sufficient for browsing all groups
}
