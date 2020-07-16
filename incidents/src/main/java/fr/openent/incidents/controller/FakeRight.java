package fr.openent.incidents.controller;

import fr.openent.incidents.Incidents;
import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;

public class FakeRight extends ControllerHelper {
    public FakeRight() {
        super();
    }

    private void notImplemented(HttpServerRequest request) {
        request.response().setStatusCode(501).end();
    }

    @Get("/rights/incidents/punishment_create")
    @SecuredAction(Incidents.PUNISHMENT_CREATE)
    public void punishmentCreate(HttpServerRequest request) {
        notImplemented(request);
    }

    @Get("/rights/incidents/sanction_create")
    @SecuredAction(Incidents.SANCTION_CREATE)
    public void sanctionCreate(HttpServerRequest request) {
        notImplemented(request);
    }

    @Get("/rights/incidents/punishments_view")
    @SecuredAction(Incidents.PUNISHMENTS_VIEW)
    public void punishmentsView(HttpServerRequest request) {
        notImplemented(request);
    }

    @Get("/rights/incidents/sanctions_view")
    @SecuredAction(Incidents.SANCTIONS_VIEW)
    public void sanctionsView(HttpServerRequest request) {notImplemented(request);}

    @Get("/rights/incidents/student/events/view")
    @SecuredAction(Incidents.STUDENT_EVENTS_VIEW)
    public void studentEventsView(HttpServerRequest request) {notImplemented(request);}
}
