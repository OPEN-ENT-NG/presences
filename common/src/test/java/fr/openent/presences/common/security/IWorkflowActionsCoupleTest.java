package fr.openent.presences.common.security;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;

@RunWith(VertxUnitRunner.class)
public class IWorkflowActionsCoupleTest {
    enum WorkflowActionsCoupleTest implements IWorkflowActionsCouple {
        SEARCH("unrestricted", "restricted");

        private final String unrestricted;
        private final String restricted;
        WorkflowActionsCoupleTest(String unrestricted, String restricted) {
            this.restricted = restricted;
            this.unrestricted = unrestricted;
        }

        @Override
        public String getUnrestrictedAction() {
            return unrestricted;
        }

        @Override
        public String getRestrictedAction() {
            return restricted;
        }
    }

    UserInfos userInfosUnrestricted = new UserInfos();
    UserInfos userInfosRestricted = new UserInfos();
    UserInfos userInfosBoth = new UserInfos();
    UserInfos userInfosNone = new UserInfos();

    @Before
    public void setUp() {
        UserInfos.Action searchUnrestricted = new UserInfos.Action();
        searchUnrestricted.setDisplayName("unrestricted");
        UserInfos.Action searchRestricted = new UserInfos.Action();
        searchRestricted.setDisplayName("restricted");

        userInfosUnrestricted.setAuthorizedActions(Collections.singletonList(searchUnrestricted));
        userInfosRestricted.setAuthorizedActions(Collections.singletonList(searchRestricted));
        userInfosBoth.setAuthorizedActions(Arrays.asList(searchRestricted, searchUnrestricted));
        userInfosNone.setAuthorizedActions(Collections.emptyList());
    }

    @Test
    public void testHasRight(TestContext ctx) {
        ctx.assertTrue(WorkflowActionsCoupleTest.SEARCH.hasRight(userInfosUnrestricted));
        ctx.assertTrue(WorkflowActionsCoupleTest.SEARCH.hasRight(userInfosRestricted));
        ctx.assertTrue(WorkflowActionsCoupleTest.SEARCH.hasRight(userInfosBoth));
        ctx.assertFalse(WorkflowActionsCoupleTest.SEARCH.hasRight(userInfosNone));

        ctx.assertFalse(WorkflowActionsCoupleTest.SEARCH.hasRight(userInfosRestricted, false));
        ctx.assertTrue(WorkflowActionsCoupleTest.SEARCH.hasRight(userInfosUnrestricted, false));
    }

    @Test
    public void testHasOnlyRestrictedRight(TestContext ctx) {
        ctx.assertFalse(WorkflowActionsCoupleTest.SEARCH.hasOnlyRestrictedRight(userInfosUnrestricted));
        ctx.assertTrue(WorkflowActionsCoupleTest.SEARCH.hasOnlyRestrictedRight(userInfosRestricted));
        ctx.assertFalse(WorkflowActionsCoupleTest.SEARCH.hasOnlyRestrictedRight(userInfosBoth));
        ctx.assertFalse(WorkflowActionsCoupleTest.SEARCH.hasOnlyRestrictedRight(userInfosNone));

        ctx.assertFalse(WorkflowActionsCoupleTest.SEARCH.hasOnlyRestrictedRight(userInfosRestricted, false));
        ctx.assertFalse(WorkflowActionsCoupleTest.SEARCH.hasOnlyRestrictedRight(userInfosUnrestricted, false));
    }
}
