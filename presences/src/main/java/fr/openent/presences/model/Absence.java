package fr.openent.presences.model;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Absence implements Cloneable {
    public static List<String> MANDATORY_ATTRIBUTE = new ArrayList<>();

    private Integer id;
    private Boolean absence;
    private String studentId;
    private Integer reasonId;
    private String startDate;
    private String endDate;

    public Absence(JsonObject absence, List<String> mandatoryAttributes) {
        for (String attribute : mandatoryAttributes) {
            if (!absence.containsKey(attribute) || absence.getValue(attribute) == null) {
                throw new IllegalArgumentException("[Presences@AbsenceModel] Mandatory attribute not present " + attribute);
            }
        }
        this.id = absence.getInteger("id", null);
        this.absence = null;
        this.studentId = absence.getString("student_id", "");
        this.reasonId = absence.getInteger("reason_id", null);
        this.startDate = absence.getString("start_date", "");
        this.endDate = absence.getString("end_date", "");
    }

    @Override
    public Absence clone() {
        try {
            return (Absence) super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("id", this.id)
                .put("absence", this.absence)
                .put("student_id", this.studentId)
                .put("reason_id", this.reasonId)
                .put("start_date", this.startDate)
                .put("end_date", this.endDate);
    }
}
