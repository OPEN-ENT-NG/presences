package fr.openent.statistics_presences.bean;

import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.bean.timeslot.Timeslot;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class StatProcessSettings {

    private String studentName;
    private List<String> studentClassNames;
    private List<String> studentClassIds;
    private List<String> audienceIds;
    private Timeslot timeslot;

    @SuppressWarnings("unchecked")
    public void setStudentInfo(JsonObject student) {
        this.studentName = student.getString(Field.NAME);
        this.studentClassNames = student.getJsonArray(Field.CLASSNAME, new JsonArray()).getList();
        this.studentClassIds = student.getJsonArray(Field.CLASSIDS, new JsonArray()).getList();
    }
    
    @SuppressWarnings("unchecked")
    public void setAudienceIds(JsonArray audienceIds) {
        this.audienceIds = audienceIds != null ? audienceIds.getList() : new ArrayList<>();
    }

    public void setTimeslot(JsonObject timeslot) {
        this.timeslot = new Timeslot(timeslot);
    }

    public String getStudentName() {
        return studentName;
    }

    public List<String> getStudentClassIds() {
        return studentClassIds;
    }

    public List<String> getStudentClassNames() {
        return studentClassNames;
    }

    public List<String> getAudienceIds() {
        return audienceIds;
    }

    public Timeslot getTimeslot() {
        return timeslot;
    }


}
