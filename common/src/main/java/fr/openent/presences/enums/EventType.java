package fr.openent.presences.enums;

public enum EventType {
    ABSENCE(1),
    LATENESS(2),
    DEPARTURE(3),
    REMARK(4),
    INCIDENT(5),
    FORGOTTEN_NOTEBOOK(6);


    private final Integer status;

    EventType(Integer status) {
        this.status = status;
    }

    public Integer getType() {
        return this.status;
    }
}
