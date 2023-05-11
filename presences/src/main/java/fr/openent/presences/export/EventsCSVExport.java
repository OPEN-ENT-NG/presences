package fr.openent.presences.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.model.Event.Event;
import fr.wseduc.webutils.I18n;

import java.util.*;

public class EventsCSVExport extends CSVExport {

    private final List<Event> events;
    private final String locale;
    private final String domain;

    public EventsCSVExport(List<Event> events, String domain, String locale) {
        super();
        this.events = events;
        this.domain = domain;
        this.locale = locale;
        String date = DateHelper.getDateString(new Date(), DateHelper.MONGO_FORMAT);
        this.filename = String.format("%s - %s.csv", "export_événements", date);
    }

    @Override
    public void generate() {
        for (Event event : events) {
            this.value.append(getLine(event));
        }
    }

    private String getLine(Event event) {
        String line = event.getStudent().getLastName() + SEPARATOR;
        line += event.getStudent().getFirstName() + SEPARATOR;
        line += SPACE + event.getStudent().getClassName() + SEPARATOR;
        line += I18n.getInstance().translate(event.getEventType().getLabel(), domain, locale) + SEPARATOR;
        line += getReason(event) + SEPARATOR;
        line += event.getOwner().getName() + SEPARATOR;
        line += DateHelper.getDateString(event.getStartDate(), DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
        line += getEventTime(event) + SEPARATOR;
        line += event.getComment() + SEPARATOR;
        line += getCounsellorRegularisationState(event) + SEPARATOR;
        line += event.getId() + SEPARATOR;
        return line + EOL;
    }

    private String getEventTime(Event event) {
        String startTime = DateHelper.fetchTimeString(event.getStartDate(), DateHelper.SQL_FORMAT);
        String endTime = DateHelper.fetchTimeString(event.getEndDate(), DateHelper.SQL_FORMAT);
        return startTime + " - " + endTime;
    }

    private String getReason(Event event) {
        if (event.getReason().getLabel() != null) {
            return event.getReason().getLabel();
        } else {
            return "";
        }
    }

    private String getCounsellorRegularisationState(Event event) {
        if (event.isCounsellorRegularisation()) {
            return I18n.getInstance().translate("presences.exemptions.csv.attendance.true", domain, locale);
        } else {
            return I18n.getInstance().translate("presences.exemptions.csv.attendance.false", domain, locale);
        }
    }
}
