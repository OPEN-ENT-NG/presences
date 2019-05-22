const rights = {
    workflow: {
        access: 'fr.openent.presences.controller.PresencesController|view',
        readRegister: 'fr.openent.presences.controller.RegisterController|getRegister',
        createRegister: 'fr.openent.presences.controller.RegisterController|postRegister',
        createEvent: 'fr.openent.presences.controller.EventController|postEvent',
        search: 'fr.openent.presences.controller.SearchController|searchUsers',
        export: 'fr.openent.presences.controller.CourseController|exportCourses',
        readExemption: 'fr.openent.presences.controller.ExemptionController|getExemptions',
        manageExemption: 'fr.openent.presences.controller.ExemptionController|createExemptions'
    }
};

export default rights;