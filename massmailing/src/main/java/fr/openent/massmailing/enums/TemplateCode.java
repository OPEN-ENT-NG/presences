package fr.openent.massmailing.enums;

public enum TemplateCode {
    CHILD_NAME("massmailing.codes.child.name"),
    CLASS_NAME("massmailing.codes.class.name"),
    ABSENCE_NUMBER("massmailing.codes.absence.number"),
    LATENESS_NUMBER("massmailing.codes.lateness.number"),
    SUMMARY("massmailing.codes.summary"),
    LAST_ABSENCE("massmailing.codes.last.absence"),
    LAST_LATENESS("massmailing.codes.last.lateness"),

    PUNISHMENT_TYPE("massmailing.codes.punishment.type"),
    RESPONSIBLE("massmailing.codes.punishment.responsible"),
    SANCTION_TYPE("massmailing.codes.sanction.type"),
    PUNISHMENT_DESCRIPTION("massmailing.codes.punishment.description"),
    PUNISHMENT_DATE("massmailing.codes.punishment.date"),
    DAY_NUMBER("massmailing.codes.punishment.day.number"),
    PUNISHMENT_SUMMARY("massmailing.codes.punishment.summary"),
    DATE("massmailing.codes.today.date"),
    ADDRESS("massmailing.codes.address.legal.responsible"),
    ZIPCODE_CITY("massmailing.codes.zipcode.city.legal.responsible");

    private final String key;

    TemplateCode(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
