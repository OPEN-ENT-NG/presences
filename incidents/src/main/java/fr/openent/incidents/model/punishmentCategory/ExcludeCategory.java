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

    @Override
    public void formatDates() {
        start_at = start_at.replace("/", "-");
        end_at = end_at.replace("/", "-");
    }

    @Override
    public String getLabel() {
        return PunishmentCategory.EXCLUDE;
    }

    public String getStartAt() {
        return start_at;
    }

    public String getEndAt() {
        return end_at;
    }
}
