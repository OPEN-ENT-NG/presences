package fr.openent.statistics_presences.bean;

public class Failure {
    private String user;
    private String structure;
    private Throwable err;

    public Failure(String user, String structure, Throwable err) {
        this.user = user;
        this.structure = structure;
        this.err = err;
    }

    public String toString() {
        return String.format("user: %s structure: %s error: %s", user, structure, err.getMessage());
    }
}
