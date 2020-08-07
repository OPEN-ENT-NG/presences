package fr.openent.statistics_presences.bean;

public class Audience {
    private String id;
    private String name;

    public Audience(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }
}
