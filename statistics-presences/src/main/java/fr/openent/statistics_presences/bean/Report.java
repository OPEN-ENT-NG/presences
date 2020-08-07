package fr.openent.statistics_presences.bean;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Report {
    private Instant start;
    private Instant end;
    private String indicator;
    private boolean failureOnSave = false;
    private List<Failure> failures = new ArrayList<>();

    public Report(String indicator) {
        this.indicator = indicator;
    }

    public Report start() {
        this.start = Instant.now();
        return this;
    }

    public Report end() {
        this.end = Instant.now();
        return this;
    }

    public Report failOnSave() {
        this.failureOnSave = true;
        return this;
    }

    private Long duration() {
        return Duration.between(start, end).toMillis();
    }

    public Report fail(Failure failure) {
        this.failures.add(failure);
        return this;
    }

    public JsonObject toJSON() {
        JsonArray errors = new JsonArray(this.failures.stream().map(Failure::toString).collect(Collectors.toList()));

        return new JsonObject()
                .put("name", this.indicator)
                .put("duration", this.duration())
                .put("errorCount", this.failures.size())
                .put("saved", !this.failureOnSave)
                .put("errors", errors);
    }
}
