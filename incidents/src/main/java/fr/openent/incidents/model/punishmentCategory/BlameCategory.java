package fr.openent.incidents.model.punishmentCategory;

public class BlameCategory extends PunishmentCategory {
    public void formatDates() {}

    @Override
    public String getLabel() {
        return PunishmentCategory.BLAME;
    }
}
