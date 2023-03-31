package fr.openent.presences.controller;

import fr.openent.presences.Presences;
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

    @Get("/rights/widget/alerts")
    @SecuredAction(Presences.ALERTS_WIDGET)
    public void widgetAlerts(HttpServerRequest request) {
        notImplemented(request);
    }

    @Get("/rights/widgets/forgotten_registers")
    @SecuredAction(Presences.FORGOTTEN_REGISTERS_WIDGET)
    public void widgetForgottenRegisters(HttpServerRequest request) {
        notImplemented(request);
    }

    @Get("/rights/widgets/statements")
    @SecuredAction(Presences.STATEMENTS_WIDGET)
    public void widgetStatements(HttpServerRequest request) {
        notImplemented(request);
    }

    @Get("/rights/widgets/remarks")
    @SecuredAction(Presences.REMARKS_WIDGET)
    public void widgetRemarks(HttpServerRequest request) {
        notImplemented(request);
    }

    @Get("/rights/widgets/absences")
    @SecuredAction(Presences.ABSENCES_WIDGET)
    public void widgetAbsences(HttpServerRequest request) {
        notImplemented(request);
    }

    @Get("/rights/widgets/day_courses")
    @SecuredAction(Presences.DAY_COURSES_WIDGET)
    public void widgetDayCourses(HttpServerRequest request) {
        notImplemented(request);
    }

    @Get("/rights/widgets/current_course")
    @SecuredAction(Presences.CURRENT_COURSE_WIDGET)
    public void widgetCurrentCourse(HttpServerRequest request) {
        notImplemented(request);
    }

    @Get("/rights/widgets/day_presences")
    @SecuredAction(Presences.DAY_PRESENCES_WIDGET)
    public void widgetDayPresences(HttpServerRequest request) {
        notImplemented(request);
    }

    @Get("/rights/manage/event/absent")
    @SecuredAction(Presences.MANAGE)
    public void managePresences(HttpServerRequest request) {notImplemented(request);}

    @Get("/rights/student/events/view")
    @SecuredAction(Presences.STUDENT_EVENTS_VIEW)
    public void studentEventsView(HttpServerRequest request) {notImplemented(request);}

    @Get("/rights/absence/statements/view")
    @SecuredAction(Presences.ABSENCE_STATEMENTS_VIEW)
    public void AbsenceStatementsView(HttpServerRequest request) {notImplemented(request);}

    @Get("/rights/absence/statements/create")
    @SecuredAction(Presences.ABSENCE_STATEMENTS_CREATE)
    public void AbsenceStatementsCreate(HttpServerRequest request) {notImplemented(request);}

    @Get("/rights/absence/statements/manage/restricted")
    @SecuredAction(Presences.MANAGE_ABSENCE_STATEMENTS_RESTRICTED)
    public void manageAbsenceStatementsRestricted(HttpServerRequest request) {notImplemented(request);}

    @Get("/rights/search/restricted")
    @SecuredAction(Presences.SEARCH_RESTRICTED)
    public void searchRestricted(HttpServerRequest request) {notImplemented(request);}

    @Get("/rights/read/event/restricted")
    @SecuredAction(Presences.READ_EVENT_RESTRICTED)
    public void readEventRestricted(HttpServerRequest request) {notImplemented(request);}

    @Get("/rights/read/exemption/restricted")
    @SecuredAction(Presences.READ_EXEMPTION_RESTRICTED)
    public void readExemptionRestricted(HttpServerRequest request) {notImplemented(request);}

    @Get("/rights/manage/exemption/restricted")
    @SecuredAction(Presences.MANAGE_EXEMPTION_RESTRICTED)
    public void manageExemptionRestricted(HttpServerRequest request) {notImplemented(request);}

    @Get("/rights/settings/init/1d")
    @SecuredAction(Presences.INIT_SETTINGS_1D)
    public void initSettings1D(HttpServerRequest request) {notImplemented(request);}

    @Get("/rights/settings/init/2d")
    @SecuredAction(Presences.INIT_SETTINGS_2D)
    public void initSettings2D(HttpServerRequest request) {notImplemented(request);}

    @Get("/rights/read/presence")
    @SecuredAction(Presences.READ_PRESENCE)
    public void readPresence(HttpServerRequest request) {notImplemented(request);}

    @Get("/rights/read/presence/restricted")
    @SecuredAction(Presences.READ_PRESENCE_RESTRICTED)
    public void readPresenceRestricted(HttpServerRequest request) {notImplemented(request);}

    @Get("/rights/init/popup")
    @SecuredAction(Presences.INIT_POPUP)
    public void initPopup(HttpServerRequest request) {notImplemented(request);}

    @Get("/rights/settings/get")
    @SecuredAction(Presences.SETTINGS_GET)
    public void getSettings(HttpServerRequest request) {notImplemented(request);}

}
