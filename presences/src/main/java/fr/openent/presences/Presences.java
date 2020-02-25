package fr.openent.presences;

import fr.openent.presences.common.incidents.Incidents;
import fr.openent.presences.common.massmailing.Massmailing;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.controller.*;
import fr.openent.presences.cron.CreateDailyRegistersTask;
import fr.openent.presences.worker.CreateDailyPresenceWorker;
import fr.wseduc.cron.CronTrigger;
import fr.wseduc.webutils.Utils;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.BaseServer;

import java.text.ParseException;

public class Presences extends BaseServer {

    public static String dbSchema;
    public static String ebViescoAddress = "viescolaire";

    public static final String READ_PRESENCE = "presences.presence.read";
    public static final String CREATE_PRESENCE = "presences.presence.create";
    public static final String MANAGE_PRESENCE = "presences.presence.manage";
    public static final String READ_REGISTER = "presences.register.read";
    public static final String CREATE_REGISTER = "presences.register.create";
    public static final String SEARCH = "presences.search";
    public static final String SEARCH_STUDENTS = "presences.search.students";
    public static final String EXPORT = "presences.export";
    public static final String NOTIFY = "presences.notify";
    public static final String CREATE_EVENT = "presences.event.create";
    public static final String READ_EVENT = "presences.event.read";
    public static final String READ_EXEMPTION = "presences.exemption.read";
    public static final String MANAGE_EXEMPTION = "presences.exemption.manage";
    public static final String MANAGE = "presences.manage";
    public static final String REGISTRY = "presences.registry";
    public static final String CREATE_ACTION = "presences.action.create";

    // Widget rights
    public static final String ALERTS_WIDGET = "presences.widget.alerts";
    public static final String FORGOTTEN_REGISTERS_WIDGET = "presences.widget.forgotten_registers";
    public static final String STATEMENTS_WIDGET = "presences.widget.statements";
    public static final String REMARKS_WIDGET = "presences.widget.remarks";
    public static final String ABSENCES_WIDGET = "presences.widget.absences";
    public static final String DAY_COURSES_WIDGET = "presences.widget.day_courses";
    public static final String CURRENT_COURSE_WIDGET = "presences.widget.current_course";
    public static final String DAY_PRESENCES_WIDGET = "presences.widget.day_presences";

    public static Integer PAGE_SIZE = 20;

    @Override
    public void start() throws Exception {
        super.start();
        dbSchema = config.getString("db-schema");
        ebViescoAddress = "viescolaire";
        final EventBus eb = getEventBus(vertx);
//        final String exportCron = config.getString("export-cron");


        addController(new PresencesController(eb));
        addController(new CourseController(eb));
        addController(new RegisterController(eb));
        addController(new AbsenceController(eb));
        addController(new EventController(eb));
        addController(new ExemptionController(eb));
        addController(new SearchController(eb));
        addController(new CalendarController(eb));
        addController(new ReasonController());
        addController(new RegistryController(eb));
        addController(new EventBusController(eb));
        addController(new NotebookController());
        addController(new SettingsController());
        addController(new AlertController(eb));
        addController(new MementoController(eb));
        addController(new ActionController());
        addController(new DisciplineController());
        addController(new InitController(eb));

        // Controller that create fake rights for widgets
        addController(new FakeRight());

        //Init incident
        Incidents.getInstance().init(eb);
        Viescolaire.getInstance().init(eb);
        Massmailing.getInstance().init(eb);

        vertx.deployVerticle(CreateDailyPresenceWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));

        try {
            new CronTrigger(vertx, config.getString("registers-cron", "0 1 * * * ? *")).schedule(new CreateDailyRegistersTask(vertx.eventBus()));
        } catch (ParseException e) {
            log.fatal(e.getMessage(), e);
        }
    }

}
