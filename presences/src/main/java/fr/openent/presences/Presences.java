package fr.openent.presences;

import fr.openent.presences.common.export.ExportData;
import fr.openent.presences.common.incidents.Incidents;
import fr.openent.presences.common.massmailing.Massmailing;
import fr.openent.presences.common.statistics_presences.StatisticsPresences;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.controller.*;
import fr.openent.presences.controller.events.EventController;
import fr.openent.presences.controller.events.LatenessEventController;
import fr.openent.presences.cron.CreateDailyRegistersTask;
import fr.openent.presences.db.DB;
import fr.openent.presences.event.PresencesRepositoryEvents;
import fr.openent.presences.service.CommonPresencesServiceFactory;
import fr.openent.presences.worker.CreateDailyPresenceWorker;
import fr.openent.presences.worker.EventExportWorker;
import fr.openent.presences.worker.PresencesExportWorker;
import fr.openent.presences.worker.ResetAlertsWorker;
import fr.wseduc.cron.CronTrigger;
import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.BaseServer;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

import java.text.ParseException;

public class Presences extends BaseServer {

    public static final String VIEW = "view";
    public static final String READ_PRESENCE = "presences.presence.read";
    public static final String READ_PRESENCE_RESTRICTED = "presences.presence.read.restricted";
    public static final String CREATE_PRESENCE = "presences.presence.create";
    public static final String MANAGE_PRESENCE = "presences.presence.manage";
    public static final String READ_REGISTER = "presences.register.read";
    public static final String CREATE_REGISTER = "presences.register.create";
    public static final String SEARCH = "presences.search";
    public static final String SEARCH_RESTRICTED = "presences.search.restricted";
    public static final String SEARCH_STUDENTS = "presences.search.students";
    public static final String READ_CHILDREN = "presences.children.read";
    public static final String EXPORT = "presences.export";
    public static final String NOTIFY = "presences.notify";
    public static final String CREATE_EVENT = "presences.event.create";
    public static final String READ_EVENT = "presences.event.read";
    public static final String READ_EVENT_RESTRICTED = "presences.event.read.restricted";
    public static final String READ_EXEMPTION = "presences.exemption.read";
    public static final String MANAGE_EXEMPTION = "presences.exemption.manage";
    public static final String READ_EXEMPTION_RESTRICTED = "presences.exemption.read.restricted";
    public static final String MANAGE_EXEMPTION_RESTRICTED = "presences.exemption.manage.restricted";
    public static final String MANAGE = "presences.manage";
    public static final String REGISTRY = "presences.registry";
    public static final String CREATE_ACTION = "presences.action.create";
    public static final String STUDENT_EVENTS_VIEW = "presences.student.events.view";
    public static final String ABSENCE_STATEMENTS_VIEW = "presences.absence.statements.view";
    public static final String MANAGE_ABSENCE_STATEMENTS = "presences.manage.absence.statements";
    public static final String MANAGE_ABSENCE_STATEMENTS_RESTRICTED = "presences.manage.absence.statements.restricted";
    public static final String ABSENCE_STATEMENTS_CREATE = "presences.absence.statements.create";
    public static final String MANAGE_FORGOTTEN_NOTEBOOK = "presences.manage.forgotten.notebook";
    public static final String MANAGE_COLLECTIVE_ABSENCES = "presences.manage.collective.absences";
    public static final String ALERTS_STUDENT_NUMBER = "presences.alerts.students.number";
    public static final String READ_OWN_INFO = "presences.read.own.info";
    public static final String INIT_SETTINGS_1D = "presences.init.settings.1d";
    public static final String INIT_SETTINGS_2D = "presences.init.settings.2d";

    public static final String INIT_POPUP = "presences.init.popup";

    // Widget rights
    public static final String ALERTS_WIDGET = "presences.widget.alerts";
    public static final String FORGOTTEN_REGISTERS_WIDGET = "presences.widget.forgotten_registers";
    public static final String STATEMENTS_WIDGET = "presences.widget.statements";
    public static final String REMARKS_WIDGET = "presences.widget.remarks";
    public static final String ABSENCES_WIDGET = "presences.widget.absences";
    public static final String DAY_COURSES_WIDGET = "presences.widget.day_courses";
    public static final String CURRENT_COURSE_WIDGET = "presences.widget.current_course";
    public static final String DAY_PRESENCES_WIDGET = "presences.widget.day_presences";
    public static final Integer PAGE_SIZE = 20;
    // Statistics
    public static final String STATISTICS_ACCESS_DATA = "presences.statistics.access_data";
    public static final String SETTINGS_GET = "presences.settings.get";
    public static final String VIEW_STATISTICS = "statistics_presences.view";
    public static final String VIEW_STATISTICS_RESTRICTED = "statistics_presences.view.restricted";

    public static String dbSchema;
    public static String ebViescoAddress = "viescolaire";

    // Calendar
    public static final String CALENDAR_VIEW = "presences.calendar.view";

    @Override
    public void start() throws Exception {
        super.start();
        dbSchema = config.getString("db-schema");
        ebViescoAddress = "viescolaire";
        final EventBus eb = getEventBus(vertx);
        Storage storage = new StorageFactory(vertx, config).getStorage();
        ExportData exportData = new ExportData(vertx);
        DB.getInstance().init(Neo4j.getInstance(), Sql.getInstance(), MongoDb.getInstance());
        CommonPresencesServiceFactory commonPresencesServiceFactory = new CommonPresencesServiceFactory(vertx, storage, config, exportData);

//        final String exportCron = config.getString("export-cron");

        addController(new PresencesController(commonPresencesServiceFactory));
        addController(new CourseController(commonPresencesServiceFactory));
        addController(new RegisterController(commonPresencesServiceFactory));
        addController(new AbsenceController(commonPresencesServiceFactory));
        addController(new EventController(commonPresencesServiceFactory));
        addController(new LatenessEventController(commonPresencesServiceFactory));
        addController(new ExemptionController(commonPresencesServiceFactory));
        addController(new SearchController(eb));
        addController(new CalendarController(eb));
        addController(new ReasonController());
        addController(new RegistryController(eb));
        addController(new EventBusController(eb, commonPresencesServiceFactory));
        addController(new NotebookController());
        addController(new SettingsController());
        addController(new AlertController(eb));
        addController(new ActionController());
        addController(new DisciplineController());
        addController(new InitController(eb));
        addController(new StudentController(eb));
        addController(new StatementAbsenceController(eb, storage));
        addController(new CollectiveAbsenceController());
        addController(new ArchiveController(commonPresencesServiceFactory));
        addController(new ConfigController());
        addController(new StatisticsController());
        addController(new GroupingController(commonPresencesServiceFactory));

        // Controller that create fake rights for widgets
        addController(new FakeRight());

        //Init incident
        Incidents.getInstance().init(eb);
        Viescolaire.getInstance().init(eb);
        Massmailing.getInstance().init(eb);
        StatisticsPresences.getInstance().init(eb);

        // Repository Events
        setRepositoryEvents(new PresencesRepositoryEvents(eb));

        if (config.containsKey("registers-cron")) {
            vertx.deployVerticle(CreateDailyPresenceWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));
            try {
                new CronTrigger(vertx, config.getString("registers-cron")).schedule(new CreateDailyRegistersTask(vertx.eventBus()));
            } catch (ParseException e) {
                log.fatal(e.getMessage(), e);
            }
        }

        // worker to be triggered manually (API will call its worker to send csv's export via email)
        vertx.deployVerticle(EventExportWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));
        vertx.deployVerticle(ResetAlertsWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));

        vertx.deployVerticle(PresencesExportWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));
    }

    public static void launchResetAlertsWorker(EventBus eb, JsonObject params) {
        eb.send(ResetAlertsWorker.class.getName(), params, new DeliveryOptions().setSendTimeout(1000 * 1000L));
    }

}
