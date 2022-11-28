package fr.openent.statistics_presences.bean.monthly;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.core.constants.MongoField;
import fr.openent.statistics_presences.bean.Audience;
import fr.openent.statistics_presences.bean.User;
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
        JsonObject dateString = new JsonObject().put(Field.DATESTRING, MongoField.$ + Field.START_DATE);

        JsonObject dateFromString = new JsonObject()
                .put(MongoField.$DATEFROMSTRING, dateString);

        JsonObject statAtField = new JsonObject()
                .put(Field.START_AT, dateFromString);

        return new JsonObject()
                .put(MongoField.$ADDFIELDS, statAtField);
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
                .put(Field.STRUCTURE, this.filter.structure())
                .put(MongoField.$OR, filterType(types))
                .put(Field.START_DATE, this.startDateFilter())
                .put(Field.END_DATE, this.endDateFilter());

        if (!this.filter.audiences().isEmpty() && userIdentifiers.isEmpty()) {
            matcher.put(Field.AUDIENCES, audienceFilter());
        }

        if (!userIdentifiers.isEmpty()) {
            matcher.put(Field.USER, usersFilter(userIdentifiers));
        }

        return new JsonObject()
                .put(MongoField.$MATCH, matcher);
    }

    private JsonArray filterType(List<String> types) {
        List<String> typesToUse = types != null ? types : this.filter().types();
        JsonArray filters = new JsonArray();
        for (String type : typesToUse) {
            JsonObject filterType = new JsonObject()
                    .put(Field.TYPE, type);

            EventType eventType = EventType.valueOf(type);
            switch (eventType) {
                case UNREGULARIZED:
                case REGULARIZED:
                    JsonObject inFilterReasons = new JsonObject()
                            .put(MongoField.$IN, this.filter().reasons());
                    filterType.put(Field.REASON, inFilterReasons);
                    break;
                case SANCTION:
                    JsonObject inFilterSanctionTypes = new JsonObject()
                            .put(MongoField.$IN, this.filter.sanctionTypes());
                    filterType.put(Field.PUNISHMENT_TYPE, inFilterSanctionTypes);
                    break;
                case PUNISHMENT:
                    JsonObject inFilterPunishmentTypes = new JsonObject()
                            .put(MongoField.$IN, this.filter.punishmentTypes());
                    filterType.put(Field.PUNISHMENT_TYPE, inFilterPunishmentTypes);
                    break;
                case LATENESS:
                    List<Integer> list = this.filter().reasons();
                    if (Boolean.TRUE.equals(this.filter().getNoLatenessReason())) {
                        list.add(null);
                    }
                    JsonObject inFilterLatenessReasons = new JsonObject()
                            .put(MongoField.$IN, list);
                    filterType.put(Field.REASON, inFilterLatenessReasons );
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
                .put(MongoField.$IN, this.filter().audiences());
    }

    private JsonObject usersFilter(List<String> users) {
        return new JsonObject()
                .put(MongoField.$IN, users);
    }

    private JsonObject startDateFilter() {
        return new JsonObject()
                .put(MongoField.$GTE, this.filter().start());
    }

    private JsonObject endDateFilter() {
        return new JsonObject()
                .put(MongoField.$LTE, this.filter().end());
    }

    private JsonObject addCountIdField() {
        JsonObject groupedPunishmentIdExistsQuery = new JsonObject().put(MongoField.$GTE,
                new JsonArray().add(MongoField.$ + Field.GROUPED_PUNISHMENT_ID).addNull()
        );

        JsonObject cond = new JsonObject()
                .put(MongoField.$COND, new JsonArray()
                        .add(groupedPunishmentIdExistsQuery)
                        .add(MongoField.$ + Field.GROUPED_PUNISHMENT_ID)
                        .add(MongoField.$ + Field._ID)
                );

        return new JsonObject().put(MongoField.$ADDFIELDS, new JsonObject().put(COUNTID, cond));
    }

    private JsonObject audienceGroupByCountId() {
        JsonObject id = new JsonObject()
                .put(Field.CLASS_NAME, String.format("$%s", Field.CLASS_NAME))
                .put(Field.MONTH, month())
                .put(Field.YEAR, year())
                .put(COUNTID, String.format("$%s", COUNTID));

        JsonObject group = id(id)
                .put(Field.SLOTS, sum(atLeastOne(new JsonObject().put(MongoField.$SIZE, MongoField.$ + Field.SLOTS))))
                .put(Field.START_AT, first(String.format("$%s", Field.START_AT)));

        return group(group);
    }

    private JsonObject audienceGroupById() {
        JsonObject id = new JsonObject()
                .put(Field.CLASS_NAME, String.format("%s.%s", MongoField.$ + Field._ID, Field.CLASS_NAME))
                .put(Field.MONTH, String.format("%s.%s", MongoField.$ + Field._ID, Field.MONTH))
                .put(Field.YEAR, String.format("%s.%s", MongoField.$ + Field._ID, Field.YEAR));

        JsonObject group = id(id)
                .put(Field.COUNT, sum(MongoField.$ + Field.SLOTS))
                .put(Field.START_AT, first(String.format("$%s", Field.START_AT)))
                .put(Field.SLOTS, sum(MongoField.$ + Field.SLOTS));

        return group(group);
    }

    private JsonObject audienceGroup() {
        JsonObject id = new JsonObject()
                .put(Field.CLASS_NAME, MongoField.$ + Field.CLASS_NAME)
                .put(Field.MONTH, month())
                .put(Field.YEAR, year());

        JsonObject group = id(id)
                .put(Field.COUNT, sum(new JsonObject().put(MongoField.$SIZE, MongoField.$ + Field.SLOTS)))
                .put(Field.START_AT, first(MongoField.$ + Field.START_AT))
                .put(Field.SLOTS, sum(new JsonObject().put(MongoField.$SIZE, MongoField.$ + Field.SLOTS)));

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
                .put(Field.SLOTS, sum(atLeastOne(new JsonObject().put(MongoField.$SIZE, MongoField.$ + Field.SLOTS))))
                .put(Field.START_AT, first(String.format("$%s", Field.START_AT)));

        return group(group);
    }


    private JsonObject studentGroupById() {
        JsonObject id = new JsonObject()
                .put(Field.USER, String.format("%s.%s", MongoField.$ + Field._ID, Field.USER))
                .put(Field.NAME, String.format("%s.%s", MongoField.$ + Field._ID, Field.NAME))
                .put(Field.CLASS_NAME, String.format("%s.%s", MongoField.$ + Field._ID, Field.CLASS_NAME))
                .put(Field.MONTH, String.format("%s.%s", MongoField.$ + Field._ID, Field.MONTH))
                .put(Field.YEAR, String.format("%s.%s", MongoField.$ + Field._ID, Field.YEAR));

        JsonObject group = id(id)
                .put(Field.COUNT, sum(String.format("$%s", Field.SLOTS)))
                .put(Field.START_AT, first(String.format("$%s", Field.START_AT)))
                .put(Field.SLOTS, sum(String.format("$%s", Field.SLOTS)));

        return group(group);
    }

    private JsonObject studentGroup() {
        JsonObject id = new JsonObject()
                .put(Field.USER, MongoField.$ + Field.USER)
                .put(Field.NAME, MongoField.$ + Field.NAME)
                .put(Field.CLASS_NAME, MongoField.$ + Field.CLASS_NAME)
                .put(Field.MONTH, month())
                .put(Field.YEAR, year());

        JsonObject group = id(id)
                .put(Field.COUNT, sum(new JsonObject().put(MongoField.$SIZE, MongoField.$ + Field.SLOTS)))
                .put(Field.START_AT, first(MongoField.$ + Field.START_AT))
                .put(Field.SLOTS, sum(new JsonObject().put(MongoField.$SIZE, MongoField.$ + Field.SLOTS)));

        return group(group);
    }

    private JsonObject audienceProject() {
        JsonObject project = new JsonObject()
                .put(Field._ID, 0)
                .put(Field.MONTH, dateToString(MongoField.$ + Field.START_AT, "%Y-%m"))
                .put(Field.CLASS_NAME, "$_id.class_name")
                .put(Field.COUNT, sum(MongoField.$ + Field.COUNT))
                .put(Field.SLOTS, sum(MongoField.$ + Field.SLOTS));

        return new JsonObject()
                .put(MongoField.$PROJECT, project);

    }

    private JsonObject studentProject() {
        JsonObject project = new JsonObject()
                .put(Field._ID, 0)
                .put(Field.MONTH, dateToString(MongoField.$ + Field.START_AT, "%Y-%m"))
                .put(Field.CLASS_NAME, "$_id.class_name")
                .put(Field.USER, "$_id.user")
                .put(Field.NAME, "$_id.name")
                .put(Field.COUNT, sum(MongoField.$ + Field.COUNT))
                .put(Field.SLOTS, sum(MongoField.$ + Field.SLOTS));

        return new JsonObject()
                .put(MongoField.$PROJECT, project);

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
                .put("user", "$user")
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
                .add(dateToString(MongoField.$ + Field.START_AT, "%H:%M:%S"))
                .add(halfDay);

        return new JsonObject()
                .put(MongoField.$LT, lt);
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
                .put(Field.FORMAT, format)
                .put(Field.DATE, dateParam);

        return new JsonObject().put(MongoField.$DATETOSTRING, dateToString);
    }

    private JsonObject day() {
        return new JsonObject()
                .put(MongoField.$DAYOFMONTH, MongoField.$ + Field.START_AT);
    }

    private JsonObject month() {
        return new JsonObject()
                .put(MongoField.$MONTH, MongoField.$ + Field.START_AT);
    }

    private JsonObject year() {
        return new JsonObject()
                .put(MongoField.$YEAR, MongoField.$ + Field.START_AT);
    }

    private JsonObject first(String value) {
        return new JsonObject()
                .put(MongoField.$FIRST, value);
    }

    private JsonObject sum(String value) {
        return new JsonObject().put(MongoField.$SUM, value);
    }

    private JsonObject sum() {
        return new JsonObject().put(MongoField.$SUM, 1);
    }

    private JsonObject sum(JsonObject value) {
        return new JsonObject().put(MongoField.$SUM, value);
    }

    private JsonObject id(JsonObject value) {
        return new JsonObject()
                .put(Field._ID, value);
    }

    private JsonObject group(JsonObject value) {
        return new JsonObject()
                .put(MongoField.$GROUP, value);
    }

    private JsonObject atLeastOne(JsonObject value) {
        return new JsonObject().put(MongoField.$MAX, new JsonArray().add(value).add(1));
    }
}
