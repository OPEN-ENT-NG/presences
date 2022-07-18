package fr.openent.presences.enums;

import java.util.Arrays;

public enum SettingsFieldEnum {
    ALERT_ABSENCE_THRESHOLD("alert_absence_threshold"),
    ALERT_LATENESS_THRESHOLD("alert_lateness_threshold"),
    ALERT_INCIDENT_THRESHOLD("alert_incident_threshold"),
    ALERT_FORGOTTEN_NOTEBOOK_THRESHOLD("alert_forgotten_notebook_threshold"),
    EVENT_RECOVERY_METHOD("event_recovery_method"),
    EXCLUDE_ALERT_ABSENCE_NO_REASON("exclude_alert_absence_no_reason"),
    EXCLUDE_ALERT_LATENESS_NO_REASON("exclude_alert_lateness_no_reason"),
    EXCLUDE_ALERT_FORGOTTEN_NOTEBOOK("exclude_alert_forgotten_notebook"),
    INITIALIZED("initialized");

    private final String value;

    SettingsFieldEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Get value from key/field
     *
     * @param field key of enum
     * @return value of enum
     */
    public static String getSettingsField(String field) {
        SettingsFieldEnum settingsFieldValue = Arrays.stream(SettingsFieldEnum.values())
                .filter(settingsFieldEnum -> settingsFieldEnum.getValue().equals(field))
                .findFirst()
                .orElse(null);
        return settingsFieldValue == null ? null : settingsFieldValue.getValue();
    }
}
