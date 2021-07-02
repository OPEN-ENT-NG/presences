import {IStructureSlot, ITimeSlot, TimeSlotHourPeriod} from '@common/model';
import {collectiveAbsenceService, reasonService, SearchItem, ViescolaireService} from '../services';
import {ICollectiveAbsence, ICollectiveAbsenceAudience, ICollectiveAbsenceBody, ICollectiveAbsenceStudent, Reason} from '../models';
import {idiom as lang, moment, toasts} from 'entcore';
import {DateUtils, GlobalSearch} from '@common/utils';
import {SearchService, GroupService} from '@common/services';
import {IAngularEvent} from 'angular';
import {COLLECTIVE_ABSENCE_FORM_EVENTS, SNIPLET_FORM_EMIT_COLLECTIVE_ABSENCES_EVENTS} from '../core/enum/collective-absences-events';
import {AxiosError, AxiosResponse} from 'axios';

console.log('collectiveAbsenceFormSniplets');

declare let window: any;

interface IFormData {
    id?: number;
    startDate: string;
    endDate: string;
    reasonId: number;
    audiences: ICollectiveAbsenceAudience[];
    comment?: string;
    counsellorRegularisation?: boolean;

    timeSlotTimePeriod?: {
        start: ITimeSlot;
        end: ITimeSlot;
    };
    startSlot?: string;
    endSlot?: string;
}

interface IViewModel {
    openCollectiveAbsenceLightBox: boolean;
    form: IFormData;
    structureTimeSlot: IStructureSlot;
    reasons: Array<Reason>;
    display: { isFreeSchedule: boolean, confirmValidation: boolean, confirmDeletion: boolean };
    date: {startDate: Date, startTime: Date, endDate: Date, endTime: Date};
    canRegularize: boolean;
    updateAbsenceRegularisation: boolean;
    selectedReason: Reason;
    globalSearch: GlobalSearch;
    loadedStudentIds: Array<string>;

    openCreateCollectiveLightBox(): Promise<void>;

    openEditCollectiveLightBox(collectiveId: number): Promise<void>;

    closeCollectiveLightBox(): void;

    getCollectiveAbsence(id: number): Promise<void>;

    createCollectiveAbsence(): Promise<void>;

    updateCollectiveAbsence(): Promise<void>;

    deleteCollectiveAbsence(): Promise<void>;

    isValidForm(form: IFormData): boolean;

    prepareCollectiveForm(): ICollectiveAbsence;

    prepareAudiencesForm(): ICollectiveAbsenceAudience[];

    prepareDateForm(): void;

    setTimeSlot(): void;

    selectTimeSlot(hourPeriod: TimeSlotHourPeriod): void;

    selectReason(): Promise<void>;

    getStudentIssue(student: ICollectiveAbsenceStudent): string;

    getListAudienceStudentIds(audienceId: string): Array<string>;

    getListStudentIds(): Array<string>;

    getNbStudentIssues(): number;

    getNbStudentsSubmit(): number;

    toggleAudienceDisplay(audience: ICollectiveAbsenceAudience): void;
    
    removeAudience(audienceId: string): void;
    
    removeStudent(audienceId: string, studentId: string): void;

    isStudentAdded(audienceId: string, studentId: string): boolean;

    getAbsencesStatus(): void;

    getStudent(studentId: string): ICollectiveAbsenceStudent;

    isFormDateTimeValid(): boolean;

    isFormValid(): boolean;

    hasBeenAdded(studentId: string): boolean;

    isMultiple(audienceId: string, studentId: string): boolean;

    /* SEARCH METHODS */
    searchStudentsOrGroups(valueInput: string): Promise<void>;

    selectItem(valueInput: string, itemForm: SearchItem): Promise<void>;

    updateData(): Promise<void>;

    safeApply(fn?: () => void): void;
}

const vm: IViewModel = {
    safeApply: null,
    openCollectiveAbsenceLightBox: false,
    form: null,
    reasons:  null,
    selectedReason: null,
    canRegularize: false,
    updateAbsenceRegularisation: false,
    display: {isFreeSchedule: false, confirmValidation: false, confirmDeletion: false},
    structureTimeSlot: {} as IStructureSlot,
    globalSearch: undefined,
    loadedStudentIds: [],
    date: {
        startDate: moment(new Date()).toDate(),
        startTime: moment(new Date()).set({second: 0, millisecond: 0}).toDate(),
        endDate: moment(new Date()).toDate(),
        endTime: moment(new Date()).set({second: 0, millisecond: 0}).toDate()
    },

    openCreateCollectiveLightBox: async (): Promise<void> => {
        vm.openCollectiveAbsenceLightBox = true;
        vm.form = {
            audiences: [],
            timeSlotTimePeriod: {start: null, end: null}
        } as IFormData;

        vm.date = {
            startDate: moment(new Date()).toDate(),
            startTime: moment(new Date()).set({second: 0, millisecond: 0}).toDate(),
            endDate: moment(new Date()).toDate(),
            endTime: moment(new Date()).set({second: 0, millisecond: 0}).toDate()
        };

        vm.getAbsencesStatus();
        vm.safeApply();
    },

    openEditCollectiveLightBox: async (collectiveId: number): Promise<void> => {
        vm.openCollectiveAbsenceLightBox = true;
        vm.form = {
            audiences: [],
            timeSlotTimePeriod: {start: null, end: null}
        } as IFormData;

        await vm.getCollectiveAbsence(collectiveId);
        vm.setTimeSlot();
        vm.getAbsencesStatus();
        vm.safeApply();
    },

    closeCollectiveLightBox: (): void => {
        vm.openCollectiveAbsenceLightBox = false;
        vm.display = {isFreeSchedule: false, confirmValidation: false, confirmDeletion: false};
        vm.loadedStudentIds = [];
        vm.form = null;
    },

    getCollectiveAbsence: async (id: number): Promise<void> => {
        let collectiveAbsence: ICollectiveAbsence = await collectiveAbsenceService.getCollectiveAbsence(window.structure.id, id);

        vm.form.id = collectiveAbsence.id;
        vm.form.comment = collectiveAbsence.comment;
        vm.form.reasonId = collectiveAbsence.reasonId;
        await vm.selectReason();
        vm.form.counsellorRegularisation = collectiveAbsence.counsellorRegularisation;
        vm.form.audiences = collectiveAbsence.audiences;
        vm.date.startDate = moment(collectiveAbsence.startDate).toDate();
        vm.date.startTime = moment(collectiveAbsence.startDate).toDate();
        vm.date.endDate = moment(collectiveAbsence.endDate).toDate();
        vm.date.endTime = moment(collectiveAbsence.endDate).toDate();
        vm.loadedStudentIds = [];

        vm.form.audiences.forEach((audience: ICollectiveAbsenceAudience) => {
            if (audience.students) {
                audience.students.forEach((student: ICollectiveAbsenceStudent) => {
                    vm.loadedStudentIds.push(student.id);
                });

                audience.isDisplayed = true;
            }
        });

        vm.safeApply();
    },

    createCollectiveAbsence: async (): Promise<void> => {
        if (vm.form) {
            vm.prepareDateForm();
            if (!vm.isValidForm(vm.form)) {
                toasts.warning(lang.translate('presences.invalid.form'));
                return;
            }
            collectiveAbsenceService.createCollectiveAbsence(window.structure.id, vm.prepareCollectiveForm())
                .then((res: AxiosResponse) => {
                    if (res.status === 200 || res.status === 201) {
                        collectiveAbsenceForm.that.$emit(SNIPLET_FORM_EMIT_COLLECTIVE_ABSENCES_EVENTS.CREATION);
                        vm.closeCollectiveLightBox();
                        toasts.confirm('presences.collective.absences.form.create.success');
                    } else {
                        toasts.warning('presences.collective.absences.form.create.error');
                    }
                }).catch((_: AxiosError) => toasts.warning('presences.collective.absences.form.create.error'));
        }
    },

    updateCollectiveAbsence: async (): Promise<void> => {
        if (vm.form) {
            if (!vm.isValidForm(vm.form)) {
                toasts.warning(lang.translate('presences.invalid.form'));
                return;
            }
            collectiveAbsenceService.updateCollectiveAbsence(window.structure.id, vm.prepareCollectiveForm())
                .then((res: AxiosResponse) => {
                    if (res.status === 200 || res.status === 201) {
                        collectiveAbsenceForm.that.$emit(SNIPLET_FORM_EMIT_COLLECTIVE_ABSENCES_EVENTS.EDIT);
                        vm.closeCollectiveLightBox();
                        toasts.confirm('presences.collective.absences.form.edit.success');
                    } else {
                        toasts.warning('presences.collective.absences.form.edit.error');
                    }
                }).catch((_: AxiosError) => toasts.warning('presences.collective.absences.form.edit.error'));
        }
    },

    isValidForm: (form: IFormData): boolean => {
        return DateUtils.isPeriodValid(form.startDate, form.endDate);
    },

    prepareCollectiveForm: (): ICollectiveAbsence => {
        vm.prepareDateForm();
        return {
            id: vm.form.id,
            startDate: vm.form.startDate,
            endDate: vm.form.endDate,
            counsellorRegularisation: vm.form.counsellorRegularisation ? vm.form.counsellorRegularisation : false,
            reasonId: vm.form.reasonId ? vm.form.reasonId : null,
            comment: vm.form.comment,
            audiences: vm.prepareAudiencesForm()
        };
    },

    prepareAudiencesForm: (): ICollectiveAbsenceAudience[] => {

        let audiences: ICollectiveAbsenceAudience[] = [];

        if (vm.form.audiences) {
            vm.form.audiences.forEach((audience: ICollectiveAbsenceAudience) => {
                let studentIds: Array<string> = vm.getListAudienceStudentIds(audience.id);
                if (studentIds.length > 0) {
                    audiences.push({id: audience.id, studentIds: studentIds});
                }
            });
        }

        return audiences;
    },

    getListAudienceStudentIds: (audienceId: string): Array<string> => {
        let studentIds: Array<string> = [];

        let audience: ICollectiveAbsenceAudience = vm.form.audiences
            .find((item: ICollectiveAbsenceAudience) => item.id === audienceId);

        if (audience !== undefined && audience.students) {
            audience.students.forEach((student: ICollectiveAbsenceStudent) => {
                if (!vm.isMultiple(audienceId, student.id) && !student.isAbsent && !vm.hasBeenAdded(student.id)) {
                    studentIds.push(student.id);
                }
            });
        }

        return studentIds;
    },

    prepareDateForm: (): void => {

        if (vm.display.isFreeSchedule) {
            vm.form.startDate =  DateUtils.getDateFormat(vm.date.startDate, vm.date.startTime);
            vm.form.endDate = DateUtils.getDateFormat(vm.date.endDate, vm.date.endTime);
        } else if (vm.form.timeSlotTimePeriod.start && vm.form.timeSlotTimePeriod.end) {
            vm.form.startDate = DateUtils.getDateFormat(vm.date.startDate,
                DateUtils.getTimeFormatDate(vm.form.timeSlotTimePeriod.start.startHour));
            vm.form.endDate = DateUtils.getDateFormat(vm.date.endDate,
                DateUtils.getTimeFormatDate(vm.form.timeSlotTimePeriod.end.endHour));
        }
    },

    deleteCollectiveAbsence: async (): Promise<void> => {
        if (vm.form && vm.form.id) {
            collectiveAbsenceService.deleteCollectiveAbsence(window.structure.id, vm.form.id)
                .then((res: AxiosResponse) => {
                    if (res.status === 200 || res.status === 201) {
                        collectiveAbsenceForm.that.$emit(SNIPLET_FORM_EMIT_COLLECTIVE_ABSENCES_EVENTS.DELETE);
                        vm.closeCollectiveLightBox();
                        toasts.confirm('presences.collective.absences.form.delete.success');
                    } else {
                        toasts.warning('presences.collective.absences.form.delete.error');
                    }
                }).catch((_: AxiosError) => toasts.warning('presences.collective.absences.form.delete.error'));
        }
    },

    setTimeSlot: (): void => {
        let start: string = DateUtils.format(vm.date.startTime, DateUtils.FORMAT['HOUR-MIN']);
        let end: string = DateUtils.format(vm.date.endTime, DateUtils.FORMAT['HOUR-MIN']);

        vm.form.timeSlotTimePeriod = {
            start: {endHour: '', id: '', name: '', startHour: ''},
            end: {endHour: '', id: '', name: '', startHour: ''}
        };
        vm.structureTimeSlot.slots.forEach((slot: ITimeSlot) => {
            if (slot.startHour === start) {
                vm.form.timeSlotTimePeriod.start = slot;
            }
            if (slot.endHour === end) {
                vm.form.timeSlotTimePeriod.end = slot;
            }
        });

        if (!vm.form.timeSlotTimePeriod.start.startHour && vm.form.timeSlotTimePeriod.end.endHour) {
            vm.form.timeSlotTimePeriod.start = vm.form.timeSlotTimePeriod.end;
        }
        if (!vm.form.timeSlotTimePeriod.end.endHour && vm.form.timeSlotTimePeriod.start.startHour) {
            vm.form.timeSlotTimePeriod.end = vm.form.timeSlotTimePeriod.start;
        }

        vm.display.isFreeSchedule = !(vm.form.timeSlotTimePeriod.start.startHour !== '' &&
            vm.form.timeSlotTimePeriod.end.endHour !== '');
    },

    selectTimeSlot: (hourPeriod: TimeSlotHourPeriod): void => {
        switch (hourPeriod) {
            case TimeSlotHourPeriod.START_HOUR:
                vm.form.startSlot = vm.form.timeSlotTimePeriod.start != null ?
                    DateUtils.getDateFormat(new Date(vm.date.startDate),
                    DateUtils.getTimeFormatDate(vm.form.timeSlotTimePeriod.start.startHour)) : null;
                break;
            case TimeSlotHourPeriod.END_HOUR:
                vm.form.endSlot = vm.form.timeSlotTimePeriod.end != null ?
                    DateUtils.getDateFormat(new Date(vm.date.endDate),
                    DateUtils.getTimeFormatDate(vm.form.timeSlotTimePeriod.end.endHour)) : null;
                break;
            default:
                return;
        }
    },

    selectReason: async (): Promise<void> => {
        vm.selectedReason = vm.reasons ? vm.reasons.find((reason: Reason) => reason.id === vm.form.reasonId) : null;
        vm.canRegularize = (vm.selectedReason) ? (!vm.selectedReason.proving) : false;
        vm.updateAbsenceRegularisation = vm.selectedReason ? vm.selectedReason.proving : false;
        vm.form.counsellorRegularisation = vm.selectedReason ? vm.selectedReason.proving : false;
        await vm.updateData();
    },

    getStudentIssue: (student: ICollectiveAbsenceStudent): string => {
        if (student.isUpdated) {
            return lang.translate('presences.collective.absences.form.modified.individually');
        } else if (student.isAbsent) {
            return lang.translate('presences.collective.absences.form.absent.on.period');
        }

        return '';
    },


    getListStudentIds: (): Array<string> => {
        let studentIds: Array<string> = [];
        if (vm.form && vm.form.audiences) {
            vm.form.audiences.forEach((audience: ICollectiveAbsenceAudience) => {
                if (audience.students) {
                    audience.students.forEach((student: ICollectiveAbsenceStudent) => {
                        if (!vm.isMultiple(audience.id, student.id)) {
                            studentIds.push(student.id);
                        }
                    });
                }
            });
        }
        return studentIds;
    },

    getNbStudentIssues: (): number => {
      let issuesStudentIds: Array<string> = [];

      if (vm.form && vm.form.audiences) {
          vm.form.audiences.forEach((audience: ICollectiveAbsenceAudience) => {
              if (audience.students) {
                  audience.students.forEach((student: ICollectiveAbsenceStudent) => {
                      if ((student.isAbsent || student.isUpdated) && issuesStudentIds.indexOf(student.id) === -1) {
                          issuesStudentIds.push(student.id);
                      }
                  });
              }
          });
      }

      return issuesStudentIds.length;
    },

    getNbStudentsSubmit: (): number => {

        let absentStudentIds: Array<string> = [];
        if (vm.form && vm.form.audiences) {
            vm.form.audiences.forEach((audience: ICollectiveAbsenceAudience) => {
                if (audience.students) {
                    audience.students.forEach((student: ICollectiveAbsenceStudent) => {
                        if ((student.isAbsent) && absentStudentIds.indexOf(student.id) === -1) {
                            absentStudentIds.push(student.id);
                        }
                    });
                }
            });
        }
        return vm.getListStudentIds().length - absentStudentIds.length;
    },

    toggleAudienceDisplay: (audience: ICollectiveAbsenceAudience): void => {
        audience.isDisplayed = !audience.isDisplayed;
    },

    removeAudience: (audienceId: string): void => {
        vm.form.audiences.forEach((async (value: ICollectiveAbsenceAudience, index: number) => {
            if (value.id === audienceId) {

                if (vm.form.id && value.students && value.students.length > 0) {
                    let studentIds: Array<string> = [];
                    value.students.forEach((student: ICollectiveAbsenceStudent) => {
                        studentIds.push(student.id);
                    });
                    collectiveAbsenceService.removeAbsenceFromCollectiveAbsence(window.structure.id,
                        vm.form.id, studentIds).then((res: AxiosResponse) => {
                        if (res.status === 200) {
                            vm.form.audiences.splice(index, 1);
                            vm.updateData();
                            toasts.confirm('presences.collective.absences.form.delete.absence.success');
                            collectiveAbsenceForm.that.$emit(SNIPLET_FORM_EMIT_COLLECTIVE_ABSENCES_EVENTS.EDIT);
                        } else {
                            toasts.warning('presences.collective.absences.form.delete.absence.error');
                        }
                    }).catch((_: AxiosError) => toasts.warning('presences.collective.absences.form.delete.absence.error'));
                } else {
                    vm.form.audiences.splice(index, 1);
                }
            }
        }));
        vm.safeApply();
    },

    removeStudent: (audienceId: string, studentId: string): void => {
        if (vm.form.id && vm.hasBeenAdded(studentId)) {
            collectiveAbsenceService.removeAbsenceFromCollectiveAbsence(window.structure.id,
                vm.form.id, [studentId]).then((res: AxiosResponse) => {
                if (res.status === 200) {
                    vm.updateData();
                    toasts.confirm('presences.collective.absences.form.delete.absence.success');
                    collectiveAbsenceForm.that.$emit(SNIPLET_FORM_EMIT_COLLECTIVE_ABSENCES_EVENTS.EDIT);
                } else {
                    toasts.warning('presences.collective.absences.form.delete.absence.error');
                }
            }).catch((_: AxiosError) => toasts.warning('presences.collective.absences.form.delete.absence.error'));
        }

        vm.form.audiences.forEach((audience: ICollectiveAbsenceAudience) => {
            if (audience.students) {
                audience.students.forEach((student: ICollectiveAbsenceStudent, index: number) => {
                    if (student.id === studentId) {
                        audience.students.splice(index, 1);
                    }
                    if (audience.students.length === 0) {
                        vm.removeAudience(audienceId);
                    }
                });
            }
        });
        
        vm.safeApply();
    },

    isStudentAdded: (audienceId: string, studentId: string): boolean => {
        let audience: ICollectiveAbsenceAudience = vm.form.audiences
            .find((audienceItem: ICollectiveAbsenceAudience) => audienceItem.id === audienceId);

        return (audience !== undefined && audience.students) ?
            audience.students.find((student: ICollectiveAbsenceStudent) => student.id === studentId) !== undefined :
            false;
    },

    getAbsencesStatus: (): void => {
        if (vm.form) {

            let studentIds: Array<string> = vm.getListStudentIds();
            
            if (studentIds.length === 0) {
                return;
            }
            
            vm.prepareDateForm();

            let collectiveParams: ICollectiveAbsenceBody = {
                startDate: vm.form.startDate,
                endDate: vm.form.endDate,
                studentIds: studentIds,
                collectiveId: vm.form.id ? vm.form.id : null
            };
            collectiveAbsenceService.getStudentsAbsencesStatus(window.structure.id, collectiveParams)
                .then(
                (res: Array<ICollectiveAbsenceStudent>) => {
                    res.forEach((studentRes: ICollectiveAbsenceStudent) => {

                        let student: ICollectiveAbsenceStudent = vm.getStudent(studentRes.studentId);

                        if (student) {
                            student.isAbsent = studentRes.isAbsent;
                            student.isUpdated = studentRes.isUpdated;
                        }
                    });
                    vm.safeApply();
                }
            );
        }
    },

    getStudent: (studentId: string): ICollectiveAbsenceStudent => {
        let student: ICollectiveAbsenceStudent = null;

        vm.form.audiences.forEach((audience: ICollectiveAbsenceAudience) => {
            if (audience.students) {
                let studentFind: ICollectiveAbsenceStudent = audience.students
                    .find((student: ICollectiveAbsenceStudent) => student.id === studentId);

                if (studentFind !== undefined) {
                    student = studentFind;
                    return student;
                }
            }
        });

        return student;
    },

    isFormDateTimeValid: (): boolean => {
        return vm.display.isFreeSchedule ?
            (vm.date.startTime !== null && vm.date.endTime !== null) :
            (vm.form.timeSlotTimePeriod &&
                vm.form.timeSlotTimePeriod.start !== null &&
                vm.form.timeSlotTimePeriod.end !== null);
    },

    isFormValid: (): boolean => {
        return vm.form && vm.date.startDate && vm.date.endDate &&
            vm.isFormDateTimeValid() &&
            vm.form.audiences.length > 0 &&
            (vm.getNbStudentsSubmit() > 0);
    },

    hasBeenAdded: (studentId: string): boolean => {
        return vm.loadedStudentIds.indexOf(studentId) !== -1;
    },

    isMultiple: (audienceId: string, studentId: string): boolean => {
        let isMultiple: boolean = false;
        let firstElementAudienceId: string = null;
        vm.form.audiences.forEach((audience: ICollectiveAbsenceAudience) => {
            if (audience.students) {
                audience.students.forEach((student: ICollectiveAbsenceStudent) => {
                    if (student.id === studentId) {
                        if (firstElementAudienceId === null) {
                            firstElementAudienceId = audience.id;
                        }
                        isMultiple = true;
                    }
                });
            }
        });

        return (isMultiple && (audienceId !== firstElementAudienceId));
    },

    /* Search methods */

    searchStudentsOrGroups: async (valueInput: string): Promise<void> => {
        await vm.globalSearch.searchStudentsOrGroups(valueInput);
        vm.safeApply();
    },

    selectItem: async (valueInput: string, itemForm: SearchItem): Promise<void> => {
        vm.globalSearch.selectItems(valueInput, itemForm);

        let audience: ICollectiveAbsenceAudience = (vm.form.audiences
            .find((audience: ICollectiveAbsenceAudience) => audience.name === itemForm.groupName));

        await vm.globalSearch.getStudentsFromGroup(itemForm.groupId, GlobalSearch.TYPE.group);
        let students: SearchItem[] = vm.globalSearch.getStudents();


        /* if search item result is USER */
        if (itemForm.type === GlobalSearch.TYPE.user && !vm.isStudentAdded(itemForm.groupId, itemForm.id)) {

            if (audience === undefined) {
                vm.form.audiences.push(
                    {
                        id: itemForm.groupId,
                        name: itemForm.groupName,
                        students: [{id: itemForm.id, displayName: itemForm.displayName}],
                        countStudents: students.length,
                        isDisplayed: true
                    } as ICollectiveAbsenceAudience
                );
            } else {
                audience.students.push(
                    {id: itemForm.id, displayName: itemForm.displayName}
                );
            }
        }
        /* case if it is a class */
        if (itemForm.type === GlobalSearch.TYPE.group) {

            if (audience === undefined) {

                vm.form.audiences.push({
                    id: itemForm.groupId,
                    name: itemForm.groupName,
                    students: [],
                    isDisplayed: true
                } as ICollectiveAbsenceAudience);
            }

            audience = (vm.form.audiences.find(audience => audience.name === itemForm.groupName));
            audience.countStudents = students.length;

            students.forEach((student: SearchItem) => {

                if (!vm.isStudentAdded(audience.id, student.id)) {
                    audience.students.push({
                        id: student.id,
                        displayName: student.lastName + ' ' + student.firstName
                    });
                }
            });

            if (audience.students.length === 0) {
                vm.removeAudience(audience.id);
            }
        }

        vm.globalSearch.search = '';
        await vm.updateData();
    },

    updateData: async (): Promise<void> => {
        await vm.getAbsencesStatus();
        vm.safeApply();
    }

};

export const collectiveAbsenceForm = {
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            vm.globalSearch = new GlobalSearch(window.structure.id, SearchService, GroupService);
            vm.structureTimeSlot = await ViescolaireService.getSlotProfile(window.structure.id);
            vm.reasons = await reasonService.getReasons(window.structure.id);

            collectiveAbsenceForm.that = this;

            vm.safeApply = this.safeApply;
        },
        setHandler: function () {
            this.$on(COLLECTIVE_ABSENCE_FORM_EVENTS.CREATE,
                () => vm.openCreateCollectiveLightBox());
            this.$on(COLLECTIVE_ABSENCE_FORM_EVENTS.EDIT,
                (event: IAngularEvent, collectiveId: { id: number }) => vm.openEditCollectiveLightBox(collectiveId.id));
        }
    }
};