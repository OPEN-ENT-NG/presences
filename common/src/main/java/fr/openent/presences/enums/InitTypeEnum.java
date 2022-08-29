package fr.openent.presences.enums;

import java.util.Arrays;

public enum InitTypeEnum {
    ONE_D(1),
    TWO_D(2);

    private final int value;

    InitTypeEnum(int value) {
        this.value = value;
    }

    public static InitTypeEnum getInitType(int value) {
        return Arrays.stream(InitTypeEnum.values()).filter(initTypeEnum -> initTypeEnum.value == value)
                .findFirst()
                .orElse(InitTypeEnum.TWO_D);
    }

    public int getValue() {
        return value;
    }
}
