package fr.openent.presences.core.constants;

public class Field {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String STRUCTURE = "structure";

    // Dates
    public static final String END_DATE = "end_date";
    public static final String START_DATE = "start_date";

    // School
    public static final String SCHOOL_YEAR = "schoolYear";
    public static final String SPLITTED_DATE_SCHOOL_YEAR = "separatedDateBySchoolYear";

    // File
    public static final String FILES = "files";
    public static final String CONTENT = "content";
    public static final String CONTENTS = "contents";

    // Reason
    public static final String REASON = "reason";

    // i18n
    public static final String LOCALE = "locale";
    public static final String DOMAIN = "domain";


    private Field() {
        throw new IllegalStateException("Utility class");
    }
}
