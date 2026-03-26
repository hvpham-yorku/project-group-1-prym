package com.prym.backend.unit.service;
import com.prym.backend.service.GroupService;

import com.prym.backend.model.*;
import com.prym.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupServiceTest {

    @Mock
    private BuyerGroupRepository groupRepository;

    @Mock
    private BuyerGroupMemberRepository memberRepository;

    @Mock
    private BuyerRepository buyerRepository;

    @InjectMocks
    private GroupService groupService;

    private User testUser;
    private Buyer testBuyer;
    private BuyerGroup testGroup;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
        testUser.setFirstName("Jane");
        testUser.setRole(User.Role.BUYER);

        testBuyer = new Buyer();
        testBuyer.setId(10L);
        testBuyer.setUser(testUser);

        testGroup = new BuyerGroup();
        testGroup.setId(100L);
        testGroup.setName("Test Group");
        testGroup.setCreator(testBuyer);
        testGroup.setCertifications("HALAL");
    }

    // Helper: creates a BuyerGroupMember linking a buyer to a group with optional claimed cuts
    private BuyerGroupMember makeMember(BuyerGroup group, Buyer buyer, String cuts) {
        BuyerGroupMember m = new BuyerGroupMember();
        m.setGroup(group);
        m.setBuyer(buyer);
        m.setClaimedCuts(cuts);
        return m;
    }

    // Test 1: createGroup_Success — buyer not in any group, valid name → group is created
    @Test
    public void createGroup_Success() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(memberRepository.findByBuyerId(10L)).thenReturn(Collections.emptyList());
        when(groupRepository.save(any(BuyerGroup.class))).thenReturn(testGroup);
        when(memberRepository.save(any(BuyerGroupMember.class))).thenReturn(makeMember(testGroup, testBuyer, null));
        when(memberRepository.findByGroupId(100L)).thenReturn(List.of(makeMember(testGroup, testBuyer, null)));

        Map<String, Object> result = groupService.createGroup(1L, "Test Group", "HALAL");

        assertNotNull(result);
        assertEquals(100L, result.get("groupId"));
        assertEquals("Test Group", result.get("groupName"));
        verify(groupRepository).save(any(BuyerGroup.class));
        verify(memberRepository).save(any(BuyerGroupMember.class));
    }

    // Test 2: createGroup_BuyerAlreadyInGroup — buyer already has a membership → throws
    @Test
    public void createGroup_BuyerAlreadyInGroup() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(memberRepository.findByBuyerId(10L)).thenReturn(List.of(makeMember(testGroup, testBuyer, null)));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> groupService.createGroup(1L, "New Group", ""));

        assertTrue(ex.getMessage().contains("already in a group"));
        verify(groupRepository, never()).save(any());
    }

    // Test 3: createGroup_BlankName — blank group name → throws
    @Test
    public void createGroup_BlankName() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(memberRepository.findByBuyerId(10L)).thenReturn(Collections.emptyList());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> groupService.createGroup(1L, "  ", ""));

        assertEquals("Group name is required.", ex.getMessage());
        verify(groupRepository, never()).save(any());
    }

    // Test 4: createGroup_BuyerNotFound — userId has no buyer profile → throws
    @Test
    public void createGroup_BuyerNotFound() {
        when(buyerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> groupService.createGroup(999L, "Group", ""));
    }

    // Test 5: joinGroup_Success — buyer is not in any group, group exists → member added
    @Test
    public void joinGroup_Success() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.findByBuyerId(10L)).thenReturn(Collections.emptyList());
        when(memberRepository.existsByGroupIdAndBuyerId(100L, 10L)).thenReturn(false);
        when(memberRepository.save(any(BuyerGroupMember.class))).thenReturn(makeMember(testGroup, testBuyer, null));
        when(memberRepository.findByGroupId(100L)).thenReturn(List.of(makeMember(testGroup, testBuyer, null)));

        Map<String, Object> result = groupService.joinGroup(1L, 100L);

        assertNotNull(result);
        verify(memberRepository).save(any(BuyerGroupMember.class));
    }

    // Test 6: joinGroup_AlreadyInAGroup — buyer already has a membership → throws
    @Test
    public void joinGroup_AlreadyInAGroup() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.findByBuyerId(10L)).thenReturn(List.of(makeMember(testGroup, testBuyer, null)));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> groupService.joinGroup(1L, 100L));

        assertTrue(ex.getMessage().contains("already in a group"));
        verify(memberRepository, never()).save(any());
    }

    // Test 7: joinGroup_GroupNotFound — group does not exist → throws
    @Test
    public void joinGroup_GroupNotFound() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(groupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> groupService.joinGroup(1L, 999L));
    }

    // Test 8: leaveGroup_DeletesGroupWhenEmpty — last member leaves → group is deleted
    @Test
    public void leaveGroup_DeletesGroupWhenEmpty() {
        BuyerGroupMember membership = makeMember(testGroup, testBuyer, null);
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.findByGroupIdAndBuyerId(100L, 10L)).thenReturn(Optional.of(membership));
        when(memberRepository.findByGroupId(100L)).thenReturn(Collections.emptyList()); // no members left

        groupService.leaveGroup(1L, 100L);

        verify(memberRepository).delete(membership);
        verify(groupRepository).deleteById(100L); // empty group gets deleted
    }

    // Test 9: leaveGroup_KeepsGroupWhenMembersRemain — other members exist → group is kept
    @Test
    public void leaveGroup_KeepsGroupWhenMembersRemain() {
        User user2 = new User();
        user2.setId(2L);
        user2.setFirstName("Bob");
        Buyer buyer2 = new Buyer();
        buyer2.setId(20L);
        buyer2.setUser(user2);
        BuyerGroupMember otherMember = makeMember(testGroup, buyer2, null);

        BuyerGroupMember membership = makeMember(testGroup, testBuyer, null);
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.findByGroupIdAndBuyerId(100L, 10L)).thenReturn(Optional.of(membership));
        when(memberRepository.findByGroupId(100L)).thenReturn(List.of(otherMember)); // one member still remains

        groupService.leaveGroup(1L, 100L);

        verify(memberRepository).delete(membership);
        verify(groupRepository, never()).deleteById(any()); // group must NOT be deleted
    }

    // Test 10: leaveGroup_NotMember — buyer is not in this group → throws
    @Test
    public void leaveGroup_NotMember() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.findByGroupIdAndBuyerId(100L, 10L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> groupService.leaveGroup(1L, 100L));
    }

    // Test 11: saveCuts_Success — valid cuts with available slots → saved successfully
    @Test
    public void saveCuts_Success() {
        BuyerGroupMember membership = makeMember(testGroup, testBuyer, null);
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(memberRepository.findByGroupIdAndBuyerId(100L, 10L)).thenReturn(Optional.of(membership));
        when(memberRepository.findByGroupId(100L)).thenReturn(List.of(membership)); // only this buyer in group
        when(memberRepository.save(any(BuyerGroupMember.class))).thenReturn(membership);
        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));

        Map<String, Object> result = groupService.saveCuts(1L, 100L, "Chuck, Rib");

        assertNotNull(result);
        assertEquals("Chuck, Rib", membership.getClaimedCuts());
    }

    // Test 12: saveCuts_ExceedsMaxQuantity — other buyer claimed all slots → throws
    @Test
    public void saveCuts_ExceedsMaxQuantity() {
        // Another buyer already has both Chuck slots
        User user2 = new User();
        user2.setId(2L);
        user2.setFirstName("Bob");
        Buyer buyer2 = new Buyer();
        buyer2.setId(20L);
        buyer2.setUser(user2);
        BuyerGroupMember otherMember = makeMember(testGroup, buyer2, "Chuck x2");

        BuyerGroupMember membership = makeMember(testGroup, testBuyer, null);
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(memberRepository.findByGroupIdAndBuyerId(100L, 10L)).thenReturn(Optional.of(membership));
        when(memberRepository.findByGroupId(100L)).thenReturn(List.of(membership, otherMember));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> groupService.saveCuts(1L, 100L, "Chuck"));

        assertTrue(ex.getMessage().contains("'Chuck' only has 0 slot(s) left"));
    }

    // Test 13: saveCuts_NotMember — buyer is not in the group → throws
    @Test
    public void saveCuts_NotMember() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(memberRepository.findByGroupIdAndBuyerId(100L, 10L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> groupService.saveCuts(1L, 100L, "Chuck"));
    }

    // Test 14: saveCuts_ClearsCuts — null or blank cuts string clears the selection
    @Test
    public void saveCuts_ClearsCuts() {
        BuyerGroupMember membership = makeMember(testGroup, testBuyer, "Chuck");
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(memberRepository.findByGroupIdAndBuyerId(100L, 10L)).thenReturn(Optional.of(membership));
        when(memberRepository.findByGroupId(100L)).thenReturn(List.of(membership));
        when(memberRepository.save(any(BuyerGroupMember.class))).thenReturn(membership);
        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));

        groupService.saveCuts(1L, 100L, "");

        assertNull(membership.getClaimedCuts()); // blank string → null stored
    }

    // Test 15: getAvailableGroups_ExcludesJoinedGroups — buyer is already in testGroup → not shown
    @Test
    public void getAvailableGroups_ExcludesJoinedGroups() {
        BuyerGroupMember membership = makeMember(testGroup, testBuyer, null);
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(memberRepository.findByBuyerId(10L)).thenReturn(List.of(membership));
        when(groupRepository.findAll()).thenReturn(List.of(testGroup));

        List<Map<String, Object>> result = groupService.getAvailableGroups(1L);

        assertTrue(result.isEmpty()); // testGroup is excluded because buyer is already a member
    }

    // Test 16: getMyGroups_ReturnsOnlyJoinedGroups — returns groups the buyer is a member of
    @Test
    public void getMyGroups_ReturnsOnlyJoinedGroups() {
        BuyerGroupMember membership = makeMember(testGroup, testBuyer, null);
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(memberRepository.findByBuyerId(10L)).thenReturn(List.of(membership));
        when(memberRepository.findByGroupId(100L)).thenReturn(List.of(membership));

        List<Map<String, Object>> result = groupService.getMyGroups(1L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).get("groupId"));
    }

    // Test 17: getMyGroups_Empty — buyer has no memberships → empty list
    @Test
    public void getMyGroups_Empty() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(testBuyer));
        when(memberRepository.findByBuyerId(10L)).thenReturn(Collections.emptyList());

        List<Map<String, Object>> result = groupService.getMyGroups(1L);

        assertTrue(result.isEmpty());
    }
}
