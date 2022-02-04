package fr.openent.statistics_presences.bean.monthly;

import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.bean.Audience;
import fr.openent.statistics_presences.bean.User;
import fr.openent.statistics_presences.indicator.impl.Monthly;
import fr.openent.statistics_presences.model.StatisticsFilter;
import fr.openent.statistics_presences.utils.EventType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MonthlySearch {
    private static final String HALF_DAY = "HALF_DAY";
    private static final String DAY = "DAY";
    private static final String COUNTID = "countId";
    private final StatisticsFilter filter;
    List<String> totalAbsenceTypes = Arrays.asList(EventType.NO_REASON.name(), EventType.UNREGULARIZED.name(), EventType.REGULARIZED.name());
    private List<Audience> audiences = new ArrayList<>();
    private List<User> users = new LinkedList<>();
    private Map<String, List<Month>> statisticsByAudience;
    private Map<String, List<Student>> studentsByAudience;
    private List<AudienceMap> audienceResult;
    private String recoveryMethod;
    private String halfDay;

    public MonthlySearch(StatisticsFilter filter) {
        this.filter = filter;
    }

    public StatisticsFilter filter() {
        return this.filter;
    }

    public MonthlySearch setAudiences(List<Audience> audiences) {
        this.audiences = audiences;
        return this;
    }

    public List<Audience> audiences() {
        return this.audiences;
    }

    public MonthlySearch setUsers(List<User> users) {
        this.users = users;
        return this;
    }

    public List<AudienceMap> audienceResult() {
        return audienceResult;
    }

    public MonthlySearch setAudienceResult(List<AudienceMap> audienceResult) {
        this.audienceResult = audienceResult;
        return this;
    }

    public String recovery() {
        return recoveryMethod;
    }

    public String halfDay() {
        return halfDay;
    }

    public List<User> users() {
        return this.users;
    }


    public MonthlySearch setStatistics(Map<String, List<Month>> statistics) {
        this.statisticsByAudience = statistics;
        return this;
    }

    public MonthlySearch setStudents(Map<String, List<Student>> students) {
        this.studentsByAudience = students;
        return this;
    }

    public Map<String, List<Month>> statistics() {
        return this.statisticsByAudience;
    }

    public Map<String, List<Student>> students() {
        return this.studentsByAudience;
    }

    public MonthlySearch setRecoveryMethod(String recoveryMethod) {
        this.recoveryMethod = recoveryMethod;
        return this;
    }

    public MonthlySearch setHalfDay(String halfDay) {
        this.halfDay = halfDay;
        return this;
    }

    public JsonArray searchBasicEventTypedByAudiencePipeline() {
        List<String> types = typesIn(type -> !totalAbsenceTypes.contains(type));
        if (types == null || types.isEmpty()) return new JsonArray();

        return new JsonArray()
                .add(addStartAtField())
                .add(match(types))
                .add(addCountIdField())
                .add(audienceGroupByCountId())
                .add(audienceGroupById())
                .add(audienceProject());
    }

    public JsonArray searchAbsencesByAudiencePipeline() {
        List<String> types = typesIn(type -> totalAbsenceTypes.contains(type));
        if (types == null || types.isEmpty()) return new JsonArray();

        return new JsonArray()
                .add(addStartAtField())
                .add(match(types))
                .addAll(audienceGroupAbsences())
                .add(audienceProject());
    }

    public JsonArray searchBasicEventTypedByStudentPipeline() {
        List<String> types = typesIn(type -> !totalAbsenceTypes.contains(type));
        if (types == null || types.isEmpty())
            return new JsonArray();

        return new JsonArray()
                .add(addStartAtField())
                .add(match(types))
                .add(addCountIdField())
                .add(studentGroupByCountId())
                .add(studentGroupById())
                .add(studentProject());
    }

    public JsonArray searchAbsencesByStudentPipeline() {
        List<String> types = typesIn(type -> totalAbsenceTypes.contains(type));
        if (types == null || types.isEmpty())
            return new JsonArray();

        return new JsonArray()
                .add(addStartAtField())
                .add(match(types))
                .addAll(studentGroupAbsences())
                .add(studentProject());
    }

    /**
     * add a start_at field at the beginning of the pipeline to have start_date in mongo date format (and so handle it)
     *
     * @return field start_at
     */
    private JsonObject addStartAtField() {
        JsonObject dateString = new JsonObject().put("dateString", "$start_date");

        JsonObject dateFromString = new JsonObject()
                .put("$dateFromString", dateString);

        JsonObject statAtField = new JsonObject()
                .put("start_at", dateFromString);

        return new JsonObject()
                .put("$addFields", statAtField);
    }

    private JsonObject match(List<String> types) {

        List<String> userIdentifiers;
        if (this.filter().users() != null) {
            userIdentifiers = !this.filter().users().isEmpty()
                    ? this.filter().users()
                    : this.users().stream().map(User::id).collect(Collectors.toList());
        } else {
            userIdentifiers = new ArrayList<>();
        }

        JsonObject matcher = new JsonObject()
                .put("structure", this.filter.structure())
                .put("indicator", Monthly.class.getName())
                .put("$or", filterType(types))
                .put("start_date", this.startDateFilter())
                .put("end_date", this.endDateFilter());

        if (!this.filter.audiences().isEmpty() && userIdentifiers.isEmpty()) {
            matcher.put("audiences", audienceFilter());
        }

        if (!userIdentifiers.isEmpty()) {
            matcher.put("user", usersFilter(userIdentifiers));
        }

        return new JsonObject()
                .put("$match", matcher);
    }

    private JsonArray filterType(List<String> types) {
        List<String> typesToUse = types != null ? types : this.filter().types();
        JsonArray filters = new JsonArray();
        for (String type : typesToUse) {
            JsonObject filterType = new JsonObject()
                    .put("type", type);

            EventType eventType = EventType.valueOf(type);
            switch (eventType) {
                case UNREGULARIZED:
                case REGULARIZED:
                    JsonObject inFilterReasons = new JsonObject()
                            .put("$in", this.filter().reasons());
                    filterType.put("reason", inFilterReasons);
                    break;
                case SANCTION:
                    JsonObject inFilterSanctionTypes = new JsonObject()
                            .put("$in", this.filter.sanctionTypes());
                    filterType.put("punishment_type", inFilterSanctionTypes);
                    break;
                case PUNISHMENT:
                    JsonObject inFilterPunishmentTypes = new JsonObject()
                            .put("$in", this.filter.punishmentTypes());
                    filterType.put("punishment_type", inFilterPunishmentTypes);
                    break;
                default:
                    break;
            }

            filters.add(filterType);
        }

        return filters;
    }

    private JsonObject audienceFilter() {
        return new JsonObject()
                .put("$in", this.filter().audiences());
    }

    private JsonObject usersFilter(List<String> users) {
        return new JsonObject()
                .put("$in", users);
    }

    private JsonObject startDateFilter() {
        return new JsonObject()
                .put("$gte", this.filter().start());
    }

    private JsonObject endDateFilter() {
        return new JsonObject()
                .put("$lte", this.filter().end());
    }

    private JsonObject addCountIdField() {
        JsonObject groupedPunishmentIdExistsQuery = new JsonObject().put(Field.$GT,
                new JsonArray().add(Field.$GROUPED_PUNISHMENT_ID).addNull()
        );

        JsonObject cond = new JsonObject()
                .put(Field.$COND, new JsonArray()
                        .add(groupedPunishmentIdExistsQuery)
                        .add(Field.$GROUPED_PUNISHMENT_ID)
                        .add(Field.$_ID)
                );

        return new JsonObject().put(Field.$ADDFIELDS, new JsonObject().put(COUNTID, cond));
    }

    private JsonObject audienceGroupByCountId() {
        JsonObject id = new JsonObject()
                .put(Field.CLASS_NAME, String.format("$%s", Field.CLASS_NAME))
                .put(Field.MONTH, month())
                .put(Field.YEAR, year())
                .put(COUNTID, String.format("$%s", COUNTID));

        JsonObject group = id(id)
                .put(Field.SLOTS, sum(String.format("$%s", Field.SLOTS)))
                .put(Field.START_AT, first(String.format("$%s", Field.START_AT)));

        return group(group);
    }

    private JsonObject audienceGroupById() {
        JsonObject id = new JsonObject()
                .put(Field.CLASS_NAME, String.format("%s.%s", Field.$_ID, Field.CLASS_NAME))
                .put(Field.MONTH, String.format("%s.%s", Field.$_ID, Field.MONTH))
                .put(Field.YEAR, String.format("%s.%s", Field.$_ID, Field.YEAR));

        JsonObject group = id(id)
                .put(Field.COUNT, sum())
                .put(Field.START_AT, first(String.format("$%s", Field.START_AT)))
                .put(Field.SLOTS, sum());

        return group(group);
    }

    private JsonObject audienceGroup() {
        JsonObject id = new JsonObject()
                .put("class_name", "$class_name")
                .put("month", month())
                .put("year", year());

        JsonObject group = id(id)
                .put("count", sum())
                .put("start_at", first("$start_at"))
                .put("slots", sum());

        return group(group);
    }

    private JsonObject studentGroupByCountId() {
        JsonObject id = new JsonObject()
                .put(Field.USER, String.format("$%s", Field.USER))
                .put(Field.NAME, String.format("$%s", Field.NAME))
                .put(Field.MONTH, month())
                .put(Field.YEAR, year())
                .put(Field.CLASS_NAME, String.format("$%s", Field.CLASS_NAME))
                .put(COUNTID, String.format("$%s", COUNTID));

        JsonObject group = id(id)
                .put(Field.SLOTS, sum())
                .put(Field.START_AT, first(String.format("$%s", Field.START_AT)));

        return group(group);
    }


    private JsonObject studentGroupById() {
        JsonObject id = new JsonObject()
                .put(Field.USER, String.format("%s.%s", Field.$_ID, Field.USER))
                .put(Field.NAME, String.format("%s.%s", Field.$_ID, Field.NAME))
                .put(Field.CLASS_NAME, String.format("%s.%s", Field.$_ID, Field.CLASS_NAME))
                .put(Field.MONTH, String.format("%s.%s", Field.$_ID, Field.MONTH))
                .put(Field.YEAR, String.format("%s.%s", Field.$_ID, Field.YEAR));

        JsonObject group = id(id)
                .put(Field.COUNT, sum())
                .put(Field.START_AT, first(String.format("$%s", Field.START_AT)))
                .put(Field.SLOTS, sum(String.format("$%s", Field.SLOTS)));

        return group(group);
    }

    private JsonObject studentGroup() {
        JsonObject id = new JsonObject()
                .put("user", "$user")
                .put("name", "$name")
                .put("class_name", "$class_name")
                .put("month", month())
                .put("year", year());

        JsonObject group = id(id)
                .put("count", sum())
                .put("start_at", first("$start_at"))
                .put("slots", sum());

        return group(group);
    }

    private JsonObject audienceProject() {
        JsonObject project = new JsonObject()
                .put("_id", 0)
                .put("month", dateToString("$start_at", "%Y-%m"))
                .put("class_name", "$_id.class_name")
                .put("count", sum("$count"))
                .put("slots", sum("$slots"));

        return new JsonObject()
                .put("$project", project);

    }

    private JsonObject studentProject() {
        JsonObject project = new JsonObject()
                .put("_id", 0)
                .put("month", dateToString("$start_at", "%Y-%m"))
                .put("class_name", "$_id.class_name")
                .put("user", "$_id.user")
                .put("name", "$_id.name")
                .put("count", sum("$count"))
                .put("slots", sum("$slots"));

        return new JsonObject()
                .put("$project", project);

    }

    private JsonArray audienceGroupAbsences() {
        JsonArray groups;
        switch (recoveryMethod) {
            case DAY:
                groups = audienceGroupByCount(audienceIdByDay());
                break;
            case HALF_DAY:
                groups = audienceGroupByCount(audienceIdByHalfDay());
                break;
            default:
                groups = new JsonArray().add(audienceGroup());
        }
        return groups;
    }

    private JsonArray studentGroupAbsences() {
        JsonArray groups;
        switch (recoveryMethod) {
            case DAY:
                groups = studentGroupByCount(studentIdByDay());
                break;
            case HALF_DAY:
                groups = studentGroupByCount(studentIdByHalfDay());
                break;
            default:
                groups = new JsonArray().add(studentGroup());
        }
        return groups;
    }

    private JsonObject audienceIdByDay() {
        return new JsonObject()
                .put("class_name", "$class_name")
                .put("day", day())
                .put("month", month())
                .put("year", year());
    }

    private JsonObject studentIdByDay() {
        return audienceIdByDay()
                .put("user", "$user")
                .put("name", "$name");
    }

    private JsonObject audienceIdByHalfDay() {
        return new JsonObject()
                .put("name", "$name")
                .put("class_name", "$class_name")
                .put("day", day())
                .put("month", month())
                .put("year", year())
                .put("is_before_halfday", isBeforeHalfday());
    }

    private JsonObject studentIdByHalfDay() {
        return audienceIdByHalfDay()
                .put("user", "$user")
                .put("name", "$name");
    }

    private JsonArray audienceGroupByCount(JsonObject id) {
        JsonArray groups = new JsonArray();
        JsonObject group = id(id)
                .put("start_at", first("$start_at"))
                .put("slots", sum());
        groups.add(group(group));

        id = new JsonObject()
                .put("month", "$_id.month")
                .put("year", "$_id.year")
                .put("class_name", "$_id.class_name");

        group = id(id)
                .put("start_at", first("$start_at"))
                .put("count", sum())
                .put("slots", sum("$slots"));
        groups.add(group(group));

        return groups;
    }

    private JsonArray studentGroupByCount(JsonObject id) {
        JsonArray groups = new JsonArray();
        JsonObject group = id(id)
                .put("start_at", first("$start_at"))
                .put("slots", sum());
        groups.add(group(group));

        id = new JsonObject()
                .put("month", "$_id.month")
                .put("year", "$_id.year")
                .put("class_name", "$_id.class_name")
                .put("user", "$_id.user")
                .put("name", "$_id.name");

        group = id(id)
                .put("start_at", first("$start_at"))
                .put("count", sum())
                .put("slots", sum("$slots"));
        groups.add(group(group));

        return groups;
    }

    private JsonObject isBeforeHalfday() {
        JsonArray lt = new JsonArray()
                .add(dateToString("$start_at", "%H:%M:%S"))
                .add(halfDay);

        return new JsonObject()
                .put("$lt", lt);
    }

    /*
    UTILITIES
     */

    private List<String> typesIn(Predicate<String> inTypes) {
        return this.filter().types().stream()
                .filter(inTypes)
                .collect(Collectors.toList());
    }

    private JsonObject dateToString(String dateParam, String format) {
        JsonObject dateToString = new JsonObject()
                .put("format", format)
                .put("date", dateParam);

        return new JsonObject().put("$dateToString", dateToString);
    }

    private JsonObject day() {
        return new JsonObject()
                .put("$dayOfMonth", "$start_at");
    }

    private JsonObject month() {
        return new JsonObject()
                .put("$month", "$start_at");
    }

    private JsonObject year() {
        return new JsonObject()
                .put("$year", "$start_at");
    }

    private JsonObject first(String value) {
        return new JsonObject()
                .put("$first", value);
    }

    private JsonObject sum(String value) {
        return new JsonObject().put("$sum", value);
    }

    private JsonObject sum() {
        return new JsonObject().put("$sum", 1);
    }

    private JsonObject id(JsonObject value) {
        return new JsonObject()
                .put("_id", value);
    }

    private JsonObject group(JsonObject value) {
        return new JsonObject()
                .put("$group", value);
    }
}
