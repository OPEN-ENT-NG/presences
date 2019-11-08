package fr.openent.massmailing.enums;

public enum TemplateCode {
    CHILD_NAME("massmailing.codes.child.name"),
    CLASS_NAME("massmailing.codes.class.name"),
    ABSENCE_NUMBER("massmailing.codes.absence.number"),
    LATENESS_NUMBER("massmailing.codes.lateness.number"),
    SUMMARY("massmailing.codes.summary");

    private final String key;

    TemplateCode(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
