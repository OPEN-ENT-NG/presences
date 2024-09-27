package fr.openent.statistics_presences.bean.weekly;

import com.mongodb.client.model.Filters;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.core.constants.MongoField;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.model.StatisticsFilter;
import fr.openent.statistics_presences.utils.EventType;
import fr.wseduc.mongodb.AggregationsBuilder;
import io.vertx.core.json.JsonObject;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

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
                .withUnwind(String.format("$%s", Field.SLOTS))
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
                .withProjection(projectCountStudents());

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

    private Bson matchEvents() {
        List<Bson> filters = new ArrayList<>();

        filters.add(Filters.eq(Field.STRUCTURE, this.filter.structure()));
        filters.add(Filters.lt(Field.START_DATE, this.filter.end()));
        filters.add(Filters.gt(Field.END_DATE, this.filter.start()));

        if (!this.filter.audiences().isEmpty() && this.filter.userId() == null) {
            filters.add(Filters.in(Field.AUDIENCES, this.filter.audiences()));
        }

        if (this.filter.userId() != null) {
            filters.add(Filters.eq(Field.USER, this.filter.userId()));
        }

        return Filters.and(filters);
    }

    private Bson matchCountStudents() {
        List<Bson> filters = new ArrayList<>();

        filters.add(Filters.eq(Field.STRUCTURE_ID, this.filter.structure()));
        filters.add(Filters.lt(String.format("%s.%s", Field._ID, Field.START_AT), this.filter.end()));
        filters.add(Filters.gt(String.format("%s.%s", Field._ID, Field.END_AT), this.filter.start()));

        if (!this.filter.audiences().isEmpty()) {
            filters.add(Filters.in(String.format("%s.%s", Field._ID, Field.AUDIENCE_ID), this.filter.audiences()));
        }

        return Filters.and(filters);
    }


    private Bson filterType(Bson query) {
        List<Bson> orFilters = new ArrayList<>();

        for (String type : this.filter().types()) {
            List<Bson> subFilters = new ArrayList<>();

            // Filtre sur le type d'événement
            subFilters.add(Filters.eq(Field.TYPE, type));
            EventType eventType = EventType.valueOf(type);

            switch (eventType) {
                case UNREGULARIZED:
                case REGULARIZED:
                    // Ajoute un filtre sur les raisons
                    subFilters.add(Filters.in(Field.REASON, this.filter().reasons()));
                    break;
                case LATENESS:
                    // Gère les retards avec ou sans raison
                    List<Integer> reasonsList = new ArrayList<>(this.filter().reasons());
                    if (Boolean.TRUE.equals(this.filter().getNoLatenessReason())) {
                        reasonsList.add(null);
                    }
                    subFilters.add(Filters.in(Field.REASON, reasonsList));
                    break;
                default:
                    break;
            }

            // Combine les sous-filtres avec un opérateur AND pour ce type
            Bson andFilter = Filters.and(subFilters);
            // Ajoute le filtre combiné à la liste des filtres OR
            orFilters.add(andFilter);
        }

        // Combine tous les filtres de type avec un opérateur OR
        Bson typeFilter;
        if (!orFilters.isEmpty()) {
            typeFilter = Filters.or(orFilters);
        } else {
            // Si aucun type n'est spécifié, ne pas filtrer sur le type
            typeFilter = new Document();
        }

        // Combine le filtre existant avec le filtre de type avec un opérateur AND
        return Filters.and(query, typeFilter);
    }

    private JsonObject groupBySlot() {
        JsonObject id = new JsonObject()
                .put(Field.SLOT, String.format("$%s", Field.SLOTS))
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
                .put(Field.SLOT_ID, String.format("$%s.%s.%s", Field._ID, Field.SLOT, Field.ID))
                .put(Field.DAYOFWEEK, String.format("$%s.%s", Field._ID, Field.DAYOFWEEK))
                .put(Field.COUNT, sum(String.format("$%s", Field.COUNT)));

    }

    private JsonObject projectCountStudents() {
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
                .put(MongoField.$DATEFROMSTRING, dateString);
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
