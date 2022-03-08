package fr.openent.statistics_presences.bean.weekly;

import com.mongodb.QueryBuilder;
import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.indicator.impl.Monthly;
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
                .withGroup(groupBySlot())
                .withProjection(project());

        return eventsAggregation.getCommand();

        /*return new JsonArray()
                .add(addDateField(String.format("$%s", Field.START_AT)))
                .add(match())
                .add(groupBySlotAndAudience())
                .add(audienceGroupByCountId())
                .add(audienceGroupById())
                .add(audienceProject());*/
    }

    public JsonObject countStudentsBySlotsCommand() {
        AggregationsBuilder eventsAggregation = AggregationsBuilder.startWithCollection(StatisticsPresences.COLLECTION);
        eventsAggregation
                .withAllowDiskUse(true)
                .withMatch(matchCountStudents())
                .withGroup(groupStudentCountBySlot())
                .withProjection(project());

        return eventsAggregation.getCommand();
    }
    /**
     * add a start_at field at the beginning of the pipeline to have start_date in mongo date format (and so handle it)
     *
     * @return field start_at
     */
    /*private JsonObject addDateField(String dateField) {
        return new JsonObject()
                .put(Field.DATE, dateToString(dateFromString(dateField), "%Y-%m-%d"));
    }*/
    private QueryBuilder matchEvents() {
        QueryBuilder matcher = QueryBuilder.start(Field.STRUCTURE).is(this.filter.structure())
                .and(Field.INDICATOR).is(Monthly.class.getName())
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
                .and(Field.START_AT).lessThan(this.filter.end())
                .and(Field.END_AT).greaterThan(this.filter.start());

        if (!this.filter.audiences().isEmpty()) {
            matcher.put(Field.AUDIENCE_ID).in(this.filter().audiences());
        }

        return filterType(matcher);
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
                case SANCTION:
                    filterType.and(Field.PUNISHMENT_TYPE).in(this.filter().sanctionTypes());
                    break;
                case PUNISHMENT:
                    filterType.and(Field.PUNISHMENT_TYPE).in(this.filter().punishmentTypes());
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
                .put(Field.SLOT_ID, String.format("$%s", Field.SLOT_ID));

        JsonObject group = id(id)
                .put(Field.COUNT, sum());

        return group(group);
    }

    private JsonObject groupStudentCountBySlot() {
        JsonObject id = new JsonObject()
                .put(Field.SLOT_ID, String.format("$%s", Field.SLOT_ID));

        JsonObject group = id(id)
                .put(Field.COUNT, this.filter().userId() != null ? sum() : sum(Field.STUDENT_COUNT));

        return group(group);
    }


    private JsonObject project() {
        return new JsonObject()
                .put(Field._ID, 0)
                .put(Field.SLOT_ID, String.format("$%s.%s", Field._ID, Field.SLOT_ID))
                .put(Field.COUNT, sum(String.format("$%s", Field.COUNT)));

    }

    /*
    UTILITIES
     */

    private JsonObject dateToString(String dateParam, String format) {
        JsonObject dateToString = new JsonObject()
                .put("format", format)
                .put("date", dateParam);

        return new JsonObject().put("$dateToString", dateToString);
    }

    private JsonObject dateToString(JsonObject dateParam, String format) {
        JsonObject dateToString = new JsonObject()
                .put("format", format)
                .put("date", dateParam);

        return new JsonObject().put("$dateToString", dateToString);
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

    private JsonObject group(JsonObject value) {
        return new JsonObject()
                .put(String.format("$%s", Field.GROUP), value);
    }
}
