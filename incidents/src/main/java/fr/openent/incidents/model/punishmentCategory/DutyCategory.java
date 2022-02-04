package fr.openent.incidents.model.punishmentCategory;

import java.util.Arrays;

public class DutyCategory extends PunishmentCategory {
    private String delay_at;
    private String instruction;

    public DutyCategory() {
        fillables.put("delay_at", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("instruction", Arrays.asList("CREATE", "UPDATE"));
    }

    @Override
    public void formatDates() {
        delay_at = delay_at.replace("/", "-");
    }

    @Override
    public String getLabel() {
        return PunishmentCategory.DUTY;
    }
}
