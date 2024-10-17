package fr.openent.presences.constants;

import java.util.*;

public class Alerts {
    public static final String INCIDENT = "INCIDENT";
    public static final String ABSENCE = "ABSENCE";
    public static final String FORGOTTEN_NOTEBOOK = "FORGOTTEN_NOTEBOOK";
    public static final String LATENESS = "LATENESS";

    public static final List<String> ALERT_LIST = Arrays.asList(
            INCIDENT,
            ABSENCE,
            FORGOTTEN_NOTEBOOK,
            LATENESS
    );
}
