package fr.openent.statistics_presences.controller;

import fr.openent.statistics_presences.*;
import fr.wseduc.rs.*;
import fr.wseduc.security.*;
import io.vertx.core.http.*;
import org.entcore.common.controller.*;

public class FakeRight extends ControllerHelper {
    public FakeRight() {
        super();
    }

    private void notImplemented(HttpServerRequest request) {
        request.response().setStatusCode(501).end();
    }

    @Get("/rights/view/restricted")
    @SecuredAction(StatisticsPresences.VIEW_RESTRICTED)
    public void viewRestricted(HttpServerRequest request)  {
        notImplemented(request);
    }

    @Get("/rights/manage")
    @SecuredAction(StatisticsPresences.MANAGE)
    public void manage(HttpServerRequest request) {
        notImplemented(request);
    }

    @Get("/rights/manage/restricted")
    @SecuredAction(StatisticsPresences.MANAGE_RESTRICTED)
    public void manageRestricted(HttpServerRequest request) {
        notImplemented(request);
    }
}
