package fr.openent.presences.enums;

public enum EventBusActions {

    ADDFOLDER("addFolder"),
    DELETE("delete"),
    GETDOCUMENT("getDocument"),
    LIST("list"),

    GETRESPONSABLES("eleve.getResponsables");

    private final String action;

    EventBusActions(String action) {
        this.action = action;
    }

    public String action() {
        return this.action;
    }

    public static enum EventBusAddresses {

        WORKSPACE_BUS_ADDRESS("org.entcore.workspace"),
        VIESCOLAIRE_BUS_ADDRESS("viescolaire");

        private final String address;

        EventBusAddresses(String address) {
            this.address = address;
        }

        public String address() {
            return this.address;
        }
    }

}