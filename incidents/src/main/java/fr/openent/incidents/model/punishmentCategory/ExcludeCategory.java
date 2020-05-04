package fr.openent.incidents.model.punishmentCategory;

import java.util.Arrays;

public class ExcludeCategory extends PunishmentCategory {
    private String start_at;
    private String end_at;
    private String mandatory_presence;

    public ExcludeCategory() {
        fillables.put("start_at", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("end_at", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("mandatory_presence", Arrays.asList("CREATE", "UPDATE"));
    }
}
