package fr.openent.presences.model;

public class MultipleSlotSettings {

    private Boolean userValue;
    private Boolean structureValue;


    public MultipleSlotSettings() {
        this.structureValue = true;
    }

    public MultipleSlotSettings(boolean value) {
        this.structureValue = value;
        this.userValue = value;
    }


    public void setUserValue(boolean value) {
        this.userValue = value;
    }

    public void setStructureValue(boolean value) {
        this.structureValue = value;
    }

    public boolean getUserValue() {
        return this.userValue;
    }

    public boolean getStructureValue() {
        return this.structureValue;
    }

    public boolean getDefaultValue() {
        return (this.userValue != null) ? this.userValue : this.structureValue;
    }

}
