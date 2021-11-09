package fr.openent.presences.enums;

public enum EventRecoveryDay {
    HOUR("HOUR"),
    HALF_DAY("HALF_DAY"),
    DAY("DAY");


    private final String type;

    EventRecoveryDay(String type) {
        this.type = type;
    }

    public String type() {
        return this.type;
    }
}