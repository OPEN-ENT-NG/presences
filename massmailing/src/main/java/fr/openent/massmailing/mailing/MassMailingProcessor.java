package fr.openent.massmailing.mailing;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.MassmailingType;
import fr.openent.massmailing.enums.TemplateCode;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.presences.Presences;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.enums.EventType;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
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

import java.text.ParseException;
import java.util.*;

import static fr.openent.massmailing.enums.MailingType.PDF;
import static fr.openent.massmailing.enums.MassmailingType.LATENESS;
import static fr.openent.massmailing.enums.MassmailingType.NO_REASON;

public abstract class MassMailingProcessor implements Mailing {
    Logger LOGGER = LoggerFactory.getLogger(MassMailingProcessor.class);
    private final MailingType mailingType;
    private final Template template;
    private final Boolean massmailed;
    private final List<MassmailingType> massmailingTypeList;
    private final String structure;
    private final List<Integer> reasons;
    private final String start;
    private final String end;
    private final Boolean noReason;
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
        Future<JsonObject> hourEventsFuture = Future.future();
        Future<JsonObject> reasonsFuture = Future.future();
        Future<JsonArray> slotsFutures = Future.future();
        Future<JsonObject> settingFutures = Future.future();
        Future<JsonObject> recoveryFuture = Future.future();

        CompositeFuture.all(Arrays.asList(templateFuture, relativeFuture, eventsFuture, reasonsFuture, slotsFutures,
                settingFutures, recoveryFuture, hourEventsFuture)).setHandler(asyncResult -> {
            if (asyncResult.failed()) {
                LOGGER.error("[Massmailing@MassMailingProcessor] Failed to process", asyncResult.cause());
                handler.handle(new Either.Left<>(asyncResult.cause().toString()));
            }

            JsonObject reasons = reasonsFuture.result();
            JsonObject events = eventsFuture.result();
            JsonObject hourEvents = hourEventsFuture.result();
            JsonObject relatives = relativeFuture.result();
            HashMap<String, JsonObject> massmailings = formatData(events, relatives.copy(), reasons);
            HashMap<String, JsonObject> massmailingsHour = formatData(hourEvents, relatives.copy(), reasons);
            JsonArray slots = slotsFutures.result();
            JsonObject settings = settingFutures.result();
            String recoveryMethod = recoveryFuture.result().containsKey("event_recovery_method") ? recoveryFuture.result().getString("event_recovery_method") : "HALF_DAY";

            List<JsonObject> result = new ArrayList<>();
            for (String key : massmailings.keySet()) {
                result.add(massmailings.get(key));
                JsonObject massmailing = formatMassmailingBasedOnRecoveryMethod(massmailings.get(key), slots, recoveryMethod, settings.getString("end_of_half_day"));
                setCodeValues(massmailingsHour, slots, settings, recoveryMethod, key, massmailing);
            }
            handler.handle(new Either.Right<>(result));
        });

        retrieveEvents(null, FutureHelper.handlerJsonObject(eventsFuture));
        retrieveEvents("HOUR", FutureHelper.handlerJsonObject(hourEventsFuture));
        retrieveRelatives(FutureHelper.handlerJsonObject(relativeFuture));
        template.init(FutureHelper.handlerJsonObject(templateFuture));
        fetchReasons(FutureHelper.handlerJsonObject(reasonsFuture));
        Viescolaire.getInstance().getDefaultSlots(this.structure, FutureHelper.handlerJsonArray(slotsFutures));
        Viescolaire.getInstance().getSlotProfileSetting(this.structure, FutureHelper.handlerJsonObject(settingFutures));
        Presences.getInstance().getSettings(this.structure, FutureHelper.handlerJsonObject(recoveryFuture));
    }

    private void setCodeValues(HashMap<String, JsonObject> massmailingsHour, JsonArray slots, JsonObject settings,
                               String recoveryMethod, String key, JsonObject massmailing) {
        HashMap<TemplateCode, Object> codeValues = new HashMap<>();

        // Child Name code implemented
        codeValues.put(TemplateCode.CHILD_NAME, massmailing.getString("studentDisplayName", ""));
        // Legal Name code implemented
        codeValues.put(TemplateCode.LEGAL_NAME, massmailingsHour.get(key).getString("displayName", ""));
        // Class Name code implemented
        codeValues.put(TemplateCode.CLASS_NAME, massmailing.getString("className", ""));
        // Date code implemented
        codeValues.put(TemplateCode.DATE, DateHelper.getCurrentDate(DateHelper.DAY_MONTH_YEAR));
        // Zipcode code implemented
        codeValues.put(TemplateCode.ZIPCODE_CITY, massmailingsHour.get(key).getString("zipcodeCity", ""));
        // Address code implemented
        codeValues.put(TemplateCode.ADDRESS, massmailingsHour.get(key).getString("address", ""));

        JsonObject massmailingHour = formatMassmailingBasedOnRecoveryMethod(massmailingsHour.get(key), slots, "hour", settings.getString("end_of_half_day"));
        JsonArray absencesHour = massmailingHour.getJsonObject("events", new JsonObject()).getJsonArray(EventType.ABSENCE.name(), new JsonArray());
        JsonArray latenessesHour = massmailingHour.getJsonObject("events", new JsonObject()).getJsonArray(EventType.LATENESS.name(), new JsonArray());

        JsonArray absences = massmailing.getJsonObject("events", new JsonObject()).getJsonArray(EventType.ABSENCE.name(), new JsonArray());
        codeValues.put(TemplateCode.ABSENCE_NUMBER, absences.size() + " " + getTranslatedUnit(recoveryMethod));
        codeValues.put(TemplateCode.LATENESS_NUMBER, latenessesHour.size());

        if (mailingType.equals(MailingType.SMS)) {
            JsonObject absence = null;
            JsonObject lateness = null;
            if (absencesHour.size() > 0) absence = absencesHour.getJsonObject(absencesHour.size() - 1);
            codeValues.put(TemplateCode.LAST_ABSENCE, absence);

            if (latenessesHour.size() > 0) lateness = latenessesHour.getJsonObject(latenessesHour.size() - 1);
            codeValues.put(TemplateCode.LAST_LATENESS, lateness);
        } else {
            codeValues.put(TemplateCode.SUMMARY, massmailingHour.getJsonObject("events", new JsonObject()));
        }
        massmailing.put("message", template.process(codeValues));
    }

    private String transformDate(String date, String slotHour) {
        try {
            Calendar slotCal = Calendar.getInstance();
            slotCal.setTime(DateHelper.parse(slotHour, "HH:mm:ss"));

            Calendar dateCal = Calendar.getInstance();
            dateCal.setTime(DateHelper.parse(date, DateHelper.SQL_DATE_FORMAT));
            dateCal.set(Calendar.HOUR_OF_DAY, slotCal.get(Calendar.HOUR_OF_DAY));
            dateCal.set(Calendar.MINUTE, slotCal.get(Calendar.MINUTE));
            dateCal.set(Calendar.SECOND, slotCal.get(Calendar.SECOND));
            return DateHelper.getPsqlSimpleDateFormat().format(dateCal.getTime());
        } catch (ParseException e) {
            LOGGER.error("[Massmailing@MassMailingProcessor] Failed to transform date", e);
            return "";
        }
    }

    private String getTranslatedUnit(String recoveryType) {
        return I18n.getInstance().translate("massmailing.recovery." + recoveryType.toLowerCase() + "s", template.getDomain(), template.getLocale());
    }

    private JsonObject formatMassmailingBasedOnRecoveryMethod(JsonObject massmailing, JsonArray slots, String recoveryMethod, String midHour) {
        JsonObject events = massmailing.getJsonObject("events", new JsonObject());
        if ("HOUR".equals(recoveryMethod) || slots.isEmpty() || !events.containsKey("ABSENCE")) {
            return massmailing;
        }

        if (events.containsKey("ABSENCE")) {
            String startMorningHour = slots.getJsonObject(0).getString("start_hour");
            String endAfternoonHour = slots.getJsonObject(slots.size() - 1).getString("end_hour");
            String startAfternoonHour = startAfternoonHour(midHour, slots);
            JsonArray absences = massmailing.getJsonObject("events").getJsonArray("ABSENCE");
            for (int i = 0; i < absences.size(); i++) {
                JsonObject absence = absences.getJsonObject(i);
                switch (recoveryMethod) {
                    case "hour":
                        absence.put("display_start_date", absence.getString("start_date"));
                        absence.put("display_end_date", absence.getString("end_date"));
                        break;
                    case "DAY":
                        absence.put("display_start_date", transformDate(absence.getString("start_date"), startMorningHour));
                        absence.put("display_end_date", transformDate(absence.getString("end_date"), endAfternoonHour));
                        break;
                    case "HALF_DAY":
                    default:
                        String period = absence.getString("period");
                        //TODO an error throw on the second related due to reference changes below
                        absence.put("display_start_date", transformDate(absence.getString("start_date"), "MORNING".equals(period) ? startMorningHour : startAfternoonHour));
                        absence.put("display_end_date", transformDate(absence.getString("end_date"), "MORNING".equals(period) ? midHour : endAfternoonHour));

                }
            }
        }

        if (massmailing.getJsonObject("events").containsKey("LATENESS")) {
            JsonArray latenesses = new JsonArray();
            JsonArray values = massmailing.getJsonObject("events").getJsonArray("LATENESS");
            for (int i = 0; i < values.size(); i++) {
                JsonObject lateness = values.getJsonObject(i);
                latenesses.addAll(lateness.getJsonArray("events"));
            }
            massmailing.getJsonObject("events").put("LATENESS", latenesses);
        }
        return massmailing;
    }

    private String startAfternoonHour(String midHour, JsonArray slots) {
        for (int i = 0; i < slots.size(); i++) {
            if (midHour.equals(slots.getJsonObject(i).getString("end_hour"))) {
                int index = i < slots.size() ? i + 1 : i;
                return slots.getJsonObject(index).getString("start_hour");
            }
        }

        return "12:00:00";
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

    private HashMap<String, JsonObject> formatData(JsonObject events, JsonObject relatives, JsonObject reasons) {
        HashMap<String, JsonObject> data = new HashMap<>();

        List<String> relativeIdentifiers = new ArrayList<>(relatives.getMap().keySet());
        Map<String, JsonObject> duplicatedData = new HashMap<>();

        for (String relativeId : relativeIdentifiers) {

            JsonObject relative = relatives.getJsonObject(relativeId);

            if (events.getJsonArray(relative.getString("student_id")) != null) {

                relative.put("events", groupEvents(events.getJsonArray(relative.getString("student_id")), reasons));

                // if student_id is not seen first time
                if (!duplicatedData.containsKey(relative.getString("student_id"))) {
                    duplicatedData.put(relative.getString("student_id"), relative);
                    data.put(relative.getString("id"), relative);
                } else {
                    // case we already seen student_id
                    proceedOnDuplicate(data, duplicatedData, relative);
                }
            }

        }

        return data;
    }

    private void proceedOnDuplicate(HashMap<String, JsonObject> data, Map<String, JsonObject> duplicatedData, JsonObject relative) {

        String duplicatedDataContact = this.mailingType.equals(PDF) ? getAddressAndZipCodeValue(duplicatedData, relative)
                : duplicatedData.get(relative.getString("student_id")).getString("contact");
        String relativeContact = this.mailingType.equals(PDF) ? getAddressAndZipCodeValue(relative) : relative.getString("contact");

        // first check if has contact or if it is null
        if (duplicatedDataContact == null) {
            // case it is null or non existing, we then add new relative object
            data.put(relative.getString("id"), relative);
        } else if (duplicatedDataContact.equals(relativeContact)) {
            // case it is not null and they are also EQUAL, we then modify the data already
            // set with displayName that will concat with another relative displayName
            data.get(duplicatedData.get(relative.getString("student_id")).getString("id"))
                    .put("displayName", data.get(duplicatedData.get(relative.getString("student_id")).getString("id"))
                            .getString("displayName") + ", " + relative.getString("displayName"));
        } else {
            // case there is no match, we add new relative object
            data.put(relative.getString("id"), relative);
        }
    }

    private String getAddressAndZipCodeValue(Map<String, JsonObject> duplicatedData, JsonObject relative) {
        return duplicatedData.get(relative.getString("student_id")).getString("address") + ", " +
                duplicatedData.get(relative.getString("student_id")).getString("zipcodeCity");
    }

    private String getAddressAndZipCodeValue(JsonObject relative) {
        return relative.getString("address") + ", " + relative.getString("zipcodeCity");
    }

    private JsonObject groupEvents(JsonArray events, JsonObject reasons) {
        JsonObject res = new JsonObject();
        for (MassmailingType type : massmailingTypeList) {
            if (type.equals(MassmailingType.REGULARIZED) || type.equals(MassmailingType.UNREGULARIZED)
                    || type.equals(NO_REASON)
                            ) {
                res.put(EventType.ABSENCE.name(), new JsonArray());
            } else {
                res.put(type.name(), new JsonArray());
            }
        }

        List<EventType> EventTypeDescriptor = Arrays.asList(EventType.ABSENCE, EventType.LATENESS);
        for (int i = 0; i < events.size(); i++) {
            JsonObject event = events.getJsonObject(i);
            JsonArray eventEvts = event.getJsonArray("events");
            for (int j = 0; j < eventEvts.size(); j++) {
                JsonObject evt = eventEvts.getJsonObject(j);
                Integer reasonId = evt.getInteger("reason_id");
                if (reasonId != null) {
                    evt.put("reason", reasons.getJsonObject(reasonId.toString()).getString("label"));
                    evt.put("proving", reasons.getJsonObject(reasonId.toString()).getBoolean("proving"));
                }
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
                case REGULARIZED:
                case UNREGULARIZED:
                case NO_REASON:
                    res.add(EventType.ABSENCE.getType());
                    break;
                case LATENESS:
                    res.add(EventType.LATENESS.getType());
                    break;
            }
        }

        return res;
    }

    private Integer getEventTypeCode(MassmailingType mailingType) {
        Integer code = null;
        switch (mailingType) {
            case REGULARIZED:
            case UNREGULARIZED:
            case NO_REASON:
                code = EventType.ABSENCE.getType();
                break;
            case LATENESS:
                code = EventType.LATENESS.getType();
                break;
        }

        return code;
    }

    /**
     * Check if massmailing is justified or not.
     * If massmailingTypeList contains REGULARIZED then the massmailing is justified
     * If massmailingTypeList contains UNREGULARIZED or NO_REASON then the massmailing is not justified
     * If massmailingTypeList contains REGULARIZED _AND_ UNREGULARIZED then the massmailing is null
     *
     * @return justified status
     */
    private Boolean isJustified(MassmailingType type) {
        Boolean justified = null;
        switch (type) {
            case REGULARIZED:
                justified = true;
                break;
            case LATENESS:
            case UNREGULARIZED:
            case NO_REASON:
                justified = false;
                break;
        }

        return justified;
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
                contactValue = "null";
        }

        String query = "MATCH (u:User)-[:RELATED]->(r:User) WHERE u.id IN {students} AND r.id IN {relatives} RETURN DISTINCT r.id as id, " +
                "(r.lastName + ' ' + r.firstName) as displayName, " + contactValue + " as contact, r.address as address, " +
                "(r.zipCode + ' ' + r.city) as zipcodeCity, u.id as student_id, " +
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
     * @param recoveryMethod Method used to recover events, can be null if method in settings wanted.
     * @param handler        Function handler returning data
     */
    void retrieveEvents(String recoveryMethod, Handler<Either<String, JsonObject>> handler) {
        List<Future> futures = new ArrayList<>();
        for (MassmailingType type : massmailingTypeList) {
            Future<JsonArray> future = Future.future();
            Presences.getInstance().getEventsByStudent(getEventTypeCode(type), getStudentsList(), structure, null,
                    (type.equals(NO_REASON) || type.equals(LATENESS) ? new ArrayList<>() : reasons), massmailed, start, end,
                    type.equals(NO_REASON) || type.equals(LATENESS), recoveryMethod, isJustified(type), FutureHelper.handlerJsonArray(future));
            futures.add(future);
        }

        CompositeFuture.all(futures).setHandler(result -> {
            if (result.failed()) {
                LOGGER.error("[Presences@MassMailingProcessor] Failed to retrieve events", result.cause());
                handler.handle(new Either.Left<>(result.cause().toString()));
                return;
            }

            JsonObject res = new JsonObject();
            for (Future<JsonArray> future : futures) {
                JsonArray events = future.result();
                for (int i = 0; i < events.size(); i++) {
                    JsonObject evt = events.getJsonObject(i);
                    if (!res.containsKey(evt.getString("student_id")))
                        res.put(evt.getString("student_id"), new JsonArray());
                    res.getJsonArray(evt.getString("student_id")).add(evt);
                }
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
                JsonArray embedEvts = item.getJsonArray("events", new JsonArray().add(item));
                for (int j = 0; j < embedEvts.size(); j++) {
                    JsonObject evt = embedEvts.getJsonObject(j);
                    evts.add(new JsonObject().put("id", evt.getInteger("id")).put("type", key));
                }
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
