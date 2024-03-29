package fr.openent.presences.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EventTypeEnum {
    ABSENCE(1),
    LATENESS(2),
    DEPARTURE(3),
    REMARK(4),
    INCIDENT(5),
    FORGOTTEN_NOTEBOOK(6),
    PUNISHMENT(7),
    SANCTION(8);

    private final Integer type_id;
    private static Map<Integer, EventTypeEnum> eventTypeMap;

    EventTypeEnum(Integer type_id) {
        this.type_id = type_id;
    }

    public Integer getType() {
        return this.type_id;
    }

    public static EventTypeEnum getEventType(Integer typeId) {
        if (eventTypeMap == null) {
            eventTypeMap = Arrays.stream(values()).collect(Collectors.toMap(EventTypeEnum::getType, Function.identity()));
        }
        return eventTypeMap.get(typeId);
    }
}
