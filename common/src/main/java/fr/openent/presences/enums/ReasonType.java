package fr.openent.presences.enums;

import java.util.Arrays;

public enum ReasonType {
    ALL(0),
    ABSENCE(1),
    LATENESS(2);

    private final int value;

    ReasonType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    static public ReasonType getReasonTypeFromValue(int value) {
        return Arrays.stream(ReasonType.values())
                .filter(reasonType -> reasonType.getValue() == value && !reasonType.equals(ALL)).findFirst().get();
    }
}
