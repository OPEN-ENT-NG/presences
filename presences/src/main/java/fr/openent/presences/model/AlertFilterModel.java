package fr.openent.presences.model;

import java.util.List;

public class AlertFilterModel {

    String structureId;
    List<String> types;
    List<String> students;
    String startDate;
    String endDate;
    String startTime;
    String endTime;
    Integer page;

    public AlertFilterModel(String structureId, List<String> types, List<String> students, String startDate, String endDate, String startTime, String endTime, Integer page) {
        this.structureId = structureId;
        this.types = types;
        this.students = students;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.page = page;
    }

    public String getStructureId() {
        return structureId;
    }

    public List<String> getTypes() {
        return types;
    }

    public List<String> getStudents() {
        return students;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public Integer getPage() {
        return page;
    }
}
