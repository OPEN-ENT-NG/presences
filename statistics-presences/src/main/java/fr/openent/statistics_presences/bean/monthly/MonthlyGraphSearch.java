package fr.openent.statistics_presences.bean.monthly;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.core.constants.MongoField;
import fr.openent.statistics_presences.bean.User;
import fr.openent.statistics_presences.model.StatisticsFilter;
import fr.openent.statistics_presences.utils.EventType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MonthlyGraphSearch {
    private static final String HALF_DAY = "HALF_DAY";
    private static final String DAY = "DAY";
    private final StatisticsFilter filter;
    List<String> totalAbsenceTypes = Arrays.asList(EventType.NO_REASON.name(), EventType.UNREGULARIZED.name(), EventType.REGULARIZED.name());
    private List<User> users = new LinkedList<>();
    private Map<String, List<Month>> statisticsByType;
    private String recoveryMethod;
    private String halfDay;

    public MonthlyGraphSearch(StatisticsFilter filter) {
        this.filter = filter;
    }

    public MonthlyGraphSearch(MonthlySearch monthlySearch) {
        this.filter = monthlySearch.filter();
        this.recoveryMethod = monthlySearch.recovery();
        this.halfDay = monthlySearch.halfDay();
    }

    public StatisticsFilter filter() {
        return this.filter;
    }

    public MonthlyGraphSearch setUsers(List<User> users) {
        this.users = users;
        return this;
    }

    public List<User> users() {
        return this.users;
    }


    public MonthlyGraphSearch setStatistics(Map<String, List<Month>> statistics) {
        this.statisticsByType = statistics;
        return this;
    }

    public Map<String, List<Month>> statistics() {
        return this.statisticsByType;
    }

    public MonthlyGraphSearch setRecoveryMethod(String recoveryMethod) {
        this.recoveryMethod = recoveryMethod;
        return this;
    }

    public MonthlyGraphSearch setHalfDay(String halfDay) {
        this.halfDay = halfDay;
        return this;
    }

    public JsonArray searchGroupedBasicEvenTypePipeline() {
        List<String> types = typesIn(type -> !totalAbsenceTypes.contains(type));
        if (types == null || types.isEmpty())
            return new JsonArray();

        return new JsonArray()
                .add(addStartAtField())
                .add(match(types))
                .add(groupEventType())
                .add(project());
    }

    public JsonArray searchGroupedAbsencesPipeline() {
        List<String> types = typesIn(type -> totalAbsenceTypes.contains(type));
        if (types == null || types.isEmpty())
            return new JsonArray();

        return new JsonArray()
                .add(addStartAtField())
                .add(match(types))
                .addAll(groupAbsences())
                .add(project());
    }

    /**
     * add a start_at field at the beginning of the pipeline to have start_date in mongo date format (and so handle it)
     *
     * @return field start_at
     */
    private JsonObject addStartAtField() {
        JsonObject dateString = new JsonObject().put("dateString", "$start_date");

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
            filters.add(new JsonObject().put(Field.TYPE, type));
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


    private JsonArray groupAbsences() {
        JsonArray groups;
        switch (recoveryMethod) {
            case DAY:
                groups = groupByCount(idByDay());
                break;
            case HALF_DAY:
                groups = groupByCount(idByHalfDay());
                break;
            default:
                groups = new JsonArray().add(idByHour());
        }
        return groups;
    }

    private JsonArray groupByCount(JsonObject id) {
        JsonArray groups = new JsonArray();
        JsonObject group = id(id)
                .put("start_at", first("$start_at"));
        groups.add(group(group));

        id = new JsonObject()
                .put("type", "$_id.type")
                .put("month", "$_id.month")
                .put("year", "$_id.year");

        group = id(id)
                .put("start_at", first("$start_at"))
                .put("count", sum());
        groups.add(group(group));

        return groups;
    }

    private JsonObject idByDay() {
        return new JsonObject()
                .put("name", "$name")
                .put("type", "$type")
                .put("day", day())
                .put("month", month())
                .put("year", year());
    }

    private JsonObject idByHalfDay() {
        return new JsonObject()
                .put("name", "$name")
                .put("type", "$type")
                .put("day", day())
                .put("month", month())
                .put("year", year())
                .put("is_before_halfday", isBeforeHalfday());
    }

    private JsonObject idByHour() {
        JsonObject id = new JsonObject()
                .put("type", "$type")
                .put("month", month())
                .put("year", year());

        JsonObject group = id(id)
                .put("count", sum())
                .put("start_at", first("$start_at"))
                .put("slots", sum());

        return group(group);
    }

    private JsonObject groupEventType() {
        JsonObject id = new JsonObject()
                .put("type", "$type")
                .put("month", month())
                .put("year", year());

        JsonObject group = id(id)
                .put("count", sum())
                .put("start_at", first("$start_at"))
                .put("slots", sum());

        return group(group);
    }

    private JsonObject project() {
        JsonObject project = new JsonObject()
                .put(Field._ID, 0)
                .put(Field.MONTH, dateToString(MongoField.$ + Field.START_AT, "%Y-%m"))
                .put(Field.TYPE, "$_id.type")
                .put(Field.COUNT, 1);

        return new JsonObject()
                .put(MongoField.$PROJECT, project);
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

    private JsonObject id(JsonObject value) {
        return new JsonObject()
                .put(Field._ID, value);
    }

    private JsonObject group(JsonObject value) {
        return new JsonObject()
                .put(MongoField.$GROUP, value);
    }
}
