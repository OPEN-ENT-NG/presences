package fr.openent.statistics_presences.indicator.export;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.statistics_presences.bean.monthly.AudienceMap;
import fr.openent.statistics_presences.bean.monthly.Month;
import fr.openent.statistics_presences.bean.monthly.Statistic;
import fr.openent.statistics_presences.bean.monthly.Student;
import fr.openent.statistics_presences.constants.ExportOptions;
import fr.openent.statistics_presences.filter.Filter;
import fr.openent.statistics_presences.helper.MonthlyHelper;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.json.JsonObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Monthly extends IndicatorExport {
    private static final String FILENAME = "statistics-presences.indicator.Monthly.export.filename";
    private static final String TYPE = "statistics-presences.indicator.filter.type.";

    private final Map<String, Number> monthsMap;
    private final List<AudienceMap> audienceMaps;
    private final List<JsonObject> studentsList;
    private final LocalDate startAt;
    private final long numOfMonthsBetween;

    public Monthly(Filter filter, List<JsonObject> values) {
        super(filter, values);

        startAt = LocalDate.parse(DateHelper.getDateString(filter.start(), DateHelper.YEAR_MONTH_DAY));

        // adding 1 to include date itself (e.g filtering start(2021/01) and end(2021/01)
        // will return 1 instead of 0 in order to still "count";
        // then start(2021/01) and end(2021/02) will return 2, start(2021/01) and end(2021/03) will return 3 etc...
        numOfMonthsBetween = IntStream
                .range(0, (int) DateHelper.distinctMonthsNumberSeparating(filter.start(), filter.end()) + 1)
                .toArray().length;
        monthsMap = MonthlyHelper.initMonthsMap(startAt, numOfMonthsBetween);
        audienceMaps = getAudienceMap(values);
        studentsList = formatAudienceMapsIntoListStudents(filter.exportOption());
    }

    @Override
    public void generate() {
        this.setHeaderType(filter.types());
        this.setHeader(filter.types());
        this.setFilename(filename());
        studentsList.forEach(student -> value.append(getLine(student)));
    }

    /**
     * create new list of JsonObject for listing all students (using audienceMap) {@link List<AudienceMap>}
     *
     * @return {@link List<JsonObject>} Future of new list
     */
    private List<JsonObject> formatAudienceMapsIntoListStudents(String exportOption) {
        List<JsonObject> studentList = new ArrayList<>();
        audienceMaps.forEach(audienceMap -> {
            if (!ExportOptions.AUDIENCES.equals(exportOption)) {
                audienceMap.students().forEach(studentItem ->
                        studentList.add(new JsonObject()
                                .put("audience", audienceMap.key())
                                .put("name", studentItem.name())
                                .put("months", studentItem.months().stream().map(Month::toJson).collect(Collectors.toList()))
                                .put("total", studentItem.total())));
            }

            if (!audienceMap.students().isEmpty()) {
                studentList.add(new JsonObject()
                        .put("audience", audienceMap.key())
                        .put("name", "TOTAL")
                        .put("months", audienceMap.months().stream().map(Month::toJson).collect(Collectors.toList()))
                        .put("total", audienceMap.total()));
            }
        });

        return studentList;
    }

    /**
     * create new list of audience map using values as a List of Jsonobject {@link List<JsonObject>}
     *
     * @param values {@link List<JsonObject>}
     * @return {@link List<AudienceMap>} Future of new list
     */
    @SuppressWarnings("unchecked")
    private List<AudienceMap> getAudienceMap(List<JsonObject> values) {
        return values.stream().map(value -> {
            String audience = value.getString("audience", "");
            Number totalInAudience = value.getInteger("total", 0);
            List<Month> months = getMonths(value);
            List<Student> students = ((List<JsonObject>) value.getJsonArray("students")
                    .getList())
                    .stream()
                    .map(this::getStudent)
                    .collect(Collectors.toList());

            return new AudienceMap(audience, months, students, totalInAudience);
        }).collect(Collectors.toList());
    }

    /**
     * create new Object of student using student as a JsonObject {@link JsonObject}
     *
     * @param student {@link JsonObject}
     * @return {@link Student} Future of new object
     */
    private Student getStudent(JsonObject student) {
        String name = student.getString("name", "");
        String id = student.getString("id", "");
        List<Month> monthStudent = MonthlyHelper.concatMonths(
                getStudentMonth(student), initListMonths(startAt, numOfMonthsBetween))
                .stream()
                .sorted(Comparator.comparing(Month::key))
                .collect(Collectors.toList());
        Number total = student.getInteger("total", 0);
        return new Student(name, id, monthStudent).setTotal(total);
    }

    /**
     * create new List of month for student using student as a JsonObject {@link JsonObject}
     *
     * @param student {@link JsonObject}
     * @return {@link List<Month>} Future of new list
     */
    @SuppressWarnings("unchecked")
    private List<Month> getStudentMonth(JsonObject student) {
        return ((List<JsonObject>) student.getJsonArray("months")
                .getList())
                .stream()
                .map(this::getMonth)
                .collect(Collectors.toList());
    }

    /**
     * create new List of month using value as a JsonObject {@link JsonObject}
     *
     * @param value {@link JsonObject}
     * @return {@link List<Month>} Future of new list
     */
    @SuppressWarnings("unchecked")
    private List<Month> getMonths(JsonObject value) {
        return ((List<JsonObject>) value.getJsonArray("months")
                .getList())
                .stream()
                .map(this::getMonth)
                .collect(Collectors.toList());
    }

    /**
     * create new month using month as a JsonObject {@link JsonObject}
     *
     * @param month {@link JsonObject}
     * @return {@link List<Month>} Future of new Object
     */
    private Month getMonth(JsonObject month) {
        String monthKey = String.join("", month.fieldNames());
        JsonObject monthObject = month.getJsonObject(monthKey, new JsonObject());
        Integer count = monthObject.getInteger("count", 0);
        Integer slots = monthObject.getInteger("slots", 0);
        return new Month(monthKey, new Statistic(count, slots));
    }

    /**
     * create new list of months
     *
     * @param startAt            start date {@link LocalDate}
     * @param numOfMonthsBetween number of distance month between
     * @return {@link List<Month>} Future of new list of month
     */
    private List<Month> initListMonths(LocalDate startAt, long numOfMonthsBetween) {
        return IntStream.iterate(0, i -> i + 1)
                .limit(numOfMonthsBetween)
                .mapToObj(i -> {
                    LocalDate date = startAt.plusMonths(i);
                    return new Month(date.format(DateTimeFormatter.ofPattern(DateHelper.YEAR_MONTH)), new Statistic(0, 0));
                })
                .collect(Collectors.toList());
    }

    private String filename() {
        String name = I18n.getInstance().translate(FILENAME, Renders.getHost(request), I18n.acceptLanguage(request));
        String date = DateHelper.getDateString(new Date(), DateHelper.MONGO_FORMAT);
        return String.format("%s - %s.csv", name, date);
    }

    @Override
    public void setHeader(List<String> types) {
        List<String> exportHeaders = new ArrayList<>();
        exportHeaders.add("statistics-presences.classes");
        exportHeaders.add("statistics-presences.students");
        monthsMap.forEach((k, v) -> exportHeaders.add(DateHelper.getDateString(k,
                DateHelper.YEAR_MONTH, DateHelper.SHORT_MONTH, Locale.FRANCE)));
        exportHeaders.add("statistics-presences.indicator.filter.type.ABSENCE_TOTAL.abbr.totale");

        super.setHeader(exportHeaders);
    }

    private void setHeaderType(List<String> types) {
        List<String> exportHeaders = new ArrayList<>();
        exportHeaders.add("statistics-presences.filters.type");
        exportHeaders.add(types.stream()
                .map(type -> I18n.getInstance().translate(TYPE + type, Renders.getHost(request), I18n.acceptLanguage(request)))
                .collect(Collectors.joining(",")));
        super.setHeader(exportHeaders);
    }

    @SuppressWarnings("unchecked")
    private String getLine(JsonObject value) {
        StringBuilder line = new StringBuilder();
        line.append(value.getString("audience")).append(SEPARATOR)
                .append(value.getString("name")).append(SEPARATOR);
        ((List<JsonObject>) value.getJsonArray("months").getList()).forEach(month ->
                line.append(month.getJsonObject(String.join("", month.fieldNames())).getInteger("count", 0)).append(SEPARATOR));
        line.append(value.getInteger("total", 0)).append(SEPARATOR);
        return line.append(EOL).toString();
    }
}
