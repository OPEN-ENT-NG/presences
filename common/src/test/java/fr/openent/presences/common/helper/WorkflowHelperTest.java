package fr.openent.presences.common.helper;

import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WorkflowHelperTest {

    private UserInfos user = new UserInfos();
    private String truthyActionDisplayName = "presences.event.create";
    private String falsyActionDisplayName = "presences.false.action.display.name";

    @Before
    public void initAuthorizedActions() {
        List<UserInfos.Action> actions = new ArrayList<>();
        UserInfos.Action action = new UserInfos.Action();
        action.setDisplayName(truthyActionDisplayName);
        action.setName("fr.openent.presences.controller.EventController|postEvent");
        action.setType("SECURED_ACTION_WORKFLOW");
        actions.add(action);
        user.setAuthorizedActions(actions);
    }

    @Test(expected = NullPointerException.class)
    @DisplayName("WorkflowHelper.hasRight should throws NullPointerException")
    public void hasRight_should_throws_NullPointException() throws NullPointerException {
        WorkflowHelper.hasRight(new UserInfos(), "");
    }

    @Test
    @DisplayName("WorkflowHelper.hasRight should returns false")
    public void hasRight_should_returns_false() {
        assertFalse(WorkflowHelper.hasRight(user, falsyActionDisplayName));
    }

    @Test
    @DisplayName("WorkflowHelper.hasRight should returns true")
    public void hasRight_should_returns_true() {
        assertTrue(WorkflowHelper.hasRight(user, truthyActionDisplayName));
    }
}
