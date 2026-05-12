package fr.openent.presences.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.Events;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.text.NumberFormat;

public class RegistryBoardCSVExport extends CSVExport {
    private static final String MORNING_SYMBOL = " - ";
    private static final String AFTERNOON_SYMBOL = " | ";
    private static final String FULL_DAY_SYMBOL = " + ";

    private final JsonArray students;
    private final String month;

    public RegistryBoardCSVExport(JsonArray students, String month) {
        super();
        this.students = students;
        this.month = month;
        this.filename = "Registre - " + month + ".csv";
    }

    @Override
    public void generate() {
        int daysCount = getMonthDaysCount();

        List<String> headers = new ArrayList<>();
        headers.add("presences.registry.csv.header.student.lastName");
        headers.add("presences.registry.csv.header.student.firstName");
        headers.add("presences.registry.csv.header.classname");
        headers.add("presences.registry.csv.header.absence.count");
        headers.add("presences.registry.csv.header.absence.rate");
        for (int dayNumber = 1; dayNumber <= daysCount; dayNumber++) {
            headers.add(String.valueOf(dayNumber));
        }
        this.setHeader(headers);

        if (students == null || students.isEmpty()) return;

        JsonArray firstStudentDays = students.getJsonObject(0).getJsonArray(Field.DAYS, new JsonArray());
        appendMetadataRow(firstStudentDays, daysCount);

        List<JsonObject> sortedStudents = sortStudents(students);
        String previousClassKey = null;
        int classAbsenceEvents = 0;
        double classAbsenceUnits = 0d;
        double classSchoolDays = 0d;

        for (JsonObject student : sortedStudents) {
            String classKey = getClassNames(student);
            if (previousClassKey != null && !classKey.equals(previousClassKey)) {
                appendClassTotalRow(daysCount, previousClassKey, classAbsenceEvents, classAbsenceUnits, classSchoolDays);
                appendSeparatorRow(daysCount);
                classAbsenceEvents = 0;
                classAbsenceUnits = 0d;
                classSchoolDays = 0d;
            }

            StudentAbsenceStats stats = computeStudentAbsenceStats(student);
            appendStudentRow(student, stats, daysCount);

            classAbsenceEvents += stats.absenceEventsCount;
            classAbsenceUnits += stats.absenceUnits;
            classSchoolDays += stats.schoolDaysCount;
            previousClassKey = classKey;
        }

        if (previousClassKey != null) {
            appendClassTotalRow(daysCount, previousClassKey, classAbsenceEvents, classAbsenceUnits, classSchoolDays);
            appendSeparatorRow(daysCount);
        }
    }

    private static final class StudentAbsenceStats {
        final int absenceEventsCount;
        final double absenceUnits;
        final double schoolDaysCount;

        StudentAbsenceStats(int absenceEventsCount, double absenceUnits, double schoolDaysCount) {
            this.absenceEventsCount = absenceEventsCount;
            this.absenceUnits = absenceUnits;
            this.schoolDaysCount = schoolDaysCount;
        }
    }

    private StudentAbsenceStats computeStudentAbsenceStats(JsonObject student) {
        JsonArray days = student.getJsonArray(Field.DAYS, new JsonArray());
        double absenceUnits = 0d;
        int absenceEventsCount = 0;
        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            JsonObject day = days.getJsonObject(dayIndex);
            JsonArray events = day.getJsonArray("events", new JsonArray());
            boolean excludedDay = day.getBoolean("exclude", false);
            boolean hasMorning = day.getBoolean("hasMorning", false);
            boolean hasAfternoon = day.getBoolean("hasAfternoon", false);
            if (!excludedDay) {
                absenceEventsCount += getAbsences(events).size();
            }
            absenceUnits += getAbsenceUnits(events, excludedDay, hasMorning, hasAfternoon);
        }
        return new StudentAbsenceStats(absenceEventsCount, absenceUnits, getSchoolDaysCount(days));
    }

    private void appendStudentRow(JsonObject student, StudentAbsenceStats stats, int daysCount) {
        this.value.append(getCell(student.getString(Field.LASTNAME)));
        this.value.append(getCell(student.getString(Field.FIRSTNAME)));
        this.value.append(getCell(getClassNames(student)));
        this.value.append(getCell(String.valueOf(stats.absenceEventsCount)));
        this.value.append(getCell(getAbsenceRate(stats.absenceUnits, stats.schoolDaysCount)));

        JsonArray days = student.getJsonArray(Field.DAYS, new JsonArray());
        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            JsonObject day = days.getJsonObject(dayIndex);
            JsonArray events = day.getJsonArray("events", new JsonArray());
            boolean excludedDay = day.getBoolean("exclude", false);
            boolean hasMorning = day.getBoolean("hasMorning", false);
            boolean hasAfternoon = day.getBoolean("hasAfternoon", false);
            this.value.append(getAbsenceValue(events, excludedDay, hasMorning, hasAfternoon)).append(SEPARATOR);
        }
        for (int pad = days.size(); pad < daysCount; pad++) {
            this.value.append(getCell(""));
        }
        this.value.append(EOL);
    }

    private void appendClassTotalRow(int daysCount, String classKey, int absenceEventsSum,
                                     double absenceUnitsSum, double schoolDaysSum) {
        this.value.append(getCell(translate("presences.registry.csv.row.class.total.label")));
        this.value.append(getCell(""));
        this.value.append(getCell(classKey));
        this.value.append(getCell(String.valueOf(absenceEventsSum)));
        this.value.append(getCell(getAbsenceRate(absenceUnitsSum, schoolDaysSum)));
        for (int d = 0; d < daysCount; d++) {
            this.value.append(getCell(""));
        }
        this.value.append(EOL);
    }

    private void appendSeparatorRow(int daysCount) {
        this.value.append(getCell("---"));
        this.value.append(getCell("---"));
        this.value.append(getCell("---"));
        this.value.append(getCell("---"));
        this.value.append(getCell("---"));
        for (int d = 0; d < daysCount; d++) {
            this.value.append(getCell(""));
        }
        this.value.append(EOL);
    }

    // excludedDay   → blank (holiday, weekend with no courses)
    // hasMorning && hasAfternoon → full day: +, -, or | depending on absences
    // hasMorning only  → morning half-day: always -
    // hasAfternoon only → afternoon half-day: always |
    private String getAbsenceValue(JsonArray events, boolean excludedDay, boolean hasMorning, boolean hasAfternoon) {
        if (excludedDay) return SPACE;
        List<JsonObject> absences = getAbsences(events);
        if (absences.isEmpty()) return SPACE;
        if (!hasMorning && !hasAfternoon) return SPACE;
        if (!hasAfternoon) return MORNING_SYMBOL;   // morning-only day
        if (!hasMorning) return AFTERNOON_SYMBOL;   // afternoon-only day
        // full day: use count to decide
        if (absences.size() >= 2) return FULL_DAY_SYMBOL;
        return getHalfDayValue(absences.get(0));
    }

    private double getAbsenceUnits(JsonArray events, boolean excludedDay, boolean hasMorning, boolean hasAfternoon) {
        if (excludedDay) return 0d;
        int absencesCount = getAbsences(events).size();
        if (absencesCount == 0) return 0d;
        if (!hasMorning && !hasAfternoon) return 0d;
        if (!hasMorning || !hasAfternoon) return 0.5d;  // half-day school day
        // full day
        if (absencesCount == 1) return 0.5d;
        return 1d;
    }

    private List<JsonObject> getAbsences(JsonArray events) {
        List<JsonObject> absences = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            JsonObject event = events.getJsonObject(i);
            if (Events.ABSENCE.toString().equals(event.getString(Field.TYPE))) {
                absences.add(event);
            }
        }
        return absences;
    }

    private String getHalfDayValue(JsonObject event) {
        String startDate = event.getString("start_date", "");
        if (startDate.length() >= 13) {
            try {
                int hour = Integer.parseInt(startDate.substring(11, 13));
                return hour < 12 ? MORNING_SYMBOL : AFTERNOON_SYMBOL;
            } catch (NumberFormatException ignored) {
                return MORNING_SYMBOL;
            }
        }
        return MORNING_SYMBOL;
    }

    // Metadata row: 5 fixed cells, then one presence-symbol per day column.
    private void appendMetadataRow(JsonArray days, int daysCount) {
        this.value.append(getCell(""));  // Nom
        this.value.append(getCell(""));  // Prénom
        this.value.append(getCell(""));  // Classe
        this.value.append(getCell(""));  // Total absences
        this.value.append(getCell(""));  // % d'absence
        for (int i = 0; i < days.size(); i++) {
            JsonObject day = days.getJsonObject(i);
            boolean excluded = day.getBoolean("exclude", false);
            boolean hasMorning = day.getBoolean("hasMorning", false);
            boolean hasAfternoon = day.getBoolean("hasAfternoon", false);
            this.value.append(getCell(getDayPresenceSymbol(excluded, hasMorning, hasAfternoon)));
        }
        for (int pad = days.size(); pad < daysCount; pad++) {
            this.value.append(getCell(""));
        }
        this.value.append(EOL);
    }

    // Returns the presence symbol for a day header:
    //   +  morning + afternoon courses exist → full day
    //   -  morning courses only              → morning half-day
    //   |  afternoon courses only            → afternoon half-day
    //   (empty) excluded day (holiday, weekend with no courses)
    private String getDayPresenceSymbol(boolean excluded, boolean hasMorning, boolean hasAfternoon) {
        if (excluded) return "";
        if (hasMorning && hasAfternoon) return FULL_DAY_SYMBOL;
        if (hasMorning) return MORNING_SYMBOL;
        if (hasAfternoon) return AFTERNOON_SYMBOL;
        return "";
    }

    private String getAbsenceRate(double absenceDays, double schoolDaysCount) {
        if (schoolDaysCount <= 0d) return SPACE;
        if (absenceDays <= 0d) return "0 %";
        double percent = (absenceDays / schoolDaysCount) * 100d;
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE);
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(percent) + "%";
    }

    private double getSchoolDaysCount(JsonArray days) {
        double schoolDays = 0d;
        for (int i = 0; i < days.size(); i++) {
            JsonObject day = days.getJsonObject(i);
            if (day.getBoolean("exclude", false)) continue;
            boolean hasMorning = day.getBoolean("hasMorning", false);
            boolean hasAfternoon = day.getBoolean("hasAfternoon", false);
            if (hasMorning && hasAfternoon) schoolDays += 1d;
            else if (hasMorning || hasAfternoon) schoolDays += 0.5d;
        }
        return schoolDays;
    }

    private int getMonthDaysCount() {
        try {
            return YearMonth.parse(month).lengthOfMonth();
        } catch (Exception exception) {
            if (students != null && !students.isEmpty()) {
                return students.getJsonObject(0).getJsonArray(Field.DAYS, new JsonArray()).size();
            }
            return 0;
        }
    }

    private String getClassNames(JsonObject student) {
        JsonArray classes = student.getJsonArray(Field.CLASSES, new JsonArray());
        List<String> names = new ArrayList<>();
        for (int i = 0; i < classes.size(); i++) {
            JsonObject classObject = classes.getJsonObject(i);
            String className = classObject.getString(Field.NAME, "");
            if (!className.isEmpty()) {
                names.add(className);
            }
        }
        return String.join(", ", names);
    }

    private List<JsonObject> sortStudents(JsonArray studentsList) {
        List<JsonObject> students = new ArrayList<>();
        for (int i = 0; i < studentsList.size(); i++) {
            students.add(studentsList.getJsonObject(i));
        }
        return students.stream()
                .sorted(Comparator.comparing((JsonObject student) -> getClassNames(student), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(student -> student.getString(Field.LASTNAME, ""), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(student -> student.getString(Field.FIRSTNAME, ""), String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private String getCell(String value) {
        return (value != null ? value : SPACE) + SEPARATOR;
    }
}
