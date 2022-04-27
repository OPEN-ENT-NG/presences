package fr.openent.presences.enums;

import java.util.Arrays;

public enum EventRecoveryMethodEnum {
    DAY("DAY"),
    HOUR("HOUR"),
    HALF_DAY("HALF_DAY");

    private final String value;

    EventRecoveryMethodEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EventRecoveryMethodEnum getInstanceFromString(String value) {
        return Arrays.stream(EventRecoveryMethodEnum.values())
                .filter(eventRecoveryMethodEnum -> eventRecoveryMethodEnum.value.equals(value))
                .findFirst()
                .orElse(null);
    }
}
