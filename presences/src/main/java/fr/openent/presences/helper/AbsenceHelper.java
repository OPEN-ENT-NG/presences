package fr.openent.presences.helper;

import fr.openent.presences.model.Absence;
import fr.openent.presences.model.Event.Event;
import fr.openent.presences.model.Event.EventType;
import fr.openent.presences.model.Person.Student;
import fr.openent.presences.model.Reason;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class AbsenceHelper {

    public static List<Absence> getAbsenceListFromJsonArray(JsonArray slotsJsonArray, List<String> mandatoryAttributes) {
        List<Absence> absences = new ArrayList<>();
        for (Object o : slotsJsonArray) {
            if (!(o instanceof JsonObject)) continue;
            Absence absence = new Absence((JsonObject) o, mandatoryAttributes);
            absences.add(absence);
        }
        return absences;
    }

    public static Event absenceToEvent(Absence absence, Student student, Reason reason) {
        Event convertedAbsence = new Event();
        convertedAbsence.setId(absence.getId());
        convertedAbsence.setStudent(student);
        convertedAbsence.setReason(reason);
        convertedAbsence.setComment("");
        convertedAbsence.setCounsellorInput(true);
        convertedAbsence.setCounsellorRegularisation(absence.isCounsellorRegularisation());
        convertedAbsence.setCreated(null);
        convertedAbsence.setStartDate(absence.getStartDate());
        convertedAbsence.setEndDate(absence.getEndDate());
        convertedAbsence.setRegisterId(null);
        convertedAbsence.setOwner(null);
        convertedAbsence.setEventType(new EventType(new JsonObject().put("id", 1).put("label", ""), EventType.MANDATORY_ATTRIBUTE));
        convertedAbsence.setMassmailed(false);
        return convertedAbsence;
    }

    public static JsonArray removeDuplicates(List<Event> events, JsonArray absences) {
        JsonArray newAbsences = new JsonArray();
        for (int i = 0; i < absences.size(); i++) {
            JsonObject absence = absences.getJsonObject(i);
            for (Event event : events) {
                if (!event.getId().equals(absence.getInteger("id"))) {
                    newAbsences.add(absence);
                }
            }
        }
        return newAbsences;
    }
}
