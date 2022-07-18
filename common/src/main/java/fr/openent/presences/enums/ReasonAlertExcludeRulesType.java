package fr.openent.presences.enums;

import java.util.Arrays;

public enum ReasonAlertExcludeRulesType {
    REGULARIZED(1, "excludeAlertRegularised"),
    UNREGULARIZED(2, "excludeAlertNoRegularised"),
    LATENESS(3, "excludeAlertLateness");

    private final int value;
    private final String jsonField;

    ReasonAlertExcludeRulesType(int value, String jsonField) {
        this.value = value;
        this.jsonField = jsonField;
    }

    public int getValue() {
        return value;
    }

    public String getJsonField() {
        return jsonField;
    }
}
