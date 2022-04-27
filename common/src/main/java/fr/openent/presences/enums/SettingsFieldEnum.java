package fr.openent.presences.enums;

import java.util.Arrays;

public enum SettingsFieldEnum {
    ALERT_ABSENCE_THRESHOLD("alert_absence_threshold"),
    ALERT_LATENESS_THRESHOLD("alert_lateness_threshold"),
    ALERT_INCIDENT_THRESHOLD("alert_incident_threshold"),
    ALERT_FORGOTTEN_NOTEBOOK_THRESHOLD("alert_forgotten_notebook_threshold"),
    EVENT_RECOVERY_METHOD("event_recovery_method"),
    INITIALIZED("initialized");

    private final String value;

    SettingsFieldEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    public static String getSettingsField(String field) {
        SettingsFieldEnum settingsFieldValue = Arrays.stream(SettingsFieldEnum.values()).filter(settingsFieldEnum -> settingsFieldEnum.getValue().equals(field)).findFirst().orElse(null);
        return settingsFieldValue == null ? null : settingsFieldValue.getValue();
    }
}
