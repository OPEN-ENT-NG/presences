package fr.openent.statistics_presences.bean;

import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.IModel;
import fr.openent.presences.model.SharedDataModel;
import fr.openent.presences.model.StatisticsUser;
import fr.openent.presences.model.StructureStatisticsUser;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Report implements SharedDataModel<Report>, IModel<Report> {
    private Instant start;
    private Instant end;
    private String indicator;
    private boolean failureOnSave = false;
    private List<Failure> failures = new ArrayList<>();
    private List<ReportStructure> reportStructureList = null;

    public Report() {
    }

    public Report(JsonObject jsonObject) {
        this.load(jsonObject);
    }


    public Report(String indicator) {
        this.indicator = indicator;
    }

    public Report(String indicator, List<StructureStatisticsUser> structureStatisticsUserList) {
        this.indicator = indicator;
        this.reportStructureList = structureStatisticsUserList.stream()
                .map(ReportStructure::new)
                .collect(Collectors.toList());
    }

    public Report start() {
        this.start = Instant.now();
        return this;
    }

    public Report end() {
        this.end = Instant.now();
        return this;
    }

    public Report failOnSave() {
        this.failureOnSave = true;
        return this;
    }

    private Long duration() {
        return Duration.between(start, (end != null) ? end : Instant.now()).toMillis();
    }

    public Report fail(Failure failure) {
        this.failures.add(failure);
        return this;
    }

    public JsonObject toJSON() {
        JsonArray errors = new JsonArray(this.failures.stream().map(Failure::toString).collect(Collectors.toList()));
        JsonObject result = new JsonObject()
                .put(Field.NAME, this.indicator)
                .put(Field.DURATION, this.duration())
                .put(Field.ERRORCOUNT, this.failures.size())
                .put(Field.SAVED, !this.failureOnSave)
                .put(Field.ERRORS, errors);
        if (this.reportStructureList != null) {
            result.put(Field.NB_STUDENTS, this.reportStructureList.stream()
                    .mapToLong(reportStructure -> reportStructure.reportStudentList.size())
                    .sum()
            );
            result.put(Field.NB_STUDENTS_PROCESS, this.reportStructureList.stream()
                    .flatMap(reportStructure -> reportStructure.reportStudentList.stream())
                    .filter(reportStudent -> reportStudent.processed)
                    .count()
            );

            JsonArray structures = new JsonArray(this.reportStructureList.stream().map(reportStructure -> {
                JsonObject structure = new JsonObject();
                structure.put(Field.NB_STUDENTS, reportStructure.reportStudentList.size());
                structure.put(Field.NB_STUDENTS_PROCESS, (int) reportStructure.reportStudentList.stream()
                        .filter(reportStudent -> reportStudent.processed)
                        .count()
                );
                return structure.mergeIn(reportStructure.toJson());
            }).collect(Collectors.toList()));
            result.put(Field.STRUCTURES, structures);
        }

        return result;
    }

    public void completeStudent(String structureId, String studentId) {
        this.reportStructureList.stream()
                .filter(reportStructure -> reportStructure.structureId.equals(structureId))
                .flatMap(reportStructure -> reportStructure.reportStudentList.stream())
                .filter(reportStudent -> reportStudent.studentId.equals(studentId))
                .findFirst()
                .ifPresent(reportStudentResult -> reportStudentResult.processed = true);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(false, this);
    }

    @Override
    public Report load(JsonObject jsonObject) {
        this.start = jsonObject.getInstant(Field.START, null);
        this.end = jsonObject.getInstant(Field.END, null);
        this.indicator = jsonObject.getString(Field.INDICATOR, "");
        this.failureOnSave = jsonObject.getBoolean(Field.FAILURE_ON_SAVE, false);
        this.failures = IModelHelper.toList(jsonObject.getJsonArray(Field.FAILURES, new JsonArray()), Failure.class);
        this.reportStructureList = IModelHelper.toList(jsonObject.getJsonArray(Field.REPORT_STRUCTURE_LIST, new JsonArray()), ReportStructure.class);
        return this;
    }

    @Override
    public String getKey() {
        return this.getClass().getName();
    }

    @Override
    public boolean validate() {
        return false;
    }

    private static class ReportStudent implements IModel<ReportStudent> {
        protected String studentId;
        protected boolean processed;

        public ReportStudent(StatisticsUser statisticsUser) {
            this.studentId = statisticsUser.getId();
            this.processed = false;
        }

        public ReportStudent(JsonObject jsonObject) {
            throw new UnsupportedOperationException("Scheduled for issue #253");
        }

        @Override
        public JsonObject toJson() {
            return IModelHelper.toJson(false, this);
        }

        @Override
        public boolean validate() {
            return false;
        }
    }

    private static class ReportStructure implements IModel<ReportStructure> {
        protected String structureId;
        protected List<ReportStudent> reportStudentList;

        public ReportStructure(StructureStatisticsUser structureStatisticsUser) {
            this.structureId = structureStatisticsUser.getStructureId();
            this.reportStudentList = structureStatisticsUser.getStatisticsUsers().stream()
                    .map(ReportStudent::new)
                    .collect(Collectors.toList());
        }

        public ReportStructure(JsonObject jsonObject) {
            throw new UnsupportedOperationException("Scheduled for issue #253");
        }

        @Override
        public JsonObject toJson() {
            return IModelHelper.toJson(false, this);
        }

        @Override
        public boolean validate() {
            return false;
        }
    }
}
