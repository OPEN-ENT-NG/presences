package fr.openent.statistics_presences.bean.monthly;

import fr.openent.statistics_presences.bean.Value;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class Student extends Value {
    private final String name;
    private final String id;
    private final List<Month> months;
    private Number total;

    public Student(String name, String id, List<Month> months) {
        this.name = name;
        this.id = id;
        this.months = months;
        this.total = 0;
    }

    public String name() {
        return this.name;
    }

    public String id() {
        return this.id;
    }

    public List<Month> months() {
        return this.months;
    }

    public Number total() {
        return total;
    }

    public Student setTotal(Number total) {
        this.total = total;
        return this;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put("name", this.name)
                .put("id", this.id)
                .put("months", months.stream().map(Month::toJson).collect(Collectors.toList()))
                .put("total", total);
    }
}
