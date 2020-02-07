import {Behaviours} from 'entcore';
import {
    absenceForm,
    exemptionForm,
    forgottenNotebookForm,
    navigation,
    presencesActionManage,
    presencesAlertManage,
    presencesManage,
    presencesManageLightbox,
    presencesReasonManage,
    statisticsManage
} from './sniplets'
import rights from './rights';
import incidentsRights from '@incidents/rights';

Behaviours.register('presences', {
    rights,
    incidentsRights,
    sniplets: {
        navigation,
        'exemption-form': exemptionForm,
        'absence-form': absenceForm,
        'forgotten-notebook-form': forgottenNotebookForm,
        'presences-manage': presencesManage,
        'presences-manage/reason-manage/sniplet-presences-reason-manage': presencesReasonManage,
        'presences-manage/statistics-manage/sniplet-statistics-manage': statisticsManage,
        'presences-manage/alert-manage/sniplet-presences-alert-manage': presencesAlertManage,
        'presences-manage/sniplet-presences-manage-lightbox': presencesManageLightbox,
        'presences-manage/action-manage/sniplet-presences-action-manage': presencesActionManage
    }
});
