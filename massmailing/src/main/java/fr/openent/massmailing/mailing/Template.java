package fr.openent.massmailing.mailing;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.TemplateCode;
import fr.openent.massmailing.service.SettingsService;
import fr.openent.massmailing.service.impl.DefaultSettingsService;
import fr.openent.presences.common.helper.DateHelper;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.http.BaseServer;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Template extends BaseServer {
    private Logger LOGGER = LoggerFactory.getLogger(Template.class);
    private SettingsService settingsService;
    private WorkspaceHelper workspaceHelper;
    private HashMap<TemplateCode, String> systemCodes = new HashMap<>();
    private HashMap<String, String> imageMap = new HashMap<>();
    private MailingType mailingType;
    private Integer id;
    private String structure;
    private String content;
    private String locale;
    private String domain;

    public Template(MailingType mailingType, Integer templateIdentifier, String structureId, JsonObject config) {
        this.workspaceHelper = Massmailing.workspaceHelper;
        this.config = config;
        this.settingsService = new DefaultSettingsService();
        this.mailingType = mailingType;
        this.id = templateIdentifier;
        this.structure = structureId;
    }

    public void init(Handler<Either<String, JsonObject>> handler) {
        settingsService.get(mailingType, id, structure, event -> {
            if (event.isLeft()) {
                LOGGER.error("[Massmailing@Template] Failed to init template");
                handler.handle(event.left());
                return;
            }
            JsonObject template = event.right().getValue();
            this.content = template.getString("content");

            for (TemplateCode code : TemplateCode.values()) {
                String i18nValue = I18n.getInstance().translate(code.getKey(), domain, locale);
                systemCodes.put(code, i18nValue);
            }

            replaceImages(handler);
        });
    }

    private void replaceImages(Handler<Either<String, JsonObject>> handler) {
        Pattern globalPattern = Pattern.compile("<img src=\\\"\\/workspace\\/document\\/[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}.*>");
        Pattern idPattern = Pattern.compile("[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}");
        Matcher matcher = globalPattern.matcher(this.content);
        List<Future> futures = new ArrayList<>();
        while (matcher.find()) {
            String img = matcher.group();
            Matcher idMatch = idPattern.matcher(img);
            if (!idMatch.find()) continue;
            Future<String> future = Future.future();
            getBase64File(idMatch.group(), future);
            futures.add(future);
        }

        if (futures.isEmpty()) {
            handler.handle(new Either.Right<>(new JsonObject()));
            return;
        }

        CompositeFuture.all(futures).setHandler(asyncHandler -> {
            if (asyncHandler.failed()) {
                handler.handle(new Either.Left<>(asyncHandler.cause().toString()));
                return;
            }

            List<String> images = new ArrayList<>(imageMap.keySet());
            for (String image : images) {
                this.content = this.content.replaceAll("\\/workspace\\/document\\/" + image + "[^\"]*\"", this.imageMap.get(image) + "\"");
            }

            handler.handle(new Either.Right<>(new JsonObject()));
        });
    }

    private void getBase64File(String id, Future<String> future) {
        workspaceHelper.readDocument(id, document -> {
            if (document == null) future.fail("Document not found");
            else {
                String base64 = Base64.getEncoder().encodeToString(document.getData().getBytes());
                imageMap.put(id, "data:" + document.getDocument().getJsonObject("metadata").getString("content-type") + ";base64," + base64);
                future.complete(base64);
            }
        });
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getLocale() {
        return this.locale;
    }

    public String getDomain() {
        return this.domain;
    }

    public String process(HashMap<TemplateCode, Object> codeValues) {
        String value = this.content;
        List<TemplateCode> codes = new ArrayList<>(codeValues.keySet());
        for (TemplateCode code : codes) {
            if (TemplateCode.SUMMARY.equals(code) || TemplateCode.LAST_ABSENCE.equals(code) || TemplateCode.LAST_LATENESS.equals(code))
                continue;
            try {
                value = value.replaceAll(Pattern.quote(systemCodes.get(code)), codeValues.get(code).toString());
            } catch (Exception e) {
                LOGGER.error("[Massmailing@Template] Failed to replace code for code : " + code);
            }
        }

        if (codes.contains(TemplateCode.SUMMARY)) {
            value = value.replaceAll(Pattern.quote(systemCodes.get(TemplateCode.SUMMARY)), processSummary(codeValues));
        }

        if (codes.contains(TemplateCode.LAST_ABSENCE)) {
            value = value.replaceAll(Pattern.quote(systemCodes.get(TemplateCode.LAST_ABSENCE)), processLastEvent(codeValues, TemplateCode.LAST_ABSENCE));
        }

        if (codes.contains(TemplateCode.LAST_LATENESS)) {
            value = value.replaceAll(Pattern.quote(systemCodes.get(TemplateCode.LAST_LATENESS)), processLastEvent(codeValues, TemplateCode.LAST_LATENESS));
        }

        return value;
    }

    public String processSummary(HashMap<TemplateCode, Object> codeValues) {
        JsonObject events = (JsonObject) codeValues.get(TemplateCode.SUMMARY);
        List<String> keys;
        String summary = "";
        switch (mailingType) {
            case MAIL:
            case PDF:
                summary += "<div>";
                keys = new ArrayList<>(events.fieldNames());
                for (String key : keys) {
                    JsonArray eventsKey = events.getJsonArray(key);
                    if (eventsKey.isEmpty()) continue;
                    summary += getCSSStyle();
                    summary += "<div>" + I18n.getInstance().translate("massmailing.summary." + key, domain, locale) + ":</div>";
                    summary += "<table>";
                    summary += getHTMLHeader(key);
                    summary += "<tbody>";
                    for (int i = 0; i < eventsKey.size(); i++) {
                        JsonObject event = eventsKey.getJsonObject(i);
                        try {
                            String line = "<tr><td>" + DateHelper.getDateString(event.getString("display_start_date",
                                    event.getString("start_date")), DateHelper.DAY_MONTH_YEAR) + "</td>";
                            line += "<td>" + DateHelper.getTimeString(event.getString("display_start_date",
                                    event.getString("start_date")), DateHelper.SQL_FORMAT) + " - " +
                                    DateHelper.getTimeString(event.getString("display_end_date",
                                            event.getString
                                                    ("end_date")), DateHelper.SQL_FORMAT) + "</td>";
                            if (!"LATENESS".equals(key) && eventContainsReason(event)) {
                                line += "<td>" + getReasonLabel(event) + "</td>";
                                line += "<td>" + getRegularisedLabel(event) + "</td></tr>";
                            }
                            summary += line;
                        } catch (ParseException | NullPointerException e) {
                            LOGGER.error("[Massmailing@Template] Failed to generate table line", e, event.toString());
                        }
                    }
                    summary += "</tbody>";
                    summary += "</table>";
                }
                summary += "</div>";
                break;
            case SMS:
                keys = new ArrayList<>(events.fieldNames());
                for (String key : keys) {
                    JsonArray eventsKey = events.getJsonArray(key);
                    summary += "\n";
                    summary += I18n.getInstance().translate("massmailing.summary." + key, domain, locale) + ": ";
                    for (int i = 0; i < eventsKey.size(); i++) {
                        JsonObject event = eventsKey.getJsonObject(i);
                        try {
                            summary += formatSmsEvent(event);
                        } catch (ParseException | NullPointerException e) {
                            LOGGER.error("[Massmailing@Template] Failed to generate table line", e, event.toString());
                        }
                    }
                }
                break;
        }

        return summary;
    }

    private boolean eventContainsReason(JsonObject event) {
        List<Integer> reasonIds = new ArrayList<>();
        JsonArray events = event.getJsonArray("events", new JsonArray());
        for (int i = 0; i < events.size(); i++) {
            if (events.getJsonObject(i).getInteger("reason_id") != null) {
                reasonIds.add(events.getJsonObject(i).getInteger("reason_id"));
            }
        }
        return reasonIds.size() != 0;
    }

    public String processLastEvent(HashMap<TemplateCode, Object> codeValues, TemplateCode key) {
        JsonObject lastEvent = (JsonObject) codeValues.get(key);
        String template = "";
        if (lastEvent != null && mailingType == MailingType.SMS) {
            try {
                template += " " + formatSmsEvent(lastEvent);
            } catch (ParseException e) {
                LOGGER.error("[Massmailing@Template] Failed to generate line", e, lastEvent.toString());
            }
        }
        return template;
    }

    private String formatSmsEvent(JsonObject event) throws ParseException {
        String startDate = event.getString("display_start_date", event.getString("start_date"));
        String endDate = event.getString("display_end_date", event.getString("end_date"));
        String line = DateHelper.getDateString(startDate, DateHelper.DAY_MONTH_YEAR) + ": ";
        line += DateHelper.getTimeString(startDate, DateHelper.SQL_FORMAT) + " - " + DateHelper.getTimeString(endDate, DateHelper.SQL_FORMAT) + "; ";
        return line;
    }

    private String getReasonLabel(JsonObject event) {
        String multipleValues = I18n.getInstance().translate("massmailing.reasons.multiple", domain, locale);
        String noneValue = I18n.getInstance().translate("massmailing.reasons.none", domain, locale);
        JsonArray events = event.getJsonArray("events", new JsonArray());
        // Trick. In case of lateness event, event does not contains events array. In this case, we use event as an array of itself.
        if (events.isEmpty()) events = new JsonArray().add(event);
        List<String> reasons = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            reasons.add(events.getJsonObject(i).getString("reason"));
        }

        Set<String> values = new HashSet<>(reasons);
        return values.size() == 1 ? events.getJsonObject(0).getString("reason", noneValue) : multipleValues;
    }

    private String getRegularisedLabel(JsonObject event) {
        String yesLabel = I18n.getInstance().translate("massmailing.true", domain, locale);
        String noLabel = I18n.getInstance().translate("massmailing.false", domain, locale);
        String unnecessaryLabel = I18n.getInstance().translate("massmailing.unnecessary", domain, locale);
        JsonArray events = event.getJsonArray("events", new JsonArray().add(event));
        if (!allContainsReasons(events)) return noLabel;
        if (allRegularized(events)) return yesLabel;
        if (allProving(events)) return unnecessaryLabel;
        if (allContainsReasons(events) && !allRegularized(events)) return noLabel;
        return "";
    }

    private boolean allRegularized(JsonArray events) {
        boolean allRegularised = true;
        for (int i = 0; i < events.size(); i++) {
            allRegularised = allRegularised && (events.getJsonObject(i).getBoolean("counsellor_regularisation", false));
        }

        return allRegularised;
    }

    private boolean allContainsReasons(JsonArray events) {
        boolean hasReason = true;
        for (int i = 0; i < events.size(); i++) {
            hasReason = hasReason && events.getJsonObject(i).containsKey("reason");
        }

        return hasReason;
    }

    private boolean allProving(JsonArray events) {
        if (events.isEmpty()) return true;
        List<Boolean> provings = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            JsonObject event = events.getJsonObject(i);
            provings.add(event.getBoolean("proving"));
        }

        return (
                // All events contains a reason
                allContainsReasons(events)
        ) && (
                // All events are proving
                new HashSet<>(provings).size() == 1 && events.getJsonObject(0).getBoolean("proving")
        );
    }

    private String getCSSStyle() {
        return "<style>table{border-collapse:collapse}table thead tr{background:#fff;box-shadow:0 4px 6px rgba(0,0,0,.12);border-bottom:none}td{padding:5px 15px}tr{border-bottom:1px solid #ccc;background:0 0}</style>";
    }

    private String getHTMLHeader(String type) {
        String dateLabel = I18n.getInstance().translate("massmailing.date", domain, locale);
        String hoursLabel = I18n.getInstance().translate("massmailing.hours", domain, locale);
        String reasonHeader = "";
        if (!"LATENESS".equals(type)) {
            String reasonLabel = I18n.getInstance().translate("massmailing.reason", domain, locale);
            reasonHeader = "<td>" + reasonLabel + "</td>";
        }
        String regularizedLabel = I18n.getInstance().translate("massmailing.regularized", domain, locale);
        return "<thead><tr><td>" + dateLabel + "</td><td>" + hoursLabel + "</td>" + reasonHeader + "<td>" + regularizedLabel + "</td></tr></thead>";
    }
}
