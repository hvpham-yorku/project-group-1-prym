package com.prym.backend.repository;

import com.prym.backend.model.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    // Returns the last 50 messages for a group, oldest first
    List<GroupMessage> findTop50ByGroupIdOrderBySentAtAsc(Long groupId);
}
