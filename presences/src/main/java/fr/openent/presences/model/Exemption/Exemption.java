package fr.openent.presences.model.Exemption;

import fr.openent.presences.model.Model;

public abstract class Exemption extends Model {
    protected String start_date;
    protected String end_date;
    protected String structure_id;
    protected String comment;
    protected String subject_id;
    protected Boolean attendance;
    protected Boolean is_every_two_weeks;

    public abstract String getStartDate();

    public abstract String getEndDate();

    public abstract String getStructureId();

    public abstract String getComment();

    public abstract Boolean isEveryTwoWeeks();

    public abstract Boolean getAttendance();

}
