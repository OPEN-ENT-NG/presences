import {_, moment, ng, notify} from 'entcore';
import {AbsenceService, EventRequest, EventService, IViescolaireService, ReasonService,
    SearchItem, SearchService} from '../../services';
import {CounsellorAbsence, EventResponse, Events, ITimeSlot, Reason, Students, IEvent} from '../../models';
import {DateUtils} from '@common/utils/date';
import {IStructureSlot} from '@common/model/Viescolaire';

interface ViewModel {
    absences: Array<CounsellorAbsence | IEvent>;
    reasons: Array<Reason>;
    timeSlots: ITimeSlot[];
    students: Students;
    searchResults: SearchItem[];
    params: {
        start: Date,
        end: Date,
        students: Array<SearchItem>,
        groups: Array<SearchItem>,
        search: string
    };
    provingReasonMap: any;

    load(): Promise<void>;

    searchStudentOrGroup(searchText: string): Promise<void>;

    selectStudentOrGroup(model: SearchItem, option: SearchItem): void;

    removeStudent(student: SearchItem): void;

    removeGroup(group: SearchItem): void;

    setAbsenceRegularisation(absence: CounsellorAbsence): void;

    regularizeAbsence(absence: CounsellorAbsence | IEvent): void;

    showAbsenceRange(absence: CounsellorAbsence): String;

    getAbsenceEvents(structureId: string, students: string[], classesIds: string[], startDate: string,
                     endDate: string): Promise<Array<IEvent>>;

    updateReasonAbsenceEvent(absence: CounsellorAbsence | any): void;

    updateRegularisationAbsenceEvent(absence: CounsellorAbsence | any): void;

    getTimeSlots(): Promise<void>;
}

declare let window: any;

export const absencesController = ng.controller('AbsenceController', ['$scope', 'AbsenceService', 
    'EventService', 'ReasonService', 'SearchService', 'ViescolaireService',
    function ($scope, AbsenceService: AbsenceService, eventService: EventService, ReasonService: ReasonService,
              SearchService: SearchService, viescolaireService: IViescolaireService) {
        const vm: ViewModel = this;
        vm.absences = [];
        vm.reasons = [];
        vm.timeSlots = [];
        vm.provingReasonMap = {};
        vm.params = {
            start: moment().add(-5, 'days').toDate(),
            end: new Date(),
            students: [],
            groups: [],
            search: null
        };

        vm.students = new Students();
        vm.searchResults = [];

        /**
         * Load all absences.
         */
        vm.load = async (): Promise<void> => {
            try {
                let start: string = moment(vm.params.start).format(DateUtils.FORMAT['YEAR-MONTH-DAY']);
                let end: string = moment(vm.params.end).format(DateUtils.FORMAT['YEAR-MONTH-DAY']);
                let students: string[] = [];
                let groups: string[] = [];
                vm.params.students.forEach(student => students.push(student.id));
                vm.params.groups.forEach(group => groups.push(group.id));
                await vm.getTimeSlots();

                vm.absences = await AbsenceService.getCounsellorAbsence(window.structure.id, students, groups, start,
                    end, null, false, null);

                let absenceEvents: IEvent[] = await vm.getAbsenceEvents(window.structure.id, students, groups,
                    start, end);

                // Merge absences with absence events.
                vm.absences = vm.absences.concat(absenceEvents);
                $scope.safeApply();
            } catch (err) {
                notify.error('presences.absences.load.failed');
            }
        };

        async function loadReasons() {
            vm.reasons = await ReasonService.getReasons(window.structure.id);
            vm.reasons.forEach(reason => vm.provingReasonMap[reason.id] = reason.proving);
            $scope.safeApply();
        }

        /**
         * Retrieve students and groups based on the user query.
         * @param searchText The user query
         */
        vm.searchStudentOrGroup = async (searchText: string): Promise<void> => {
            vm.searchResults = await SearchService.search(window.structure.id, searchText);
            $scope.safeApply();
        };


        /**
         * Select the item from the absence search results.
         * @param model The user query
         * @param option The selected student or group from the search results
         */
        vm.selectStudentOrGroup = (model: SearchItem, option: SearchItem): void => {
            if (!_.find(option, vm.params.students) && option.type === 'USER') {
                vm.params.students.push(option);
            } else if (!_.find(option, vm.params.groups) && option.type === 'GROUP') {
                vm.params.groups.push(option);
            }

            vm.searchResults = null;
            vm.params.search = '';
            vm.load();
        };

        /**
         * Remove a student from the search selection.
         * @param student The student to remove.
         */
        vm.removeStudent = (student: SearchItem): void => {
            vm.params.students = _.without(vm.params.students, student);
            vm.load();
        };

        /**
         * Remove a group from the search selection.
         * @param group The group to remove.
         */
        vm.removeGroup = (group: SearchItem): void => {
            vm.params.groups = _.without(vm.params.groups, group);
            vm.load();
        };

        /**
         * Set regularisation state based on the reason.
         * @param absence
         */
        vm.setAbsenceRegularisation = (absence: CounsellorAbsence | IEvent) => {
            absence.counsellor_regularisation = vm.provingReasonMap[absence.reason_id];
            vm.regularizeAbsence(absence);
            $scope.safeApply();
        };

        /**
         * Remove absence from list when regularized.
         * @param absence
         */
        vm.regularizeAbsence = (absence: CounsellorAbsence | IEvent) => {
            if (absence.counsellor_regularisation) {
                vm.absences = vm.absences.filter(abs => abs.id !== absence.id);
                $scope.safeApply();
            }
        };

        vm.showAbsenceRange = (absence: CounsellorAbsence): String => {
            let result: String = `${DateUtils.format(absence.start_date, DateUtils.FORMAT['DAY-MONTH-YEAR'])} ${DateUtils.format(absence.start_date, DateUtils.FORMAT['HOUR-MINUTES'])}`;

            if (DateUtils.getDayNumberDifference(absence.start_date, absence.end_date))
                result += ` - ${DateUtils.format(absence.end_date, DateUtils.FORMAT['DAY-MONTH-YEAR'])} `;
            else
                result += "-";

            return result + DateUtils.format(absence.end_date, DateUtils.FORMAT['HOUR-MINUTES']);
        };

        /**
         * Get events with absence type.
         */
        vm.getAbsenceEvents = async (structureId: string, students: string[], classesIds: string[], startDate: string,
                               endDate: string): Promise<Array<IEvent>> => {

            let absenceEvents: IEvent[] = [];

            let eventRequest: EventRequest = {
                structureId: structureId,
                startDate: startDate,
                endDate: endDate,
                eventType: '1',
                userIds: students,
                classesIds: classesIds,
                regularized: false
            };

            await eventService.get(eventRequest).then(
                (res: { pageCount: number, events: EventResponse[], all: EventResponse[] }) => {
                    res.all.forEach((student: EventResponse) => {

                        for (let i = 0; i < student.events.length ; i++) {
                            let event: IEvent = student.events[i];
                            let previousEvent: IEvent = (i > 0) ? student.events[i - 1] : null;
                            let nextEvent: IEvent = (i < (student.events.length - 1)) ? student.events[i + 1] : null;

                            // Checking if events not following on multiple slots
                            if (((previousEvent !== null && event.start_date !== previousEvent.end_date) || previousEvent == null) &&
                                ((nextEvent !== null && event.end_date !== nextEvent.start_date) || nextEvent == null)) {
                                let absenceEvent: IEvent = {
                                    id: event.id,
                                    start_date: event.start_date,
                                    end_date: event.end_date,
                                    student_id: student.student.id,
                                    reason_id: event.reason_id,
                                    counsellor_regularisation: event.counsellor_regularisation,
                                    student: {id: student.student.id, name: student.student.displayName, className: student.student.classeName}
                                };

                                /* Check if absence on a unique slot and remove duplicates */
                                if (!absenceEvent.reason_id &&
                                    absenceEvent.student && absenceEvent.student.name &&
                                    vm.absences.indexOf(vm.absences.find(
                                        (abs: CounsellorAbsence) => {
                                            return (DateUtils.isBetween(abs.start_date, abs.end_date,
                                                absenceEvent.start_date, absenceEvent.end_date) &&
                                                (abs.student.id === absenceEvent.student_id));
                                        })) === -1) {
                                    absenceEvents.push(absenceEvent);
                                }
                            }
                        }
                    });
                }
            );
            return absenceEvents;
        };

        /**
         * Update the absence reason
         * @param absence an absence, can be an absence event.
         */
        vm.updateReasonAbsenceEvent = async (absence: CounsellorAbsence | IEvent): Promise<void> => {
            if (absence instanceof CounsellorAbsence) {
                await absence.updateAbsence();
            } else {
                await new Events().updateReason([absence], absence.reason_id, absence.student.id, window.structure.id);
            }
        };

        /**
         * Regularize th absence
         * @param absence absence an absence, can be an absence event.
         */
        vm.updateRegularisationAbsenceEvent = async (absence: CounsellorAbsence | IEvent): Promise<void> => {
            if (absence instanceof CounsellorAbsence) {
                await absence.updateRegularisation();
            } else {
                await new Events().updateRegularized([absence], absence.counsellor_regularisation, absence.student.id, window.structure.id);
            }
        };

        /**
         * Retrieve the structure time slots.
         */
        vm.getTimeSlots = async (): Promise<void> => {
            await viescolaireService.getSlotProfile(window.structure.id).then((structureSlot: IStructureSlot) => {
                vm.timeSlots = structureSlot.slots;
            });
        };

        /* Events handler */
        $scope.$watch(() => window.structure, async () => {
            if (window.structure) {
                await vm.load();
                await loadReasons();
            }
        });
    }]);