const rights = {
    workflow: {
        access: 'fr.openent.massmailing.controller.MassmailingController|view',
        manage: 'fr.openent.massmailing.controller.SettingsController|getTemplates',
        manageRestricted: 'fr.openent.massmailing.controller.FakeRight|manageRestricted'
    }
};

export default rights;