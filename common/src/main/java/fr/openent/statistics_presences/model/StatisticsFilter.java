package fr.openent.statistics_presences.model;

import fr.wseduc.webutils.http.Renders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StatisticsFilter {
    private final Logger log = LoggerFactory.getLogger(StatisticsFilter.class);
    private final List<String> types = new ArrayList<>();
    private final List<String> audiences = new ArrayList<>();
    private final List<String> users = new ArrayList<>();
    private final List<Integer> reasons = new ArrayList<>();
    private final List<Integer> punishmentTypes = new ArrayList<>();
    private final List<Integer> sanctionTypes = new ArrayList<>();
    private String structure;
    private String start;
    private String end;
    private Integer from;
    private Integer to;
    private String exportOption;
    private Boolean hourDetail;
    private Integer page;
    private Boolean rateDisplay;

    @SuppressWarnings("unchecked")
    public StatisticsFilter(String structure, JsonObject body) {
        this.structure = structure;
        this.start = body.getString("start", null);
        this.end = body.getString("end", null);
        this.types.addAll(body.getJsonArray(StatisticsFilterField.TYPES, new JsonArray()).getList());
        this.audiences.addAll(body.getJsonArray(StatisticsFilterField.AUDIENCES, new JsonArray()).getList());
        this.users.addAll(body.getJsonArray(StatisticsFilterField.USERS, new JsonArray()).getList());
        this.reasons.addAll(body.getJsonArray(StatisticsFilterField.REASONS, new JsonArray()).getList());
        this.punishmentTypes.addAll(body.getJsonArray(StatisticsFilterField.PUNISHMENT_TYPES, new JsonArray()).getList());
        this.sanctionTypes.addAll(body.getJsonArray(StatisticsFilterField.SANCTION_TYPES, new JsonArray()).getList());
        this.exportOption = body.getString(StatisticsFilterField.EXPORT_OPTION);

        JsonObject filters = body.getJsonObject(StatisticsFilterField.FILTERS, new JsonObject());
        this.from = filters.getInteger(StatisticsFilterField.FROM, null);
        this.to = filters.getInteger(StatisticsFilterField.TO, null);
        this.hourDetail = filters.getBoolean(StatisticsFilterField.HOUR_DETAILS, false);
    }

    public StatisticsFilter(HttpServerRequest request) {
        try {
            this.structure = request.getParam("structure");
            this.start = request.getParam("start");
            this.end = request.getParam("end");
            this.types.addAll(request.params().getAll(StatisticsFilterField.TYPES));
            this.audiences.addAll(request.params().getAll(StatisticsFilterField.AUDIENCES));
            this.users.addAll(request.params().getAll(StatisticsFilterField.USERS));
            this.reasons.addAll(request.params().getAll(StatisticsFilterField.REASONS).stream().map(Integer::parseInt).collect(Collectors.toList()));
            this.punishmentTypes.addAll(request.params().getAll(StatisticsFilterField.PUNISHMENT_TYPES).stream().map(Integer::parseInt).collect(Collectors.toList()));
            this.sanctionTypes.addAll(request.params().getAll(StatisticsFilterField.SANCTION_TYPES).stream().map(Integer::parseInt).collect(Collectors.toList()));
            this.exportOption = request.getParam(StatisticsFilterField.EXPORT_OPTION);
            this.from = request.params().contains(StatisticsFilterField.FROM) ? Integer.parseInt(request.getParam(StatisticsFilterField.FROM)) : null;
            this.to = request.params().contains(StatisticsFilterField.TO) ? Integer.parseInt(request.getParam(StatisticsFilterField.TO)) : null;
            this.hourDetail = request.params().contains(StatisticsFilterField.HOUR_DETAILS) && Boolean.parseBoolean(request.getParam(StatisticsFilterField.HOUR_DETAILS));
            this.rateDisplay = Boolean.parseBoolean(request.getParam(StatisticsFilterField.RATE));
        } catch (Exception e) {
            log.error("Failed to parse Filter from HttpServerRequest", e);
            Renders.badRequest(request);
        }
    }

    public StatisticsFilter setUsers(List<String> users) {
        this.users.addAll(users);
        return this;
    }

    public String structure() {
        return this.structure;
    }

    public List<String> types() {
        return this.types;
    }

    public List<String> audiences() {
        return this.audiences;
    }

    public List<String> users() {
        return this.users;
    }

    public List<Integer> reasons() {
        return this.reasons;
    }

    public List<Integer> punishmentTypes() {
        return this.punishmentTypes;
    }

    public List<Integer> sanctionTypes() {
        return this.sanctionTypes;
    }

    public String exportOption() {
        return this.exportOption;
    }

    public Integer from() {
        return this.from;
    }

    public Integer to() {
        return this.to;
    }

    public Boolean hourDetail() {
        return this.hourDetail;
    }

    public Boolean rateDisplay() {
        return this.rateDisplay;
    }


    public StatisticsFilter setPage(Integer page) {
        this.page = page;
        return this;
    }

    public Integer page() {
        return this.page;
    }

    public String start() {
        return this.start;
    }

    public String end() {
        return this.end;
    }

    public static class StatisticsFilterField {
        public static final String START = "start";
        public static final String END = "end";
        public static final String TYPES = "types";
        public static final String AUDIENCES = "audiences";
        public static final String USERS = "users";
        public static final String FILTERS = "filters";
        public static final String REASONS = "reasons";
        public static final String PUNISHMENT_TYPES = "punishmentTypes";
        public static final String SANCTION_TYPES = "sanctionTypes";
        public static final String EXPORT_OPTION = "export_option";
        public static final String FROM = "FROM";
        public static final String TO = "TO";
        public static final String HOUR_DETAILS = "HOUR_DETAIL";
        public static final String TOTAL = "TOTAL";
        public static final String RATE = "rate";

        private StatisticsFilterField() {
            throw new IllegalStateException("Utility class");
        }
    }
}
