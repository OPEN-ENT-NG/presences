package fr.openent.statistics_presences.bean.weekly;

import com.mongodb.QueryBuilder;
import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.indicator.impl.Weekly;
import fr.openent.statistics_presences.model.StatisticsFilter;
import fr.openent.statistics_presences.utils.EventType;
import fr.wseduc.mongodb.AggregationsBuilder;
import io.vertx.core.json.JsonObject;

public class WeeklySearch {
    private final StatisticsFilter filter;

    public WeeklySearch(StatisticsFilter filter) {
        this.filter = filter;
    }

    public StatisticsFilter filter() {
        return this.filter;
    }

    public JsonObject countEventTypedBySlotsCommand() {
        AggregationsBuilder eventsAggregation = AggregationsBuilder.startWithCollection(StatisticsPresences.COLLECTION);
        eventsAggregation
                .withAllowDiskUse(true)
                .withMatch(matchEvents())
                .withAddFields(addDayOfWeekField(String.format("$%s", Field.START_DATE)))
                .withGroup(groupBySlot())
                .withProjection(project());

        return eventsAggregation.getCommand();
    }

    public JsonObject countStudentsBySlotsCommand() {
        AggregationsBuilder eventsAggregation = AggregationsBuilder.startWithCollection(StatisticsPresences.WEEKLY_AUDIENCES_COLLECTION);
        eventsAggregation
                .withAllowDiskUse(true)
                .withMatch(matchCountStudents())
                .withAddFields(addDayOfWeekField(String.format("$%s.%s", Field._ID, Field.START_AT)))
                .withGroup(groupStudentCountBySlot())
                .withProjection(project());

        return eventsAggregation.getCommand();
    }
    /**
     * add a start_at field at the beginning of the pipeline to have start_date in mongo date format (and so handle it)
     *
     * @return field start_at
     */
    private JsonObject addDayOfWeekField(String dateField) {
        return new JsonObject()
                .put(Field.DAYOFWEEK, isoDayOfWeek(dateFromString(dateField)));
    }

    private QueryBuilder matchEvents() {
        QueryBuilder matcher = QueryBuilder.start(Field.STRUCTURE).is(this.filter.structure())
                .and(Field.INDICATOR).is(Weekly.class.getName())
                .and(Field.START_DATE).lessThan(this.filter.end())
                .and(Field.END_DATE).greaterThan(this.filter.start());

        if (!this.filter.audiences().isEmpty() && this.filter().userId() == null) {
            matcher.put(Field.AUDIENCES).in(this.filter().audiences());
        }

        if (this.filter().userId() != null) {
            matcher.put(Field.USER).is(this.filter.userId());
        }

        return filterType(matcher);
    }

    private QueryBuilder matchCountStudents() {
        QueryBuilder matcher = QueryBuilder.start(Field.STRUCTURE_ID).is(this.filter.structure())
                .and(String.format("%s.%s", Field._ID, Field.START_AT)).lessThan(this.filter.end())
                .and(String.format("%s.%s", Field._ID, Field.END_AT)).greaterThan(this.filter.start());

        if (!this.filter.audiences().isEmpty()) {
            matcher.put(String.format("%s.%s", Field._ID, Field.AUDIENCE_ID)).in(this.filter().audiences());
        }

        return matcher;
    }

    private QueryBuilder filterType(QueryBuilder query) {
        for (String type : this.filter().types()) {
            QueryBuilder filterType = QueryBuilder.start(Field.TYPE).is(type);
            EventType eventType = EventType.valueOf(type);

            switch (eventType) {
                case UNREGULARIZED:
                case REGULARIZED:
                    filterType.and(Field.REASON).in(this.filter().reasons());
                    break;
                default:
                    break;
            }
            query.or(filterType.get());
        }

        return query;
    }

    private JsonObject groupBySlot() {
        JsonObject id = new JsonObject()
                .put(Field.SLOT_ID, String.format("$%s", Field.SLOT_ID))
                .put(Field.DAYOFWEEK, String.format("$%s", Field.DAYOFWEEK));

        return id(id)
                .put(Field.COUNT, sum());
    }

    private JsonObject groupStudentCountBySlot() {
        JsonObject id = new JsonObject()
                .put(Field.SLOT_ID, String.format("$%s", Field.SLOT_ID))
                .put(Field.DAYOFWEEK, String.format("$%s", Field.DAYOFWEEK));

        return id(id)
                .put(Field.COUNT, this.filter().userId() != null ? sum() : sum(String.format("$%s", Field.STUDENT_COUNT)));
    }


    private JsonObject project() {
        return new JsonObject()
                .put(Field._ID, 0)
                .put(Field.SLOT_ID, String.format("$%s.%s", Field._ID, Field.SLOT_ID))
                .put(Field.DAYOFWEEK, String.format("$%s.%s", Field._ID, Field.DAYOFWEEK))
                .put(Field.COUNT, sum(String.format("$%s", Field.COUNT)));

    }

    /*
    UTILITIES
     */

    private JsonObject isoDayOfWeek(JsonObject dateParam) {
        return new JsonObject().put(String.format("$%s", Field.ISODAYOFWEEK), dateParam);
    }

    private JsonObject dateFromString(String dateParam) {

        JsonObject dateString = new JsonObject().put(Field.DATESTRING, dateParam);

        return new JsonObject()
                .put(String.format("$%s", Field.DATEFROMSTRING), dateString);
    }

    private JsonObject sum(String value) {

        return new JsonObject().put(String.format("$%s", Field.SUM), value);
    }

    private JsonObject sum() {
        return new JsonObject().put(String.format("$%s", Field.SUM), 1);
    }

    private JsonObject id(JsonObject value) {
        return new JsonObject()
                .put(Field._ID, value);
    }
}
