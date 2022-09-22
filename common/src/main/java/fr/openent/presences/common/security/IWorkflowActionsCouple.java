package fr.openent.presences.common.security;

import fr.openent.presences.common.helper.WorkflowHelper;
import org.entcore.common.user.UserInfos;

public interface IWorkflowActionsCouple {
    String getUnrestrictedAction();
    String getRestrictedAction();

    /**
     * @param user User info
     * @param restrictedCondition Restriction condition
     * @return true if user has unrestricted right or if user has restricted right AND restrictedCondition is true
     */
    default boolean hasRight(UserInfos user, Boolean restrictedCondition) {
        return WorkflowHelper.hasRight(user, this.getUnrestrictedAction()) ||
                (WorkflowHelper.hasRight(user, this.getRestrictedAction()) && Boolean.TRUE.equals(restrictedCondition));
    }

    /**
     * @param user User info
     * @return true if user has unrestricted right or if user has restricted right
     */
    default boolean hasRight(UserInfos user) {
        return hasRight(user, true);
    }

    /**
     * @param user User info
     * @param restrictedCondition Restriction condition
     * @return true if user don't have unrestricted right and user has restricted right and restrictedCondition is true
     */
    default boolean hasOnlyRestrictedRight(UserInfos user, Boolean restrictedCondition) {
        return !WorkflowHelper.hasRight(user, this.getUnrestrictedAction())
                && WorkflowHelper.hasRight(user, this.getRestrictedAction())
                && Boolean.TRUE.equals(restrictedCondition);
    }

    /**
     * @param user User info
     * @return true if user don't have unrestricted right and user has restricted right
     */
    default boolean hasOnlyRestrictedRight(UserInfos user) {
        return hasOnlyRestrictedRight(user, true);
    }
}
