package fr.openent.presences;

import fr.openent.presences.common.incidents.Incidents;
import fr.openent.presences.common.massmailing.Massmailing;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.controller.*;
import fr.openent.presences.controller.events.EventController;
import fr.openent.presences.controller.events.LatenessEventController;
import fr.openent.presences.cron.CreateDailyRegistersTask;
import fr.openent.presences.db.DB;
import fr.openent.presences.worker.CreateDailyPresenceWorker;
import fr.wseduc.cron.CronTrigger;
import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.http.BaseServer;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

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
    public static final String READ_CHILDREN = "presences.children.read";
    public static final String EXPORT = "presences.export";
    public static final String NOTIFY = "presences.notify";
    public static final String CREATE_EVENT = "presences.event.create";
    public static final String READ_EVENT = "presences.event.read";
    public static final String READ_EXEMPTION = "presences.exemption.read";
    public static final String MANAGE_EXEMPTION = "presences.exemption.manage";
    public static final String MANAGE = "presences.manage";
    public static final String REGISTRY = "presences.registry";
    public static final String CREATE_ACTION = "presences.action.create";
    public static final String STUDENT_EVENTS_VIEW = "presences.student.events.view";
    public static final String ABSENCE_STATEMENTS_VIEW = "presences.absence.statements.view";
    public static final String MANAGE_ABSENCE_STATEMENTS = "presences.manage.absence.statements";
    public static final String ABSENCE_STATEMENTS_CREATE = "presences.absence.statements.create";
    public static final String MANAGE_FORGOTTEN_NOTEBOOK = "presences.manage.forgotten.notebook";
    public static final String MANAGE_COLLECTIVE_ABSENCES = "presences.manage.collective.absences";

    public static final String ALERTS_STUDENT_NUMBER = "presences.alerts.students.number";

    public static final String READ_OWN_INFO = "presences.read.own.info";


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
        Storage storage = new StorageFactory(vertx, config).getStorage();
//        final String exportCron = config.getString("export-cron");

        DB.getInstance().init(Neo4j.getInstance(), Sql.getInstance(), MongoDb.getInstance());

        addController(new PresencesController());
        addController(new CourseController(eb));
        addController(new RegisterController(eb));
        addController(new AbsenceController(eb));
        addController(new EventController(eb));
        addController(new LatenessEventController(eb));
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
        addController(new StudentController(eb));
        addController(new StatementAbsenceController(eb, storage));
        addController(new CollectiveAbsenceController());

        // Controller that create fake rights for widgets
        addController(new FakeRight());

        //Init incident
        Incidents.getInstance().init(eb);
        Viescolaire.getInstance().init(eb);
        Massmailing.getInstance().init(eb);

        if (config.containsKey("registers-cron")) {
            vertx.deployVerticle(CreateDailyPresenceWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));
            try {
                new CronTrigger(vertx, config.getString("registers-cron")).schedule(new CreateDailyRegistersTask(vertx.eventBus()));
            } catch (ParseException e) {
                log.fatal(e.getMessage(), e);
            }
        }
    }

}
