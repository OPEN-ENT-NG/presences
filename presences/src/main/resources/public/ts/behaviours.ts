import {Behaviours} from 'entcore';
import {
    absenceMementoWidget,
    disciplineManage,
    eventsForm,
    exemptionForm,
    forgottenNotebookForm,
    collectiveAbsenceForm,
    navigation,
    presenceForm,
    presencesActionManage,
    presencesAlertManage,
    presencesManage,
    presencesManageLightbox,
    presencesReasonManageAbsence,
    statisticsManage,
    presencesMultipleSlotsManage,
    presencesReasonManageLateness
} from './sniplets';
import rights from './rights';
import incidentsRights from '@incidents/rights';

Behaviours.register('presences', {
    rights,
    incidentsRights,
    sniplets: {
        navigation,
        'exemption-form': exemptionForm,
        'event-form/sniplet-events-form': eventsForm,
        'forgotten-notebook-form': forgottenNotebookForm,
        'collective-absence-form/sniplet-collective-absence-form': collectiveAbsenceForm,
        'presences-manage': presencesManage,
        'presences-manage/reason-manage/sniplet-presences-reason-manage-absence': presencesReasonManageAbsence,
        'presences-manage/reason-manage/sniplet-presences-reason-manage-lateness': presencesReasonManageLateness,
        'presences-manage/statistics-manage/sniplet-statistics-manage': statisticsManage,
        'presences-manage/alert-manage/sniplet-presences-alert-manage': presencesAlertManage,
        'presences-manage/sniplet-presences-manage-lightbox': presencesManageLightbox,
        'presences-manage/action-manage/sniplet-presences-action-manage': presencesActionManage,
        'presences-manage/discipline-manage/sniplet-presences-disciplines-manage': disciplineManage,
        'presences-manage/multiple-slots-manage/sniplet-presences-multiple-slots-manage': presencesMultipleSlotsManage,
        'memento/absences': absenceMementoWidget,
        'presence-form': presenceForm,
    }
});
