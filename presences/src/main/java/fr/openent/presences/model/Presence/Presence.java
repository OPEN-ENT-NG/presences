package fr.openent.presences.model.Presence;

import fr.openent.presences.helper.PresenceHelper;
import fr.openent.presences.model.Discipline;
import fr.openent.presences.model.Person.User;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Presence implements Cloneable {

    private Integer id;
    private String structureId;
    private String startDate;
    private String endDate;
    private Discipline discipline;
    private User owner;
    private List<MarkedStudent> markedStudents;

    public Presence(JsonObject presence) {
        this.id = presence.getInteger("id", null);
        this.structureId = presence.getString("structure_id", null);
        this.startDate = presence.getString("start_date", null);
        this.endDate = presence.getString("end_date", null);
        this.discipline = new Discipline(presence.getInteger("discipline_id", null));
        this.owner = new User(presence.getString("owner", null));
        this.markedStudents = new ArrayList<>();
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("id", this.id)
                .put("structureId", this.structureId)
                .put("startDate", this.startDate)
                .put("endDate", this.endDate)
                .put("discipline", this.discipline.toJSON())
                .put("owner", this.owner.toJSON())
                .put("markedStudents", PresenceHelper.toMarkedStudentsJsonArray(this.markedStudents));
    }

    @Override
    public Presence clone() {
        try {
            return (Presence) super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStructureId() {
        return structureId;
    }

    public void setStructureId(String structureId) {
        this.structureId = structureId;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Discipline getDiscipline() {
        return discipline;
    }

    public void setDiscipline(Discipline discipline) {
        this.discipline = discipline;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public List<MarkedStudent> getMarkedStudents() {
        return markedStudents;
    }

    public void setMarkedStudents(List<MarkedStudent> markedStudents) {
        this.markedStudents = markedStudents;
    }

}