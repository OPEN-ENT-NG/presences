package fr.openent.presences.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.model.Event.Event;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;

import java.util.List;

public class EventsCSVExport extends CSVExport {

    private final List<Event> events;

    public EventsCSVExport(List<Event> events) {
        super();
        this.events = events;
        this.filename = "presences.events.csv.filename";
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
        line += event.getStudent().getClassName() + SEPARATOR;
        line += I18n.getInstance().translate(event.getEventType().getLabel(),
                Renders.getHost(this.request), I18n.acceptLanguage(this.request)) + SEPARATOR;
        line += getReason(event) + SEPARATOR;
        line += event.getOwner().getName() + SEPARATOR;
        line += DateHelper.getDateString(event.getStartDate(), DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
        line += getEventTime(event) + SEPARATOR;
        line += event.getComment() + SEPARATOR;
        line += getCounsellorRegularisationState(event) + SEPARATOR;
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
            return I18n.getInstance().translate("presences.exemptions.csv.attendance.true",
                    Renders.getHost(this.request), I18n.acceptLanguage(this.request));
        } else {
            return I18n.getInstance().translate("presences.exemptions.csv.attendance.false",
                    Renders.getHost(this.request), I18n.acceptLanguage(this.request));
        }
    }

}
