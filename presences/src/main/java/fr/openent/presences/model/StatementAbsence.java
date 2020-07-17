package fr.openent.presences.model;

import fr.openent.presences.Presences;

import java.util.Arrays;
import java.util.Collections;

public class StatementAbsence extends Model {

    private String id;
    private String start_at;
    private String end_at;
    private String student_id;
    private String structure_id;
    private String description;
    private String treated_at;
    private String validator_id;
    private String attachment_id;
    private String created_at;


    public StatementAbsence() {
        table = Presences.dbSchema + ".statement_absence";

        fillables.put("id", Collections.emptyList());
        fillables.put("start_at", Arrays.asList("CREATE", "mandatory"));
        fillables.put("end_at", Arrays.asList("CREATE", "mandatory"));
        fillables.put("student_id", Arrays.asList("CREATE", "mandatory"));
        fillables.put("structure_id", Arrays.asList("CREATE", "mandatory"));
        fillables.put("description", Collections.singletonList("CREATE"));
        fillables.put("treated_at", Collections.singletonList("UPDATE"));
        fillables.put("validator_id", Collections.singletonList("UPDATE"));
        fillables.put("attachment_id", Collections.singletonList("CREATE"));
        fillables.put("created_at", Collections.emptyList());
    }
}
