package com.prym.backend.repository;

import com.prym.backend.model.CowType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

//database access for cow type/breed listings
@Repository
public interface CowTypeRepository extends JpaRepository<CowType, Long> {
    //get all breed types offered by a specific seller
    List<CowType> findBySellerId(Long sellerId);
}
