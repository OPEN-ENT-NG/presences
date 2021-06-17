package fr.openent.statistics_presences.helper;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.statistics_presences.bean.monthly.Month;
import fr.openent.statistics_presences.bean.monthly.Statistic;
import fr.openent.statistics_presences.bean.monthly.Student;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MonthlyHelper {

    private MonthlyHelper() {
        throw new IllegalStateException("Monthly Helper class");
    }

    /**
     * concat months to make one List of months
     *
     * @param months1 first list of month
     * @param months2 second list of month
     * @return List of month corresponding to 2 merged list of months
     */
    public static List<Month> concatMonths(List<Month> months1, List<Month> months2) {
        return Stream.of(months1, months2)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(
                        Month::key,
                        Month::statistic,
                        MonthlyHelper::sumStatisticValues2
                ))
                .entrySet().stream()
                .map(month -> new Month(month.getKey(), month.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Sum statistic value (is generally used as 3rd arguments from a Collectors.toMap)
     *
     * @param stat1 first Statistic
     * @param stat2 second Statistic
     * @return new statistic with the sum of these 2 stats fetched
     */
    public static Statistic sumStatisticValues2(Statistic stat1, Statistic stat2) {
        return new Statistic(
                stat1.count() + stat2.count(),
                stat1.slots() + stat2.slots()
        );
    }

    /**
     * init all months based on date and the number of months between
     * (e.g 2020-12 and 5 months between will make a list from [2020-12, 2021-01, 2021-02... 2021-04])
     *
     * @param startAt            startAt date
     * @param numOfMonthsBetween numbe of month between
     * @return List of month corresponding to 2 merged list of months
     */
    public static List<Month> initMonths(LocalDate startAt, long numOfMonthsBetween) {
        return IntStream.iterate(0, i -> i + 1)
                .limit(numOfMonthsBetween)
                .mapToObj(i -> {
                    LocalDate date = startAt.plusMonths(i);
                    return new Month(date.format(DateTimeFormatter.ofPattern(DateHelper.YEAR_MONTH)), new Statistic(0, 0));
                })
                .collect(Collectors.toList());
    }

    /**
     * init all months based on date and the number of months between
     * (e.g 2020-12 and 5 months between will make a list from [2020-12, 2021-01, 2021-02... 2021-04])
     * <p>
     * Difference is this will be a map with string as key (month) and index as value (using Atomic Integer to not lost context
     * between stream/thread)
     *
     * @param startAt            startAt date
     * @param numOfMonthsBetween numbe of month between
     * @return List of month corresponding to 2 merged list of months
     */
    public static Map<String, Number> initMonthsMap(LocalDate startAt, long numOfMonthsBetween) {
        AtomicInteger index = new AtomicInteger();
        return IntStream.iterate(0, i -> i + 1)
                .limit(numOfMonthsBetween)
                .mapToObj(i -> {
                    LocalDate date = startAt.plusMonths(i);
                    return date.format(DateTimeFormatter.ofPattern(DateHelper.YEAR_MONTH));
                })
                .sorted(Comparator.comparing(Function.identity()))
                .collect(Collectors.toList())
                .stream()
                .collect(Collectors.toMap(Function.identity(), s -> index.getAndIncrement(),
                        (month1, month2) -> month1, LinkedHashMap::new));
    }

    /**
     * Loop through aggregation pipeline result and store values in a Map. The Map contains audience name
     * identifier as key, containing a map of month for value. This map of month contains the mont (String) as key
     * and JsonObject as value. Those JsonObjects are statistics objects extracted
     * by the aggregation pipeline.
     *
     * @param result Aggregation pipeline result
     * @return Map containing user identifier as key and an List of statistics as value
     * {@link Map} of {@link String} as key and {@link List<Month>} as value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, List<Month>> mapPipelineResultByKeyAndMonth(JsonArray result) {
        return ((List<JsonObject>) result.getList()).stream()
                .collect(Collectors.toMap(
                        stat -> stat.getString("class_name"),
                        MonthlyHelper::mapObjectToMonthsList,
                        MonthlyHelper::concatMonths
                ));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, List<Student>> mapPipelineResultByAudienceAndStudent(JsonArray result) {
        return ((List<JsonObject>) result.getList()).stream()
                .collect(Collectors.toMap(
                        stat -> stat.getString("class_name"),
                        MonthlyHelper::mapObjectToStudentList,
                        MonthlyHelper::concatStudents
                ));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, List<Month>> mapPipelineResultEventByGroup(JsonArray result, String type) {
        return ((List<JsonObject>) result.getList()).stream()
                .collect(Collectors.toMap(
                        stat -> stat.getString(type),
                        MonthlyHelper::mapObjectToMonthsList,
                        MonthlyHelper::concatMonths
                ));
    }

    public static List<Student> concatStudents(List<Student> students1, List<Student> students2) {
        return new ArrayList<>(
                Stream.of(students1, students2)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toMap(
                                Student::id,
                                student -> student,
                                (stud1, stud2) -> new Student(stud1.name(), stud1.id(), MonthlyHelper.concatMonths(stud1.months(), stud2.months())
                                        .stream()
                                        .sorted(Comparator.comparing(Month::key))
                                        .collect(Collectors.toList()))
                        ))
                        .values()
        );
    }

    public static List<Student> mapObjectToStudentList(JsonObject stat) {
        return Collections.singletonList(new Student(stat.getString("name"), stat.getString("user"), mapObjectToMonthsList(stat)));
    }

    public static List<Month> mapObjectToMonthsList(JsonObject stat) {
        return Collections.singletonList(new Month(stat.getString("month"), setStatisticValues2(stat)));
    }

    public static Statistic setStatisticValues2(JsonObject stat) {
        return new Statistic(
                stat.getInteger("count", 0),
                stat.getInteger("slots", 0)
        );
    }

}
