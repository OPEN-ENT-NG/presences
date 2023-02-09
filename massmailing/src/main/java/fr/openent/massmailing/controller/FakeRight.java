package fr.openent.massmailing.controller;

import fr.openent.massmailing.*;
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

    @Get("/rights/manage/restricted")
    @SecuredAction(Massmailing.MANAGE_RESTRICTED)
    public void manageRestricted(HttpServerRequest request) {
        notImplemented(request);
    }

}
