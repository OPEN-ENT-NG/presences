package fr.openent.statistics_presences.filter;

import fr.wseduc.webutils.http.Renders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Filter {
    private final Logger log = LoggerFactory.getLogger(Filter.class);
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

    @SuppressWarnings("unchecked")
    public Filter(String structure, JsonObject body) {
        this.structure = structure;
        this.start = body.getString("start", null);
        this.end = body.getString("end", null);
        this.types.addAll(body.getJsonArray(FilterField.TYPES, new JsonArray()).getList());
        this.audiences.addAll(body.getJsonArray(FilterField.AUDIENCES, new JsonArray()).getList());
        this.users.addAll(body.getJsonArray(FilterField.USERS, new JsonArray()).getList());
        this.reasons.addAll(body.getJsonArray(FilterField.REASONS, new JsonArray()).getList());
        this.punishmentTypes.addAll(body.getJsonArray(FilterField.PUNISHMENT_TYPES, new JsonArray()).getList());
        this.sanctionTypes.addAll(body.getJsonArray(FilterField.SANCTION_TYPES, new JsonArray()).getList());
        this.exportOption = body.getString(FilterField.EXPORT_OPTION);

        JsonObject filters = body.getJsonObject(FilterField.FILTERS, new JsonObject());
        this.from = filters.getInteger(FilterField.FROM, null);
        this.to = filters.getInteger(FilterField.TO, null);
        this.hourDetail = filters.getBoolean(FilterField.HOUR_DETAILS, false);
    }

    public Filter(HttpServerRequest request) {
        try {
            this.structure = request.getParam("structure");
            this.start = request.getParam("start");
            this.end = request.getParam("end");
            this.types.addAll(request.params().getAll(FilterField.TYPES));
            this.audiences.addAll(request.params().getAll(FilterField.AUDIENCES));
            this.users.addAll(request.params().getAll(FilterField.USERS));
            this.reasons.addAll(request.params().getAll(FilterField.REASONS).stream().map(Integer::parseInt).collect(Collectors.toList()));
            this.punishmentTypes.addAll(request.params().getAll(FilterField.PUNISHMENT_TYPES).stream().map(Integer::parseInt).collect(Collectors.toList()));
            this.sanctionTypes.addAll(request.params().getAll(FilterField.SANCTION_TYPES).stream().map(Integer::parseInt).collect(Collectors.toList()));
            this.exportOption = request.getParam(FilterField.EXPORT_OPTION);
            this.from = request.params().contains(FilterField.FROM) ? Integer.parseInt(request.getParam(FilterField.FROM)) : null;
            this.to = request.params().contains(FilterField.TO) ? Integer.parseInt(request.getParam(FilterField.TO)) : null;
            this.hourDetail = request.params().contains(FilterField.HOUR_DETAILS) && Boolean.parseBoolean(request.getParam(FilterField.HOUR_DETAILS));
        } catch (Exception e) {
            log.error("Failed to parse Filter from HttpServerRequest", e);
            Renders.badRequest(request);
        }
    }

    public Filter setUsers(List<String> users) {
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

    public Filter setPage(Integer page) {
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
}
