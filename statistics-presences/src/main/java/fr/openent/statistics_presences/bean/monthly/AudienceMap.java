package fr.openent.statistics_presences.bean.monthly;

import fr.openent.statistics_presences.bean.Value;
import io.vertx.core.json.JsonObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AudienceMap extends Value {
    private final String key;
    private final List<Month> months;
    private final List<Student> students;
    private final Number total;

    public AudienceMap(String key, List<Month> months, List<Student> students, Number total) {
        this.key = key;
        this.months = months == null ? Collections.emptyList() : months;
        this.students = students == null ? Collections.emptyList() : students;
        this.total = total;
    }

    public String key() {
        return this.key;
    }

    public List<Student> students() {
        return this.students;
    }

    public List<Month> months() {
        return this.months;
    }

    public Number total() {
        return total;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put("audience", key)
                .put("months", months.stream().map(Month::toJson).collect(Collectors.toList()))
                .put("students", students.stream().sorted(Comparator.comparing(Student::name)).map(Student::toJson).collect(Collectors.toList()))
                .put("total", total);
    }
}
