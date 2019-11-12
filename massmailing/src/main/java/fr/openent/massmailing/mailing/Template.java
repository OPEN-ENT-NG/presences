package fr.openent.massmailing.mailing;

import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.TemplateCode;
import fr.openent.massmailing.service.SettingsService;
import fr.openent.massmailing.service.impl.DefaultSettingsService;
import fr.openent.presences.common.helper.DateHelper;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class Template {
    private Logger LOGGER = LoggerFactory.getLogger(Template.class);
    private SettingsService settingsService = new DefaultSettingsService();
    private HashMap<TemplateCode, String> systemCodes = new HashMap<>();
    private MailingType mailingType;
    private Integer id;
    private String structure;
    private String content;
    private String locale;
    private String domain;

    public Template(MailingType mailingType, Integer templateIdentifier, String structureId) {
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

            handler.handle(event.right());
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
            if (TemplateCode.SUMMARY.equals(code)) continue;
            try {
                value = value.replaceAll(Pattern.quote(systemCodes.get(code)), codeValues.get(code).toString());
            } catch (Exception e) {
                LOGGER.error("[Massmailing@Template] Failed to replace code for code : " + code);
            }
        }

        if (codes.contains(TemplateCode.SUMMARY)) {
            value = value.replaceAll(Pattern.quote(systemCodes.get(TemplateCode.SUMMARY)), processSummary(codeValues));
        }

        return value;
    }

    public String processSummary(HashMap<TemplateCode, Object> codeValues) {
        JsonObject events = (JsonObject) codeValues.get(TemplateCode.SUMMARY);

        String summary = "";
        switch (mailingType) {
            case MAIL:
            case PDF:
                summary += "<div>";
                List<String> keys = new ArrayList<>(events.fieldNames());
                for (String key : keys) {
                    JsonArray eventsKey = events.getJsonArray(key);
                    if (eventsKey.isEmpty()) continue;
                    summary += getCSSStyle();
                    summary += "<div>" + I18n.getInstance().translate("massmailing.summary." + key, domain, locale) + ":</div>";
                    summary += "<table>";
                    summary += getHTMLHeader();
                    summary += "<tbody>";
                    for (int i = 0; i < eventsKey.size(); i++) {
                        JsonObject event = eventsKey.getJsonObject(i);
                        try {
                            String line = "<tr><td>" + DateHelper.getDateString(event.getString("start_date"), DateHelper.DAY_MONTH_YEAR) + "</td>";
                            line += "<td>" + DateHelper.getTimeString(event.getString("start_date"), DateHelper.SQL_FORMAT) + " - " + DateHelper.getTimeString(event.getString("end_date"), DateHelper.SQL_FORMAT) + "</td>";
                            line += "<td>" + event.getString("reason", "") + "</td>";
                            line += "<td>" + getRegularisedLabel(event) + "</td></tr>";
                            summary += line;
                        } catch (ParseException | NullPointerException e) {
                            LOGGER.error("[Massmailing@Template] Failed to generate table line", e, event.toString());
                        }
                    }
                    summary += "/tbody>";
                    summary += "</table>";
                }
                summary += "</div>";
                break;
        }

        return summary;
    }

    private String getRegularisedLabel(JsonObject event) {
        String yesLabel = I18n.getInstance().translate("massmailing.true", domain, locale);
        String noLabel = I18n.getInstance().translate("massmailing.false", domain, locale);
        String unnecessaryLabel = I18n.getInstance().translate("massmailing.unnecessary", domain, locale);
        if (!event.containsKey("reason")) return noLabel;
        if (event.containsKey("counsellor_regularisation") && event.getBoolean("counsellor_regularisation"))
            return yesLabel;
        if (event.containsKey("reason") && !event.getBoolean("counsellor_regularisation")) return noLabel;
        if (event.containsKey("reason") && event.getBoolean("proving")) return unnecessaryLabel;
        return "";
    }

    private String getCSSStyle() {
        return "<style>table{border-collapse:collapse}table thead tr{background:#fff;box-shadow:0 4px 6px rgba(0,0,0,.12);border-bottom:none}td{padding:5px 15px}tr{border-bottom:1px solid #ccc;background:0 0}</style>";
    }

    private String getHTMLHeader() {
        String dateLabel = I18n.getInstance().translate("massmailing.date", domain, locale);
        String hoursLabel = I18n.getInstance().translate("massmailing.hours", domain, locale);
        String reasonLabel = I18n.getInstance().translate("massmailing.reason", domain, locale);
        String regularizedLabel = I18n.getInstance().translate("massmailing.regularized", domain, locale);
        String header = "<thead><tr><td>" + dateLabel + "</td><td>" + hoursLabel + "</td><td>" + reasonLabel + "</td><td>" + regularizedLabel + "</td></tr></thead>";
        return header;
    }
}
