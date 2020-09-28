package fr.openent.statistics_presences.bean.global;

import fr.openent.statistics_presences.bean.Audience;
import fr.openent.statistics_presences.bean.User;
import fr.openent.statistics_presences.filter.Filter;
import fr.openent.statistics_presences.indicator.impl.Global;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

public class GlobalSearch {
    List<String> totalAbsenceTypes = Arrays.asList("UNJUSTIFIED_ABSENCE", "JUSTIFIED_UNREGULARIZED_ABSENCE", "REGULARIZED_ABSENCE");

    private Filter filter;
    private List<Audience> audiences = new ArrayList<>();
    private List<User> users = new LinkedList<>();
    private Map<String, List<JsonObject>> statisticsMapped;
    private Map<String, Number> totalAbsMap;

    public GlobalSearch(Filter filter) {
        this.filter = filter;
    }

    public Filter filter() {
        return this.filter;
    }

    public GlobalSearch setAudiences(List<Audience> audiences) {
        this.audiences = audiences;
        return this;
    }

    public List<Audience> audiences() {
        return this.audiences;
    }

    public GlobalSearch setUsers(List<User> users) {
        this.users = users;
        return this;
    }

    public Boolean containsAbsence() {
        return this.filter().types().contains("UNJUSTIFIED_ABSENCE") || this.filter().types().contains("JUSTIFIED_UNREGULARIZED_ABSENCE") || this.filter().types().contains("REGULARIZED_ABSENCE");
    }

    public List<User> users() {
        return this.users;
    }

    public GlobalSearch setTotalAbsMap(Map<String, Number> map) {
        this.totalAbsMap = map;
        return this;
    }

    public Number totalAbs(String user) {
        return this.totalAbsMap.get(user);
    }

    public GlobalSearch setStatistics(Map<String, List<JsonObject>> statistics) {
        this.statisticsMapped = statistics;
        return this;
    }

    public Map<String, List<JsonObject>> statistics() {
        return this.statisticsMapped;
    }

    public JsonArray prefetchUserPipeline() {
        JsonArray pipeline = new JsonArray()
                .add(match())
                .add(group())
                .add(fromToMatcher())
                .add(prefetchDistinct())
                .add(prefetchUserProject())
                .add(prefetchUserSort());

        if (this.filter().page() != null) {
            pipeline.add(skip())
                    .add(limit());
        }

        return pipeline;
    }

    private JsonObject prefetchUserProject() {
        JsonObject project = new JsonObject()
                .put("_id", "$_id.user")
                .put("name", "$_id.name")
                .put("class_name", "$_id.class_name");

        return new JsonObject()
                .put("$project", project);
    }

    private JsonObject prefetchUserSort() {
        JsonObject sort = new JsonObject()
                .put("class_name", 1)
                .put("name", 1);

        return new JsonObject()
                .put("$sort", sort);
    }

    private JsonObject prefetchDistinct() {
        JsonObject id = new JsonObject()
                .put("user", "$_id.user")
                .put("name", "$_id.name")
                .put("class_name", "$_id.class_name");

        JsonObject group = new JsonObject()
                .put("_id", id);

        return new JsonObject()
                .put("$group", group);
    }

    public JsonArray totalAbsenceGlobalPipeline() {
        JsonArray pipeline = new JsonArray()
                .add(match(totalAbsenceTypes))
                .add(group());

        if (this.filter().from() != null || this.filter().to() != null) {
            pipeline.add(fromToMatcher());
        }

        pipeline.add(totalGlobalAbsenceGroup())
                .add(projectCount());

        return pipeline;
    }

    public JsonArray totalAbsenceUserPipeline() {
        JsonArray pipeline = new JsonArray()
                .add(match(totalAbsenceTypes))
                .add(group());

        if (this.filter().from() != null || this.filter().to() != null) {
            pipeline.add(fromToMatcher());
        }

        pipeline.add(countTotalUser())
                .add(projectTotalUser());

        return pipeline;
    }

    public JsonArray countPipeline() {
        JsonArray pipeline = new JsonArray()
                .add(match())
                .add(group());

        if (this.filter().from() != null || this.filter().to() != null) {
            pipeline.add(fromToMatcher());
        }

        pipeline.add(countGroup())
                .add(projectCount());

        return pipeline;
    }

    private JsonObject totalGlobalAbsenceGroup() {
        JsonObject count = new JsonObject()
                .put("$sum", "$count");

        JsonObject group = new JsonObject()
                .put("_id", "ABSENCE_TOTAL")
                .put("count", count);

        return new JsonObject()
                .put("$group", group);
    }

    private JsonObject countTotalUser() {
        JsonObject group = new JsonObject()
                .put("_id", "$_id.user");

        JsonObject count = new JsonObject()
                .put("$sum", "$count");
        group.put("count", count);

        return new JsonObject().put("$group", group);
    }

    private JsonObject projectTotalUser() {
        JsonObject project = new JsonObject()
                .put("_id", 0)
                .put("user", "$_id")
                .put("count", "$count");

        return new JsonObject().put("$project", project);
    }

    private JsonObject countGroup() {
        JsonObject group = new JsonObject()
                .put("_id", "$_id.type");

        JsonObject count = new JsonObject()
                .put("$sum", "$count");
        group.put("count", count);

        return new JsonObject().put("$group", group);
    }

    private JsonObject projectCount() {
        JsonObject project = new JsonObject()
                .put("_id", 0)
                .put("type", "$_id")
                .put("count", "$count");

        return new JsonObject().put("$project", project);
    }

    public JsonArray searchPipeline() {
        JsonArray pipeline = new JsonArray()
                .add(match())
                .add(group())
                .add(project());

        if (this.filter().from() != null || this.filter().to() != null) {
            pipeline.add(fromToMatcher());
        }

        return pipeline;
    }

    private JsonObject limit() {
        return new JsonObject()
                .put("$limit", Global.PAGE_SIZE);
    }

    private JsonObject skip() {
        return new JsonObject()
                .put("$skip", this.filter().page() * Global.PAGE_SIZE);
    }

    private JsonObject fromToMatcher() {
        JsonObject filterMatcher = new JsonObject();
        if (this.filter().from() != null) {
            filterMatcher.put("$gte", this.filter().from());
        }

        if (this.filter().to() != null) {
            filterMatcher.put("$lte", this.filter().to());
        }

        JsonObject count = new JsonObject()
                .put("count", filterMatcher);

        return new JsonObject()
                .put("$match", count);
    }

    private JsonObject match(List<String> types) {
        List<String> userIdentifiers = !this.filter().users().isEmpty()
                ? this.filter().users()
                : this.users().stream().map(User::id).collect(Collectors.toList());

        JsonObject matcher = new JsonObject()
                .put("structure", this.filter.structure())
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

    private JsonObject match() {
        return match(this.filter().types());
    }

    private JsonArray filterType(List<String> types) {
        List<String> typesToUse = types != null ? types : this.filter().types();
        JsonArray filters = new JsonArray();
        for (String type : typesToUse) {
            JsonObject filterType = new JsonObject()
                    .put("type", type);

            if ("JUSTIFIED_UNREGULARIZED_ABSENCE".equals(type) || "REGULARIZED_ABSENCE".equals(type)) {
                JsonObject inFilter = new JsonObject()
                        .put("$in", this.filter().reasons());
                filterType.put("reasons", inFilter);
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

    private JsonObject group() {
        JsonObject id = new JsonObject()
                .put("user", "$user")
                .put("type", "$type")
                .put("name", "$name")
                .put("class_name", "$class_name");

        JsonObject count = new JsonObject()
                .put("$sum", 1);

        JsonObject slots = new JsonObject()
                .put("$sum", "$slot");

        JsonObject group = new JsonObject()
                .put("_id", id)
                .put("count", count)
                .put("slots", slots);

        return new JsonObject()
                .put("$group", group);
    }

    private JsonObject project() {
        JsonObject project = new JsonObject()
                .put("_id", 0)
                .put("user", "$_id.user")
                .put("type", "$_id.type")
                .put("count", "$count")
                .put("slots", "$slots");

        return new JsonObject()
                .put("$project", project);
    }
}
