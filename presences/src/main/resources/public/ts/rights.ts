const rights = {
    workflow: {
        readRegister: 'fr.openent.presences.controller.RegisterController|getRegister',
        createRegister: 'fr.openent.presences.controller.RegisterController|postRegister',
        createEvent: 'fr.openent.presences.controller.EventController|postEvent',
        search: 'fr.openent.presences.controller.SearchController|searchUsers'
    }
};

export default rights;