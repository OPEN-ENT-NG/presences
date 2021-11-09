package fr.openent.statistics_presences.filter;

public class FilterField {
    public static final String START = "start";
    public static final String END = "end";
    public static final String TYPES = "types";
    public static final String AUDIENCES = "audiences";
    public static final String USERS = "users";
    public static final String FILTERS = "filters";
    public static final String REASONS = "reasons";
    public static final String PUNISHMENT_TYPES = "punishmentTypes";
    public static final String SANCTION_TYPES = "sanctionTypes";
    public static final String EXPORT_OPTION = "export_option";
    public static final String FROM = "FROM";
    public static final String TO = "TO";
    public static final String HOUR_DETAILS = "HOUR_DETAIL";
    public static final String TOTAL = "TOTAL";
    public static final String RATE = "rate";

    private FilterField() {
        throw new IllegalStateException("Utility class");
    }
}
