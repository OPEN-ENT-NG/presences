package fr.openent.statistics_presences.bean;

import java.io.Serializable;

public class Failure implements Serializable {
    private static final long serialVersionUID = 1L;
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
