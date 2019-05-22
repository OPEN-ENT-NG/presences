package fr.openent.presences.common.security;

import org.entcore.common.user.UserInfos;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

public class ExportRightTest {
    private UserInfos.Action truthyAction = new UserInfos.Action();
    private UserInfos.Action falsyAction = new UserInfos.Action();
    private String truthyActionDisplayName = "presences.export";
    private String falsyActionDisplayName = "presences.false.action.display.name";

    @Before
    public void initAuthorizedActions() {
        truthyAction.setDisplayName(truthyActionDisplayName);
        truthyAction.setName("fr.openent.presences.controller.CourseController|exportCourses");
        truthyAction.setType("SECURED_ACTION_WORKFLOW");

        falsyAction.setDisplayName(falsyActionDisplayName);
        falsyAction.setName("fr.openent.presences.falsy.FalsyController|falsyMethod");
        falsyAction.setType("SECURED_ACTION_WORKFLOW");
    }

    @Test
    @DisplayName("ExportRight security filter should handle false")
    public void exportRight_security_filter_should_handle_false() {
        List<UserInfos.Action> actions = new ArrayList<>();
        UserInfos user = new UserInfos();
        actions.add(falsyAction);
        user.setAuthorizedActions(actions);

        new ExportRight().authorize(null, null, user, Assert::assertFalse);
    }

    @Test
    @DisplayName("ExportRight security filter should handler true")
    public void exportRight_security_filter_should_handle_true() {
        List<UserInfos.Action> actions = new ArrayList<>();
        UserInfos user = new UserInfos();
        actions.add(truthyAction);
        user.setAuthorizedActions(actions);

        new ExportRight().authorize(null, null, user, Assert::assertTrue);
    }
}
