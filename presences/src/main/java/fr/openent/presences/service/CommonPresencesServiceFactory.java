package fr.openent.presences.service;

import fr.openent.presences.common.helper.PersonHelper;
import fr.openent.presences.common.service.ExportPDFService;
import fr.openent.presences.common.service.ExportZIPService;
import fr.openent.presences.common.service.StructureService;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultStructureService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.common.service.impl.ExportPDFServiceImpl;
import fr.openent.presences.common.service.impl.ExportZIPServiceImpl;
import fr.openent.presences.helper.EventHelper;
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

    public CommonPresencesServiceFactory(Vertx vertx, Storage storage, JsonObject config) {
        this.vertx = vertx;
        this.storage = storage;
        this.config = config;
    }

    // Common
    public UserService userService() {
        return new DefaultUserService();
    }

    public StructureService structureService() {
        return new DefaultStructureService();
    }

    public ExportZIPService exportZIPService() {
        return new ExportZIPServiceImpl(vertx, storage);
    }

    public ExportPDFService exportPDFService() {
        return new ExportPDFServiceImpl(vertx, config);
    }

    public SettingsService settingsService() {
        return new DefaultSettingsService();
    }

    public EventHelper eventHelper() {
        return new EventHelper(this.vertx.eventBus());
    }

    public PersonHelper personHelper() { return new PersonHelper();}

    public SlotHelper slotHelper() { return new SlotHelper(this.vertx.eventBus());}

    // Presences Service

    public EventService eventService() {
        return new DefaultEventService(vertx.eventBus());
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

    // Helpers
    public EventBus eventBus() {
        return this.vertx.eventBus();
    }

    public Vertx vertx() {
        return this.vertx;
    }
}
