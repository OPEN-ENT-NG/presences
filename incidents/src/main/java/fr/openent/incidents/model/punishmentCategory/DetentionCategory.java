package fr.openent.incidents.model.punishmentCategory;

import java.util.Arrays;

public class DetentionCategory extends PunishmentCategory {
    private String start_at;
    private String end_at;
    private String place;

    public DetentionCategory() {
        fillables.put("start_at", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("end_at", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("place", Arrays.asList("CREATE", "UPDATE"));
    }

    public void formatDates() {
        start_at = start_at.replace("/", "-");
        end_at = end_at.replace("/", "-");
    }
}
