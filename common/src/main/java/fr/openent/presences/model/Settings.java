package fr.openent.presences.model;

import fr.openent.presences.core.constants.Field;
import io.vertx.core.json.JsonObject;

public class Settings {

    private final Number alertAbsenceThreshold;
    private final Number alertLatenessThreshold;
    private final Number alertIncidentThreshold;
    private final Number alertForgottenThreshold;
    private final String recoveryMethod;
    private final Boolean multipleSlot;
    private String endOfHalfDayTimeSlot;

    public Settings(JsonObject settings) {
        this.alertAbsenceThreshold = settings.getInteger(Field.ALERT_ABSENCE_THRESHOLD);
        this.alertLatenessThreshold = settings.getInteger(Field.ALERT_LATENESS_THRESHOLD);
        this.alertIncidentThreshold = settings.getInteger(Field.ALERT_INCIDENT_THRESHOLD);
        this.alertForgottenThreshold = settings.getInteger(Field.ALERT_FORGOTTEN_NOTEBOOK_THRESHOLD);
        this.recoveryMethod = settings.getString(Field.EVENT_RECOVERY_METHOD);
        this.multipleSlot = settings.getBoolean(Field.ALLOW_MULTIPLE_SLOTS);
    }

    public Settings(Number alertAbsenceThreshold, Number alertLatenessThreshold, Number alertIncidentThreshold, Number alertForgottenThreshold) {
        this.alertAbsenceThreshold = alertAbsenceThreshold;
        this.alertLatenessThreshold = alertLatenessThreshold;
        this.alertIncidentThreshold = alertIncidentThreshold;
        this.alertForgottenThreshold = alertForgottenThreshold;
        this.recoveryMethod = null;
        this.multipleSlot = null;
    }

    public Number alertAbsenceThreshold() {
        return alertAbsenceThreshold;
    }

    public Number alertLatenessThreshold() {
        return alertLatenessThreshold;
    }

    public Number alertIncidentThreshold() {
        return alertIncidentThreshold;
    }

    public Number alertForgottenThreshold() {
        return alertForgottenThreshold;
    }

    public String recoveryMethod() {
        return recoveryMethod;
    }

    public Boolean multipleSlot() {
        return multipleSlot;
    }

    public Settings setEndOfHalfDayTimeSlot(String endOfHalfDayTimeSlot) {
        this.endOfHalfDayTimeSlot = endOfHalfDayTimeSlot;
        return this;
    }

    public String endOfHalfDayTimeSlot() {
        return endOfHalfDayTimeSlot;
    }

}



