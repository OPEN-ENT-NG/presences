package fr.openent.presences.common.helper;

import fr.openent.presences.core.constants.*;
import io.vertx.core.json.*;
import org.entcore.common.user.*;

import java.util.*;
import java.util.stream.*;

public class UserInfosHelper {

    private UserInfosHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static JsonObject toJSON(UserInfos userInfos) {
        return new JsonObject()
                .put(Field.HASAPP, userInfos.getHasApp())
                .put(Field.USERID, userInfos.getUserId())
                .put(Field.EXTERNALID, userInfos.getExternalId())
                .put(Field.FIRSTNAME, userInfos.getFirstName())
                .put(Field.LASTNAME, userInfos.getLastName())
                .put(Field.USERNAME, userInfos.getUsername())
                .put(Field.BIRTHDATE, userInfos.getBirthDate())
                .put(Field.CLASSNAMES, userInfos.getClasses())
                .put(Field.REALCLASSNAMES, userInfos.getRealClassNames())
                .put(Field.STRUCTURENAMES, userInfos.getStructureNames())
                .put(Field.UAI, userInfos.getUai())
                .put(Field.CHILDRENIDS, userInfos.getChildrenIds())
                .put(Field.LEVEL, userInfos.getLevel())
                .put(Field.TYPE, userInfos.getType())
                .put(Field.LOGIN, userInfos.getLogin())
                .put(Field.AUTHORIZEDACTIONS, getUserActionJSONArray(userInfos.getAuthorizedActions()))
                .put(Field.GROUPSIDS, userInfos.getGroupsIds())
                .put(Field.CLASSES, userInfos.getClasses())
                .put(Field.STRUCTURES, userInfos.getStructures());
    }

    @SuppressWarnings("unchecked")
    public static UserInfos getUserInfosFromJSON(JsonObject infos) {
        UserInfos user = new UserInfos();
        user.setHasApp(infos.getBoolean(Field.HASAPP));
        user.setUserId(infos.getString(Field.USERID));
        user.setExternalId(infos.getString(Field.EXTERNALID));
        user.setFirstName(infos.getString(Field.FIRSTNAME));
        user.setLastName(infos.getString(Field.LASTNAME));
        user.setUsername(infos.getString(Field.USERNAME));
        user.setBirthDate(infos.getString(Field.BIRTHDATE));
        if (infos.getJsonArray(Field.REALCLASSNAMES) != null) {
            user.setRealClassNames(infos.getJsonArray(Field.REALCLASSNAMES, new JsonArray()).getList());
        }
        user.setStructureNames(infos.getJsonArray(Field.STRUCTURENAMES, new JsonArray()).getList());
        user.setUai(infos.getJsonArray(Field.UAI, new JsonArray()).getList());
        user.setChildrenIds(infos.getJsonArray(Field.CHILDRENIDS, new JsonArray()).getList());
        user.setLevel(infos.getString(Field.LEVEL));
        user.setType(infos.getString(Field.TYPE));
        user.setLogin(infos.getString(Field.LOGIN));
        user.setAuthorizedActions(getUserActionsFromJSONArray(infos.getJsonArray(Field.AUTHORIZEDACTIONS, new JsonArray())));
        user.setGroupsIds(infos.getJsonArray(Field.GROUPSIDS, new JsonArray()).getList());
        user.setClasses(infos.getJsonArray(Field.CLASSES, new JsonArray()).getList());
        user.setStructures(infos.getJsonArray(Field.STRUCTURES, new JsonArray()).getList());
        return user;
    }

    public static JsonObject getUserActionJSON(UserInfos.Action action) {
        return new JsonObject()
                .put(Field.NAME, action.getName())
                .put(Field.DISPLAYNAME, action.getDisplayName())
                .put(Field.TYPE, action.getType());
    }

    public static JsonArray getUserActionJSONArray(List<UserInfos.Action> actions) {
        return new JsonArray(actions.stream().map(UserInfosHelper::getUserActionJSON).collect(Collectors.toList()));
    }

    public static UserInfos.Action getUserActionFromJSON(JsonObject oAction) {
        UserInfos.Action action = new UserInfos.Action();
        action.setName(oAction.getString(Field.NAME));
        action.setDisplayName(oAction.getString(Field.DISPLAYNAME));
        action.setType(oAction.getString(Field.TYPE));
        return action;
    }

     @SuppressWarnings("unchecked")
     public static List<UserInfos.Action> getUserActionsFromJSONArray(JsonArray actions) {
        return ((List<JsonObject>) actions.getList()).stream().map(UserInfosHelper::getUserActionFromJSON).collect(Collectors.toList());
     }

}
