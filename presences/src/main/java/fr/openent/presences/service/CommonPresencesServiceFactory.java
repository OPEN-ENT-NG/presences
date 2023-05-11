package fr.openent.presences.service;

import fr.openent.presences.common.export.*;
import fr.openent.presences.common.helper.PersonHelper;
import fr.openent.presences.common.service.*;
import fr.openent.presences.common.service.impl.*;
import fr.openent.presences.helper.CourseHelper;
import fr.openent.presences.helper.EventHelper;
import fr.openent.presences.helper.PresenceHelper;
import fr.openent.presences.helper.SlotHelper;
import fr.openent.presences.service.impl.*;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.entcore.common.storage.Storage;

public class CommonPresencesServiceFactory {
    private final Vertx vertx;
    private final Storage storage;
    private final JsonObject config;
    private final ExportData exportData;


    private final UserService userService;
    private final GroupService groupService;
    private final GroupingService groupingService;

    private final AbsenceService absenceService;
    private final EventService eventService;
    private final PresenceService presenceService;

    private final InitService initService;

    private final WorkspaceService workspaceService;

    public CommonPresencesServiceFactory(Vertx vertx, Storage storage, JsonObject config, ExportData exportData) {
        this.vertx = vertx;
        this.storage = storage;
        this.config = config;
        this.exportData = exportData;

        this.userService = new DefaultUserService();
        this.groupService = new DefaultGroupService(this.vertx.eventBus());
        this.groupingService = new DefaultGroupingService();
        this.absenceService = new DefaultAbsenceService(this);
        this.eventService = new DefaultEventService(this);
        this.presenceService = new DefaultPresenceService(this);
        this.workspaceService = new DefaultWorkspaceService(this.vertx, this.storage, this.config);
        this.initService = new DefaultInitService();
    }

    public CommonPresencesServiceFactory(Vertx vertx, Storage storage, JsonObject config) {
        this.vertx = vertx;
        this.storage = storage;
        this.config = config;
        this.exportData = null;
        this.userService = new DefaultUserService();
        this.groupService = new DefaultGroupService(this.vertx.eventBus());
        this.groupingService = new DefaultGroupingService();
        this.absenceService = new DefaultAbsenceService(this);
        this.eventService = new DefaultEventService(this);
        this.presenceService = new DefaultPresenceService(this);
        this.workspaceService = new DefaultWorkspaceService(this.vertx, this.storage, this.config);
        this.initService = new DefaultInitService();
    }

    // Common
    public UserService userService() {
        return userService;
    }

    public GroupService groupService() { return groupService; }

    public GroupingService groupingService() { return groupingService; }

    public StructureService structureService() {
        return new DefaultStructureService();
    }

    public ExportZIPService exportZIPService() {
        return new ExportZIPServiceImpl(vertx, storage);
    }

    public ExportPDFService exportPDFService() {
        return new ExportPDFServiceImpl(vertx, config);
    }

    public ExportAbsenceService exportAbsenceService() {
        return new DefaultExportAbsenceService(this);
    }

    public SettingsService settingsService() {
        return new DefaultSettingsService();
    }

    public EventHelper eventHelper() {
        return new EventHelper(this.vertx.eventBus());
    }

    public PersonHelper personHelper() { return new PersonHelper();}

    public PresenceHelper presenceHelper() { return new PresenceHelper();}

    public SlotHelper slotHelper() { return new SlotHelper(this.vertx.eventBus());}

    public CourseHelper courseHelper() {
        return new CourseHelper(this.vertx.eventBus());
    }

    // Presences Service

    public AbsenceService absenceService() {
        return absenceService;
    }

    public DisciplineService disciplineService() { return new DefaultDisciplineService(); }

    public EventService eventService() {
        return eventService;
    }

    public ExportEventService exportEventService() {
        return new DefaultExportEventService(this);
    }

    public ReasonService reasonService() {
        return new DefaultReasonService();
    }

    public ArchiveService archiveService() {
        return new DefaultArchiveService(this);
    }

    public PresenceService presenceService() {
        return presenceService;
    }

    public ExemptionService exemptionService() {
        return new DefaultExemptionService(this.vertx.eventBus());
    }

    public EventStudentService eventStudentService() {
        return new DefaultEventStudentService(this.vertx.eventBus());
    }

    public RegisterService registerService() {
        return new DefaultRegisterService(this);
    }

    public CourseService courseService() {
        return new DefaultCourseService(this);
    }

    public CollectiveAbsenceService collectiveAbsenceService() {
        return new DefaultCollectiveAbsenceService(this.vertx.eventBus());
    }

    public LatenessEventService latenessEventService() {
        return new DefaultLatenessEventService(this);
    }

    public WorkspaceService workspaceService() {
        return workspaceService;
    }

    public InitService initService() {
        return initService;
    }

    // Helpers
    public EventBus eventBus() {
        return this.vertx.eventBus();
    }

    public Vertx vertx() {
        return this.vertx;
    }

    public ExportData exportData() {
        return exportData;
    }
}
