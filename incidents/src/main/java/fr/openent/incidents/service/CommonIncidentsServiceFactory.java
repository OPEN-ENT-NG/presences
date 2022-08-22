package fr.openent.incidents.service;

import fr.openent.incidents.service.impl.*;
import fr.openent.presences.common.export.*;
import fr.openent.presences.common.service.*;
import fr.openent.presences.common.service.impl.*;
import io.vertx.core.*;
import io.vertx.core.eventbus.*;

public class CommonIncidentsServiceFactory {
    private final Vertx vertx;

    public CommonIncidentsServiceFactory(Vertx vertx) {
        this.vertx = vertx;
    }

    public IncidentsService incidentsService() {
        return new DefaultIncidentsService(this.vertx.eventBus());
    }

    public PunishmentService punishmentService() {
        return new DefaultPunishmentService(this.vertx.eventBus());
    }

    public GroupService groupService() { return new DefaultGroupService(this.vertx.eventBus()); }

    public EventBus eventBus() {
        return this.vertx.eventBus();
    }

    public Vertx vertx() {
        return this.vertx;
    }

    public ExportData exportData() {
        return new ExportData(this.vertx);
    }
}
