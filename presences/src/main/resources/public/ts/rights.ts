const rights = {
    workflow: {
        access: 'fr.openent.presences.controller.PresencesController|view',
        readRegister: 'fr.openent.presences.controller.RegisterController|getRegister',
        createRegister: 'fr.openent.presences.controller.RegisterController|postRegister',
        readRegistry: 'fr.openent.presences.controller.RegistryController|getRegistry',
        readEvent: 'fr.openent.presences.controller.EventController|getEvents',
        createEvent: 'fr.openent.presences.controller.EventController|postEvent',
        search: 'fr.openent.presences.controller.SearchController|searchUsers',
        searchStudents: 'fr.openent.presences.controller.SearchController|search',
        export: 'fr.openent.presences.controller.CourseController|exportCourses',
        notify: 'fr.openent.presences.controller.CourseController|notify',
        readExemption: 'fr.openent.presences.controller.ExemptionController|getExemptions',
        manageExemption: 'fr.openent.presences.controller.ExemptionController|createExemptions',
        managePresences: 'fr.openent.presences.controller.FakeRight|managePresences',
        widget_alerts: 'fr.openent.presences.controller.FakeRight|widgetAlerts',
        widget_forgotten_registers: 'fr.openent.presences.controller.FakeRight|widgetForgottenRegisters',
        widget_statements: 'fr.openent.presences.controller.FakeRight|widgetStatements',
        widget_remarks: 'fr.openent.presences.controller.FakeRight|widgetRemarks',
        widget_absences: 'fr.openent.presences.controller.FakeRight|widgetAbsences',
        widget_day_courses: 'fr.openent.presences.controller.FakeRight|widgetDayCourses',
        widget_current_course: 'fr.openent.presences.controller.FakeRight|widgetCurrentCourse',
        widget_day_presences: 'fr.openent.presences.controller.FakeRight|widgetDayPresences',
        readPresences: 'fr.openent.presences.controller.PresencesController|getPresences',
        createPresences: 'fr.openent.presences.controller.PresencesController|createPresence',
        manageStatementAbsences: 'fr.openent.presences.controller.StatementAbsenceController|validate'
    }
};

export default rights;