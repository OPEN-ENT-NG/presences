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

    @Get("/rights/manage")
    @SecuredAction(StatisticsPresences.MANAGE)
    public void manage(HttpServerRequest request) {
        notImplemented(request);
    }
}
