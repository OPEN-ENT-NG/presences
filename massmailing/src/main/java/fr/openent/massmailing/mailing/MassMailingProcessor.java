package fr.openent.massmailing.mailing;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.MassmailingType;
import fr.openent.massmailing.enums.TemplateCode;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.presences.Presences;
import fr.openent.presences.enums.EventType;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public abstract class MassMailingProcessor implements Mailing {
    Logger LOGGER = LoggerFactory.getLogger(MassMailingProcessor.class);
    private MailingType mailingType;
    private Template template;
    private Boolean massmailed;
    private List<MassmailingType> massmailingTypeList;
    private String structure;
    private List<Integer> reasons;
    private String start;
    private String end;
    private Boolean noReason;
    JsonObject students;

    public MassMailingProcessor(MailingType mailingType, String structure, Template template, Boolean massmailed,
                                List<MassmailingType> massmailingTypeList, List<Integer> reasons, String start, String end,
                                Boolean noReason, JsonObject students) {
        this.mailingType = mailingType;
        this.structure = structure;
        this.template = template;
        this.massmailingTypeList = massmailingTypeList;
        this.reasons = reasons;
        this.massmailed = massmailed;
        this.start = start;
        this.end = end;
        this.noReason = noReason;
        this.students = students;
    }

    public Template getTemplate() {
        return template;
    }

    public void process(Handler<Either<String, List<JsonObject>>> handler) {
        Future<JsonObject> templateFuture = Future.future();
        Future<JsonObject> relativeFuture = Future.future();
        Future<JsonObject> eventsFuture = Future.future();
        Future<JsonObject> reasonsFuture = Future.future();
        CompositeFuture.all(templateFuture, relativeFuture, eventsFuture, reasonsFuture).setHandler(asyncResult -> {
            if (asyncResult.failed()) {
                LOGGER.error("[Massmailing@MassMailingProcessor] Failed to process", asyncResult.cause());
                handler.handle(new Either.Left<>(asyncResult.cause().toString()));
            }

            JsonObject reasons = reasonsFuture.result();
            JsonObject events = eventsFuture.result();
            JsonObject relatives = relativeFuture.result();
            JsonArray massmailings = formatData(events, relatives, reasons);

            for (int i = 0; i < massmailings.size(); i++) {
                JsonObject massmailing = massmailings.getJsonObject(i);
                HashMap<TemplateCode, Object> codeValues = new HashMap<>();
                codeValues.put(TemplateCode.CHILD_NAME, massmailing.getString("studentDisplayName", ""));
                codeValues.put(TemplateCode.CLASS_NAME, massmailing.getString("className", ""));
                codeValues.put(TemplateCode.ABSENCE_NUMBER, massmailing.getJsonObject("events", new JsonObject()).getJsonArray(EventType.ABSENCE.name(), new JsonArray()).size());
                codeValues.put(TemplateCode.LATENESS_NUMBER, massmailing.getJsonObject("events", new JsonObject()).getJsonArray(EventType.LATENESS.name(), new JsonArray()).size());
                codeValues.put(TemplateCode.SUMMARY, massmailing.getJsonObject("events", new JsonObject()));
                massmailing.put("message", template.process(codeValues));
            }
            handler.handle(new Either.Right<>(massmailings.getList()));
        });

        retrieveEvents(FutureHelper.handlerJsonObject(eventsFuture));
        retrieveRelatives(FutureHelper.handlerJsonObject(relativeFuture));
        template.init(FutureHelper.handlerJsonObject(templateFuture));
        fetchReasons(FutureHelper.handlerJsonObject(reasonsFuture));
    }

    private void fetchReasons(Handler<Either<String, JsonObject>> handler) {
        Presences.getInstance().getReasons(this.structure, event -> {
            if (event.isLeft()) {
                LOGGER.error("[Presences@MassMailingProcessor] Failed to retrieve reasons", event.left().getValue());
                handler.handle(new Either.Left<>(event.left().getValue()));
                return;
            }

            JsonObject map = new JsonObject();
            JsonArray reasons = event.right().getValue();
            for (int i = 0; i < reasons.size(); i++) {
                JsonObject reason = reasons.getJsonObject(i);
                map.put(reason.getInteger("id").toString(), reason);
            }

            handler.handle(new Either.Right<>(map));
        });
    }

    private JsonArray formatData(JsonObject events, JsonObject relatives, JsonObject reasons) {
        JsonArray data = new JsonArray();
        List<String> relativeIdentifiers = new ArrayList<>(relatives.getMap().keySet());
        for (String relativeId : relativeIdentifiers) {
            JsonObject relative = relatives.getJsonObject(relativeId);
            relative.put("events", groupEvents(events.getJsonArray(relative.getString("student_id")), reasons));
            data.add(relative);
        }

        return data;
    }

    private JsonObject groupEvents(JsonArray events, JsonObject reasons) {
        JsonObject res = new JsonObject();
        for (MassmailingType type : massmailingTypeList) {
            if (type.equals(MassmailingType.JUSTIFIED) || type.equals(MassmailingType.UNJUSTIFIED)) {
                res.put(EventType.ABSENCE.name(), new JsonArray());
            } else {
                res.put(type.name(), new JsonArray());
            }
        }

        List<EventType> EventTypeDescriptor = Arrays.asList(EventType.ABSENCE, EventType.LATENESS);
        for (int i = 0; i < events.size(); i++) {
            JsonObject event = events.getJsonObject(i);
            Integer reasonId = event.getInteger("reason_id");
            if (reasonId != null) {
                event.put("reason", reasons.getJsonObject(reasonId.toString()).getString("label"));
                event.put("proving", reasons.getJsonObject(reasonId.toString()).getBoolean("proving"));
            }
            EventType evtType = EventTypeDescriptor.get(event.getInteger("type_id") - 1);
            res.getJsonArray(evtType.name()).add(event);
        }

        return res;
    }

    /**
     * Retrieve student identifier list based on students map
     *
     * @return Student identifiers
     */
    private List<String> getStudentsList() {
        return new ArrayList<>(students.getMap().keySet());
    }

    /**
     * Transform mailingTypeList object to an ArrayList containing each EventType code
     *
     * @return Event Type code list
     */
    private List<Integer> getEventTypeCodeList() {
        List<Integer> res = new ArrayList<>();
        for (MassmailingType type : massmailingTypeList) {
            switch (type) {
                case JUSTIFIED:
                case UNJUSTIFIED:
                    res.add(EventType.ABSENCE.getType());
                    break;
                case LATENESS:
                    res.add(EventType.LATENESS.getType());
                    break;
            }
        }

        return res;
    }

    /**
     * Check if massmailing is justified or not.
     * If massmailingTypeList contains JUSTIFIED then the massmailing is justified
     * If massmailingTypeList contains UNJUSTIFIED then the massmailing is not justified
     * If massmailingTypeList contains JUSTIFIED _AND_ UNJUSTIFIED then the massmailing is null
     *
     * @return justified status
     */
    private Boolean isJustified() {
        Boolean bool = null;
        if (massmailingTypeList.contains(MassmailingType.UNJUSTIFIED) && massmailingTypeList.contains(MassmailingType.JUSTIFIED)) {
            return bool;
        }

        if (massmailingTypeList.contains(MassmailingType.JUSTIFIED)) {
            bool = true;
        }

        if (massmailingTypeList.contains(MassmailingType.UNJUSTIFIED)) {
            bool = false;
        }

        return bool;
    }

    /**
     * Retrieve relatives based on students map
     *
     * @param handler Function handler returning data
     */
    private void retrieveRelatives(Handler<Either<String, JsonObject>> handler) {
        List<String> studentsList = getStudentsList();
        JsonArray relativeIdentifiers = new JsonArray();
        for (String student : studentsList) {
            relativeIdentifiers.addAll(students.getJsonArray(student));
        }

        String contactValue;
        switch (mailingType) {
            case MAIL:
                contactValue = "r.email";
                break;
            case SMS:
                contactValue = "r.mobile";
                break;
            case PDF:
            default:
                contactValue = "";
        }

        String query = "MATCH (u:User)-[:RELATED]->(r:User) WHERE u.id IN {students} AND r.id IN {relatives} RETURN DISTINCT r.id as id, " +
                "(r.lastName + ' ' + r.firstName) as displayName, " + contactValue + " as contact, u.id as student_id, " +
                "(u.lastName + ' ' + u.firstName) as studentDisplayName, split(u.classes[0],'$')[1] as className";
        JsonObject params = new JsonObject()
                .put("students", new JsonArray(studentsList))
                .put("relatives", relativeIdentifiers);
        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(new Either.Left<>(event.left().getValue()));
                return;
            }

            JsonObject result = new JsonObject();
            JsonArray relatives = event.right().getValue();
            for (int i = 0; i < relatives.size(); i++) {
                JsonObject relative = relatives.getJsonObject(i);
                result.put(relative.getString("id"), relative);
            }

            handler.handle(new Either.Right<>(result));
        }));
    }

    /**
     * Retrieve student events
     *
     * @param handler Function handler returning data
     */
    void retrieveEvents(Handler<Either<String, JsonObject>> handler) {
        Presences.getInstance().getEventsByStudent(getEventTypeCodeList(), getStudentsList(), structure, isJustified(), reasons, massmailed, start, end, noReason, event -> {
            if (event.isLeft()) {
                LOGGER.error("[Presences@MassMailingProcessor] Failed to retrieve events", event.left().getValue());
                handler.handle(new Either.Left<>(event.left().getValue()));
                return;
            }

            JsonArray events = event.right().getValue();
            JsonObject res = new JsonObject();
            for (int i = 0; i < events.size(); i++) {
                JsonObject evt = events.getJsonObject(i);
                if (!res.containsKey(evt.getString("student_id")))
                    res.put(evt.getString("student_id"), new JsonArray());
                res.getJsonArray(evt.getString("student_id")).add(evt);
            }

            handler.handle(new Either.Right<>(res));
        });
    }

    void saveMassmailing(JsonObject mailing, Handler<Either<String, JsonObject>> handler) {
        JsonObject events = mailing.getJsonObject("events");
        List<JsonObject> eventsToSave = getEventsToSave(events);
        String query = "SELECT nextval('" + Massmailing.dbSchema + ".mailing_id_seq') as id";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(idMailing -> {
            if (idMailing.isLeft()) {
                handler.handle(new Either.Left<>("[Massmailing@MassMailingProcessor] Failed to query next massmailing identifier"));
                return;
            }

            Integer id = idMailing.right().getValue().getInteger("id");
            JsonArray statements = new JsonArray()
                    .add(getMailingSavingStatement(id, mailing))
                    .add(getMailingEventSavingStatement(id, eventsToSave));

            Sql.getInstance().transaction(statements, SqlResult.validUniqueResultHandler(handler));
        }));
    }

    private List<JsonObject> getEventsToSave(JsonObject events) {
        List<JsonObject> evts = new ArrayList<>();
        List<String> keys = new ArrayList<>(events.fieldNames());
        for (String key : keys) {
            JsonArray list = events.getJsonArray(key);
            for (int i = 0; i < list.size(); i++) {
                JsonObject item = list.getJsonObject(i);
                evts.add(new JsonObject().put("id", item.getInteger("id")).put("type", key));
            }
        }

        return evts;
    }

    private JsonObject getMailingSavingStatement(Integer id, JsonObject mailing) {
        String query = "INSERT INTO " + Massmailing.dbSchema + ".mailing(id, student_id, structure_id, type, content, recipient_id, recipient) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?);";
        JsonArray params = new JsonArray()
                .add(id)
                .add(mailing.getString("student_id"))
                .add(structure)
                .add(mailingType.name())
                .add(mailing.getString("message"))
                .add(mailing.getString("id"))
                .add(mailing.getString("contact"));
        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject getMailingEventSavingStatement(Integer mailingId, List<JsonObject> events) {
        StringBuilder query = new StringBuilder("INSERT INTO massmailing.mailing_event(mailing_id, event_id, event_type) VALUES ");
        JsonArray params = new JsonArray();
        for (JsonObject event : events) {
            query.append("(?, ?, ?),");
            params.add(mailingId).add(event.getInteger("id")).add(event.getString("type"));
        }

        return new JsonObject()
                .put("statement", query.toString().substring(0, query.toString().length() - 1))
                .put("values", params)
                .put("action", "prepared");
    }
}
