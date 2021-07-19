package fr.openent.presences.service;

import fr.openent.presences.common.service.ExportZIPService;
import fr.openent.presences.common.service.StructureService;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultStructureService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.common.service.impl.ExportZIPServiceImpl;
import fr.openent.presences.service.impl.DefaultArchiveService;
import fr.openent.presences.service.impl.DefaultEventService;
import fr.openent.presences.service.impl.DefaultReasonService;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.storage.Storage;

public class CommonPresencesServiceFactory {
    private final Vertx vertx;
    private final Storage storage;

    public CommonPresencesServiceFactory(Vertx vertx, Storage storage) {
        this.vertx = vertx;
        this.storage = storage;
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

    // Presences Service

    public EventService eventService() {
        return new DefaultEventService(vertx.eventBus());
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
