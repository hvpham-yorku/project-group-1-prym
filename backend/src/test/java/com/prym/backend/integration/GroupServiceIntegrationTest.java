package com.prym.backend.integration;

import com.prym.backend.model.User;
import com.prym.backend.service.AuthService;
import com.prym.backend.service.BuyerService;
import com.prym.backend.service.GroupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
public class GroupServiceIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private BuyerService buyerService;
    @Autowired private GroupService groupService;

    //Helper: registers a user and creates their buyer profile in one step
    //Basically this is making a mock user for the site with fake data
    private User registerBuyer(String email, String username) {
        User user = authService.register(
                email, "pass123", User.Role.BUYER,
                username, "Test", "Buyer", "416-555-0000", null
        );
        buyerService.createBuyerProfile(user.getId(), "Chuck");
        return user;
    }

    //Group is created with the correct name,certifications, and creator is automatically added as the first member
    @Test
    void createGroup_PersistsToDatabase() {
        User user = registerBuyer("user1@example.com", "user1");

        Map<String, Object> result = groupService.createGroup(user.getId(), "Halal Crew", "HALAL");

        assertNotNull(result.get("groupId"));
        assertEquals("Halal Crew", result.get("groupName"));
        assertEquals(List.of("HALAL"), result.get("certifications"));
        assertEquals(1, result.get("memberCount")); // creator is auto-added
        assertTrue((Boolean) result.get("alreadyJoined"));
    }

    //a buyer who is already a member of a group cannot create a second one
    @Test
    void createGroup_BuyerAlreadyInGroup_ThrowsException() {
        User user = registerBuyer("user2@example.com", "user2");
        groupService.createGroup(user.getId(), "First Group", "");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> groupService.createGroup(user.getId(), "Second Group", ""));

        assertTrue(ex.getMessage().contains("already in a group"));
    }

    //blank group name is rejected
    @Test
    void createGroup_BlankName_ThrowsException() {
        User user = registerBuyer("user3@example.com", "user3");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> groupService.createGroup(user.getId(), "   ", ""));

        assertEquals("Group name is required.", ex.getMessage());
    }

    //a second buyer joins, memberCount goes to two and the joiner's alreadyJoined is true
    @Test
    void joinGroup_PersistsToDatabase() {
        User creator = registerBuyer("user4a@example.com", "user4a");
        User joiner  = registerBuyer("user4b@example.com", "user4b");

        Map<String, Object> group = groupService.createGroup(creator.getId(), "Open Group", "ORGANIC");
        Long groupId = (Long) group.get("groupId");

        Map<String, Object> result = groupService.joinGroup(joiner.getId(), groupId);

        assertEquals(2, result.get("memberCount"));
        assertTrue((Boolean) result.get("alreadyJoined"));
    }

    //a member tries to join another group
    @Test
    void joinGroup_BuyerAlreadyInGroup_ThrowsException() {
        User creator = registerBuyer("user5a@example.com", "user5a");
        User joiner  = registerBuyer("user5b@example.com", "user5b");

        Map<String, Object> group1 = groupService.createGroup(creator.getId(), "Group One", "");
        Long groupId1 = (Long) group1.get("groupId");

        groupService.createGroup(joiner.getId(), "Group Two", "");

        // joiner is already in group2 (as creator), so joining group1 must fail
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> groupService.joinGroup(joiner.getId(), groupId1));

        assertTrue(ex.getMessage().contains("already in a group"));
    }

    //try to join a group that doesn't exist
    @Test
    void joinGroup_GroupNotFound_ThrowsException() {
        User user = registerBuyer("user6@example.com", "user6");

        assertThrows(RuntimeException.class,
                () -> groupService.joinGroup(user.getId(), 999999L));
    }

    //cuts string is saved and reflected in the DTO
    @Test
    void saveCuts_PersistsToDatabase() {
        User user = registerBuyer("user7@example.com", "user7");
        Map<String, Object> group = groupService.createGroup(user.getId(), "Cut Savers", "");
        Long groupId = (Long) group.get("groupId");

        Map<String, Object> result = groupService.saveCuts(user.getId(), groupId, "Chuck, Rib");

        assertEquals("Chuck, Rib", result.get("myClaimedCuts"));
    }

    //saving new cuts replaces the previous selection
    @Test
    void saveCuts_UpdatesExistingCuts() {
        User user = registerBuyer("user8@example.com", "user8");
        Map<String, Object> group = groupService.createGroup(user.getId(), "Update Cuts Group", "");
        Long groupId = (Long) group.get("groupId");

        groupService.saveCuts(user.getId(), groupId, "Chuck");
        Map<String, Object> result = groupService.saveCuts(user.getId(), groupId, "Rib, Sirloin");

        assertEquals("Rib, Sirloin", result.get("myClaimedCuts"));
    }

    // a blank cuts string clears the member's selection
    @Test
    void saveCuts_ClearsCuts() {
        User user = registerBuyer("user9@example.com", "user9");
        Map<String, Object> group = groupService.createGroup(user.getId(), "Clear Cuts Group", "");
        Long groupId = (Long) group.get("groupId");

        groupService.saveCuts(user.getId(), groupId, "Chuck");
        Map<String, Object> result = groupService.saveCuts(user.getId(), groupId, "");

        assertNull(result.get("myClaimedCuts"));
    }

    //buyer A claims Chuck x2 (both slots), then buyer B tries to claim Chuck and is rejected
    @Test
    void saveCuts_ExceedsAvailableSlots_ThrowsException() {
        User buyerA = registerBuyer("user10a@example.com", "user10a");
        User buyerB = registerBuyer("user10b@example.com", "user10b");

        Map<String, Object> group = groupService.createGroup(buyerA.getId(), "Full Chuck Group", "");
        Long groupId = (Long) group.get("groupId");
        groupService.joinGroup(buyerB.getId(), groupId);

        groupService.saveCuts(buyerA.getId(), groupId, "Chuck x2"); // fills both slots

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> groupService.saveCuts(buyerB.getId(), groupId, "Chuck"));

        assertTrue(ex.getMessage().contains("'Chuck' only has 0 slot(s) left"));
    }

    //buyer who never joined tries to save cuts
    @Test
    void saveCuts_NotMember_ThrowsException() {
        User creator   = registerBuyer("user11a@example.com", "user11a");
        User outsider  = registerBuyer("user11b@example.com", "user11b");

        Map<String, Object> group = groupService.createGroup(creator.getId(), "Private Group", "");
        Long groupId = (Long) group.get("groupId");

        assertThrows(RuntimeException.class,
                () -> groupService.saveCuts(outsider.getId(), groupId, "Chuck"));
    }

    //after leaving, the buyer's group list is empty
    @Test
    void leaveGroup_RemovesMembership() {
        User user = registerBuyer("user12@example.com", "user12");
        Map<String, Object> group = groupService.createGroup(user.getId(), "Temp Group", "");
        Long groupId = (Long) group.get("groupId");

        // Add a second member so the group survives the leave
        User other = registerBuyer("user12b@example.com", "user12b");
        groupService.joinGroup(other.getId(), groupId);

        groupService.leaveGroup(user.getId(), groupId);

        List<Map<String, Object>> myGroups = groupService.getMyGroups(user.getId());
        assertTrue(myGroups.isEmpty());
    }

    //when the only member leaves, the group itself is deleted and can no longer be fetched
    @Test
    void leaveGroup_LastMember_DeletesGroup() {
        User user = registerBuyer("user13@example.com", "user13");
        Map<String, Object> group = groupService.createGroup(user.getId(), "Solo Group", "");
        Long groupId = (Long) group.get("groupId");

        groupService.leaveGroup(user.getId(), groupId);

        // Group row is gone — getGroup must throw
        assertThrows(RuntimeException.class,
                () -> groupService.getGroup(user.getId(), groupId));
    }

    //ensure you can't leave a group you're not a member of
    @Test
    void leaveGroup_NotMember_ThrowsException() {
        User creator  = registerBuyer("user14a@example.com", "user14a");
        User outsider = registerBuyer("user14b@example.com", "user14b");

        Map<String, Object> group = groupService.createGroup(creator.getId(), "Another Group", "");
        Long groupId = (Long) group.get("groupId");

        assertThrows(RuntimeException.class,
                () -> groupService.leaveGroup(outsider.getId(), groupId));
    }

    //a member does not see their own group in the available list
    @Test
    void getAvailableGroups_ExcludesJoinedGroups() {
        User user = registerBuyer("user15@example.com", "user15");
        groupService.createGroup(user.getId(), "My Group", "");

        List<Map<String, Object>> available = groupService.getAvailableGroups(user.getId());

        boolean seesOwnGroup = available.stream()
                .anyMatch(g -> "My Group".equals(g.get("groupName")));
        assertFalse(seesOwnGroup);
    }

    //a non-member sees a group created by another buyer
    @Test
    void getAvailableGroups_IncludesOthersGroups() {
        User creator = registerBuyer("user16a@example.com", "user16a");
        User browser = registerBuyer("user16b@example.com", "user16b");

        groupService.createGroup(creator.getId(), "Public Group", "KOSHER");

        List<Map<String, Object>> available = groupService.getAvailableGroups(browser.getId());

        boolean seesGroup = available.stream()
                .anyMatch(g -> "Public Group".equals(g.get("groupName")));
        assertTrue(seesGroup);
    }

    //returned DTO has expected fields including certifications list, memberCount, and alreadyJoined flag
    @Test
    void getMyGroups_ReturnsCorrectGroupDetails() {
        User user = registerBuyer("user17@example.com", "user17");
        groupService.createGroup(user.getId(), "Detail Group", "HALAL,ORGANIC");

        List<Map<String, Object>> myGroups = groupService.getMyGroups(user.getId());

        assertEquals(1, myGroups.size());
        Map<String, Object> dto = myGroups.get(0);
        assertEquals("Detail Group", dto.get("groupName"));
        assertEquals(1, dto.get("memberCount"));
        assertTrue((Boolean) dto.get("alreadyJoined"));

        @SuppressWarnings("unchecked")
        List<String> certs = (List<String>) dto.get("certifications");
        assertTrue(certs.contains("HALAL"));
        assertTrue(certs.contains("ORGANIC"));
    }

    //after buyer A claims Rib x2, buyer B's view shows othersClaimedQty["Rib"] = 2
    @Test
    void othersClaimedQty_ReflectsOtherMembersCuts() {
        User buyerA = registerBuyer("user18a@example.com", "user18a");
        User buyerB = registerBuyer("user18b@example.com", "user18b");

        Map<String, Object> group = groupService.createGroup(buyerA.getId(), "Rib Group", "");
        Long groupId = (Long) group.get("groupId");
        groupService.joinGroup(buyerB.getId(), groupId);

        groupService.saveCuts(buyerA.getId(), groupId, "Rib x2");

        Map<String, Object> dto = groupService.getGroup(buyerB.getId(), groupId);

        @SuppressWarnings("unchecked")
        Map<String, Integer> othersQty = (Map<String, Integer>) dto.get("othersClaimedQty");
        assertEquals(2, othersQty.get("Rib"));
    }
}
