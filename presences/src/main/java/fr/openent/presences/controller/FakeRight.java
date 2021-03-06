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
}
