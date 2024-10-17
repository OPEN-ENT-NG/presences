package fr.openent.presences.helper;

import fr.openent.presences.model.Discipline;
import fr.openent.presences.model.Person.User;
import fr.openent.presences.model.Presence.MarkedStudent;
import fr.openent.presences.model.Presence.Presence;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class PresenceHelper {

    /**
     * Convert JsonArray into presence list
     *
     * @param presenceArray jsonArray response
     * @return new list of events
     */
    public static List<Presence> getPresenceListFromJsonArray(JsonArray presenceArray) {
        List<Presence> presenceList = new ArrayList<>();
        for (Object o : presenceArray) {
            if (!(o instanceof JsonObject)) continue;
            Presence presence = new Presence((JsonObject) o);
            presenceList.add(presence);
        }
        return presenceList;
    }

    /**
     * Convert JsonArray into marked student  list
     *
     * @param markedStudentsArray jsonArray response
     * @return new list of marked student
     */
    public static List<MarkedStudent> getMarkedStudentListFromJsonArray(JsonArray markedStudentsArray) {
        List<MarkedStudent> MarkedStudents = new ArrayList<>();
        for (Object o : markedStudentsArray) {
            if (!(o instanceof JsonObject)) continue;
            MarkedStudent markedStudent = new MarkedStudent((JsonObject) o);
            MarkedStudents.add(markedStudent);
        }
        return MarkedStudents;
    }

    /**
     * Convert List presences into presence JsonArray
     *
     * @param presencesList presences list
     * @return new JsonArray of presences
     */
    public static JsonArray toPresencesJsonArray(List<Presence> presencesList) {
        JsonArray presences = new JsonArray();
        for (Presence presence : presencesList) {
            presences.add(presence.toJSON());
        }
        return presences;
    }

    /**
     * Convert List marked students into marked students JsonArray
     *
     * @param markedStudentsList marked student list
     * @return new JsonArray of marked students
     */
    public static JsonArray toMarkedStudentsJsonArray(List<MarkedStudent> markedStudentsList) {
        JsonArray markedStudents = new JsonArray();
        for (MarkedStudent markedStudent : markedStudentsList) {
            markedStudents.add(markedStudent.toJSON());
        }
        return markedStudents;
    }

    public void addMarkedStudentsToPresence(List<Presence> presences, List<MarkedStudent> markedStudents,
                                            Future<JsonObject> future) {
        for (Presence presence : presences) {
            for (MarkedStudent markedStudent : markedStudents) {
                if (presence.getId().equals(markedStudent.getPresenceId())) {
                    presence.getMarkedStudents().add(markedStudent);
                }
            }
        }
        future.complete();
    }

    public void addOwnerToPresence(List<Presence> presences, List<User> owners, Future<JsonObject> future) {
        for (Presence presence : presences) {
            for (User owner : owners) {
                if (presence.getOwner().getId().equals(owner.getId())) {
                    presence.setOwner(owner);
                }
            }
        }
        future.complete();
    }

    public void addDisciplineToPresence(List<Presence> presences, List<Discipline> disciplines, Future<JsonObject> future) {
        for (Presence presence : presences) {
            for (Discipline discipline : disciplines) {
                if (presence.getDiscipline().getId() != null) {
                    if (presence.getDiscipline().getId().equals(discipline.getId())) {
                        presence.setDiscipline(discipline);
                    }
                }
            }
        }
        future.complete();
    }

}
