package fr.openent.statistics_presences.bean;

import fr.openent.statistics_presences.utils.EventType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface Stat {
    JsonObject toJSON();

    Stat setIndicator(String indicator);

    Stat setUser(String user);

    Stat setName(String name);

    Stat setClassName(String className);

    Stat setType(EventType type);

    Stat setStructure(String structure);

    Stat setAudiences(JsonArray audiences);
}
