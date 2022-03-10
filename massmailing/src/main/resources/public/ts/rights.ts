const rights = {
    workflow: {
        access: 'fr.openent.massmailing.controller.MassmailingController|view',
        manage: 'fr.openent.massmailing.controller.SettingsController|getTemplates',
        viewRestricted: 'fr.openent.massmailing.controller.FakeRight|viewRestricted'
    }
};

export default rights;