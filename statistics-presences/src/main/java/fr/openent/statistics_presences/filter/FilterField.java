package fr.openent.statistics_presences.filter;

public class FilterField {
    public static final String TYPES = "types";
    public static final String AUDIENCES = "audiences";
    public static final String USERS = "users";
    public static final String FILTERS = "filters";
    public static final String REASONS = "reasons";
    public static final String FROM = "FROM";
    public static final String TO = "TO";
    public static final String HOUR_DETAILS = "HOUR_DETAIL";

    private FilterField() {
        throw new IllegalStateException("Utility class");
    }
}
