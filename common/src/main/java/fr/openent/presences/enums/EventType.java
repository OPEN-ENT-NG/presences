package fr.openent.presences.enums;

public enum EventType {
    ABSENCE(1),
    LATENESS(2),
    DEPARTURE(3),
    REMARK(4),
    INCIDENT(5),
    FORGOTTEN_NOTEBOOK(6),
    PUNISHMENT(7),
    SANCTION(8);


    private final Integer type_id;

    EventType(Integer type_id) {
        this.type_id = type_id;
    }

    public Integer getType() {
        return this.type_id;
    }
}
