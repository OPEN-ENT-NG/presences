package fr.openent.presences.common.helper;

import fr.openent.presences.core.constants.*;
import io.vertx.core.json.*;
import org.entcore.common.user.*;
import org.junit.*;
import org.junit.Test;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class UserInfosHelperTest {

    private final JsonObject actionsObject = new JsonObject()
            .put("name", "name")
            .put("displayName", "displayname")
            .put("type", "type");

    private final JsonObject userInfosObject = new JsonObject()
            .put("hasApp", true)
            .put("userId", "1")
            .put("externalId", "1")
            .put("firstName", "John")
            .put("lastName", "Doe")
            .put("username", "jdoe")
            .put("birthDay", "2000-01-01")
            .put("classNames", new JsonArray().add("class1").add("class2"))
            .put("realClassNames", new JsonArray().add("class1").add("class2"))
            .put("structureNames", new JsonArray().add("structure1").add("structure2"))
            .put("uai", new JsonArray().add("uai1").add("uai2"))
            .put("childrenIds", new JsonArray().add("1").add("2"))
            .put("level", "1")
            .put("type", "type")
            .put("login", "login")
            .put("authorizedActions", new JsonArray().add(actionsObject))
            .put("groupsIds", new JsonArray().add("1").add("2"))
            .put("classes", new JsonArray().add("class1").add("class2"))
            .put("structures", new JsonArray().add("structure1").add("structure2"));


    @Test
    @DisplayName("getUserActionFromJSON object should match the getUserActionJSON result")
    public void getUserActionFromJSON_should_match_the_getUserActionJSON_result() {
        UserInfos.Action action = UserInfosHelper.getUserActionFromJSON(actionsObject);
        JsonObject actionJSON = UserInfosHelper.getUserActionJSON(action);
        assertEquals(actionsObject, actionJSON);
    }

    @Test
    @DisplayName("getUserInfosFromJSON object should match the getUserInfosJSON result")
    public void getUserInfosFromJSON_should_match_the_getUserInfosJSON_result() {
        UserInfos userInfos = UserInfosHelper.getUserInfosFromJSON(userInfosObject);
        JsonObject userInfosJSON = UserInfosHelper.toJSON(userInfos);
        assertEquals(userInfosObject, userInfosJSON);
    }

}
