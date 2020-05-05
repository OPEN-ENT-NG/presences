const incidentsRights = {
    workflow: {
        access: 'fr.openent.incidents.controller.IncidentsController|view',
        readPunishment: 'fr.openent.incidents.controller.FakeRight|punishmentsView',
        readSanction: 'fr.openent.incidents.controller.FakeRight|sanctionsView',
        createPunishment: 'fr.openent.incidents.controller.FakeRight|punishmentCreate',
        createSanction: 'fr.openent.incidents.controller.FakeRight|sanctionCreate',
    }
};

export default incidentsRights;