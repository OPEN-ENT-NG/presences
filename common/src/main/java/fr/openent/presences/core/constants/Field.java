package fr.openent.presences.core.constants;

public class Field {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String STRUCTURE = "structure";
    public static final String TEACHER = "teacher";
    public static final String SEARCH_TEACHER = "searchTeacher";
    public static final String GROUP = "group";
    public static final String LIMIT = "limit";
    public static final String OFFSET = "offset";

    // Dates
    public static final String END_DATE = "end_date";
    public static final String START_DATE = "start_date";
    public static final String START = "start";
    public static final String END = "end";
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";
    public static final String DESCENDING_DATE = "descendingDate";

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

    // Multiple slots
    public static final String MULTIPLE_SLOT = "multiple_slot";
    public static final String ALLOW_MULTIPLE_SLOTS = "allow_multiple_slots";


    public static final String FORGOTTEN_REGISTERS = "forgotten_registers";


    private Field() {
        throw new IllegalStateException("Utility class");
    }
}
