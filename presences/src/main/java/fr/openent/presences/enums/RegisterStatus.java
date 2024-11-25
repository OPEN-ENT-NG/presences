package fr.openent.presences.enums;

public enum RegisterStatus {
    TODO(1),
    IN_PROGRESS(2),
    DONE(3);

    private final Integer status;

    RegisterStatus(Integer status) {
        this.status = status;
    }

    public Integer getStatus() {
        return this.status;
    }
}
