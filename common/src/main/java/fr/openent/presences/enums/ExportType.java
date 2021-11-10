package fr.openent.presences.enums;

public enum ExportType {
    CSV("CSV"),
    PDF("PDF");


    private final String type;

    ExportType(String type) {
        this.type = type;
    }

    public String type() {
        return this.type;
    }
}
