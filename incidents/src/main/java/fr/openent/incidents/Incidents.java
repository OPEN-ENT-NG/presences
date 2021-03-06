package fr.openent.incidents;

import fr.openent.incidents.controller.*;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.http.BaseServer;

public class Incidents extends BaseServer {

    public static String dbSchema;
    public static String ebViescoAddress = "viescolaire";
    public static final String READ_INCIDENT = "incidents.incident.read";
    public static final String MANAGE_INCIDENT = "incidents.incident.manage";
    public static final String SANCTION_CREATE = "incidents.sanction.create";
    public static final String PUNISHMENT_CREATE = "incidents.punishment.create";
    public static final String PUNISHMENTS_VIEW = "incidents.punishments.view";
    public static final String SANCTIONS_VIEW = "incidents.sanction.view";
    public static final String STUDENT_EVENTS_VIEW = "presences.student.events.view";

    public static Integer PAGE_SIZE = 20;

    @Override
    public void start() throws Exception {

        super.start();
        dbSchema = config.getString("db-schema");

        final EventBus eb = getEventBus(vertx);
        addController(new IncidentsController(eb));
        addController(new IncidentsTypeController());
        addController(new PartnerController());
        addController(new PlaceController());
        addController(new ProtagonistTypeController());
        addController(new SeriousnessController());
        addController(new StudentController(eb));
        addController(new PunishmentController());
        addController(new PunishmentTypeController());
        addController(new PunishmentCategoryController());
        addController(new EventBusController(eb));
        addController(new FakeRight());
    }

}
