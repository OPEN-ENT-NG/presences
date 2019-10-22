import {model, moment, ng} from 'entcore';
import {
    CalendarService,
    Course,
    GroupService,
    SearchItem,
    SearchService,
    StructureService,
    TimeSlot
} from '../services';
import {Scope} from './main';
import {User} from '../models';
import {DateUtils} from '@common/utils';
import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from "@common/model";
import {ABSENCE_FORM_EVENTS} from "../sniplets";

declare let window: any;

interface ViewModel {
    show: { loader: boolean, exemption: { start_date: string, end_date: string } };
    courses: {
        list: Array<Course>
    };
    slots: { list: Array<TimeSlot> };
    filter: {
        search: {
            item: string,
            items: Array<SearchItem>
        },
        student: User,
        students: Array<User>
    };

    selectItem(model: any, student: any): void;

    searchItem(value: string): void;

    changeAbsence(item: Course): string;

    loadCourses(): Promise<void>;

    formatExemptionDate(date: any): string;

    editAbsenceForm(item: Course, items): void;
}

interface CalendarScope extends Scope {
    hoverExemption($event, exemption: { start_date: string, end_date: string }): void;

    hoverOutExemption(): void;

    isAbsenceOnly(item): boolean;

    isAbsenceJustifiedOnly(item): boolean;

    isGlobalAbsence(item): boolean;

    isGlobalAbsenceReason(item): boolean;

    isPastItem(item): boolean;
}

export const calendarController = ng.controller('CalendarController', ['$scope', 'route', '$location', 'StructureService', 'CalendarService', 'GroupService', 'SearchService',
    function ($scope: CalendarScope, route, $location, StructureService: StructureService, CalendarService: CalendarService, GroupService: GroupService, SearchService: SearchService) {
        const vm: ViewModel = this;
        vm.show = {
            loader: true,
            exemption: null
        };
        vm.filter = {
            search: {
                item: '',
                items: null
            },
            student: null,
            students: null
        };
        vm.courses = {list: []};
        vm.slots = {list: []};

        if ('date' in window.item) {
            const date = moment(window.item.date);
            model.calendar.setDate(date);
        } else {
            model.calendar.setDate(moment());
        }

        // model.calendar.eventer.on('calendar.create-item', () => {
        //     console.info(model.calendar.newItem);
        //     console.info(model.calendar.newItem.beginning.format());
        //     console.info(model.calendar.newItem.beginning.toString());
        //     console.info(model.calendar.newItem.beginning.isValid());
        //     console.info(model.calendar.newItem.beginning.toLocaleString());
        // });

        $scope.$watch(() => window.structure, async () => {
            const structure_slots = await StructureService.getSlotProfile(window.structure.id);
            if (Object.keys(structure_slots).length > 0) vm.slots.list = structure_slots.slots;
            else vm.slots.list = null;
            $scope.safeApply();
        });

        model.calendar.on('date-change', initCalendar);

        $scope.$on('$destroy', () => model.calendar.callbacks['date-change'] = []);

        async function initCalendar() {
            vm.show.loader = true;
            $scope.safeApply();
            const {item, structure} = window;
            if (item === null || structure === null) {
                $location.path('/');
                return;
            }

            vm.filter.student = item;
            vm.filter.students = await CalendarService.getStudentsGroup(item.groupId);
            if (item.type === 'GROUP' && vm.filter.students.length > 0) {
                vm.filter.student = vm.filter.students[0];
                window.item = vm.filter.student;
                $location.path(`/calendar/${vm.filter.student.id}`);
            }
            await vm.loadCourses();
        }

        vm.changeAbsence = function (item): string {
            if ('hash' in item) changeAbsenceView(item);
            return "";
        };

        function changeAbsenceView(item: Course) {
            let courseItems = document
                .getElementById(`absent${item.dayOfWeek}-${item.hash}`)
                .closest('.fiveDays')
                .querySelectorAll('.course-item:not(.is-absence)');
            let items = [];
            Array.from(courseItems).forEach((course: HTMLElement) => {
                const item = course.closest('.schedule-item');
                items.push(item);
                if (item === undefined || item === null) return;
                if ((item.parentNode as Element).querySelector('.globalAbsence')) {
                    item.setAttribute("class", "schedule-item schedule-globalAbsence");
                } else if ((item.parentNode as Element).querySelector('.globalAbsenceReason')) {
                    item.setAttribute("class", "schedule-item schedule-globalAbsenceReason");
                } else {
                    item.setAttribute("class", "schedule-item schedule-course");
                }
            });

            let absenceItems = items.filter(item =>
                item.getAttribute("class") === "schedule-item schedule-globalAbsence");

            let absenceReasonItems = items.filter(item =>
                item.getAttribute("class") === "schedule-item schedule-globalAbsenceReason");

            let coursesItems = items.filter(item =>
                item.getAttribute("class") !== "schedule-item schedule-globalAbsence" &&
                item.getAttribute("class") !== "schedule-item schedule-globalAbsenceReason");

            coursesItems.forEach(course => {

                /* Coloring course in red if inside global absent bloc */
                absenceItems.forEach(absenceItem => {
                    if (isItemInside(course, absenceItem)) {
                        course.querySelectorAll(".course-item")[0].classList.add("isAbsent");
                    }
                });

                /* Coloring course in pink if inside global justified absent bloc */
                absenceReasonItems.forEach(absenceReasonItem => {
                    if (isItemInside(course, absenceReasonItem)) {
                        course.querySelectorAll(".course-item")[0].classList.add("isJustifiedAbsent");
                    }
                });
            });
        }

        function isItemInside(item, itemAbsence): boolean {
            let itemPosX = item.offsetLeft;
            let itemPosY = item.offsetTop;
            let itemHeight = item.clientHeight;
            let itemWidth = item.clientWidth;
            let itemBottom = itemPosY + itemHeight - 2;
            let itemRight = itemPosX + itemWidth;

            let itemAbsenceX = itemAbsence.offsetLeft;
            let itemAbsenceY = itemAbsence.offsetTop;
            let itemAbsenceHeight = itemAbsence.clientHeight;
            let itemAbsenceWidth = itemAbsence.clientWidth;
            let itemAbsenceBottom = itemAbsenceY + itemAbsenceHeight - 2;
            let itemAbsenceRight = itemAbsenceX + itemAbsenceWidth;
            return !(itemBottom < itemAbsenceY || itemPosY > itemAbsenceBottom || itemRight < itemAbsenceX || itemPosX > itemAbsenceRight);
        }

        vm.loadCourses = async function (student = vm.filter.student) {
            vm.show.loader = true;
            if (vm.filter.student.id !== student.id) {
                vm.filter.student = student;
                window.item = student;
            }
            const {structure} = window;
            const start = DateUtils.format(model.calendar.firstDay, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
            const end = DateUtils.format(DateUtils.add(model.calendar.firstDay, 1, 'w'), DateUtils.FORMAT["YEAR-MONTH-DAY"]);
            vm.courses.list = await CalendarService.getCourses(structure.id, student.id, start, end);
            vm.show.loader = false;
            $scope.safeApply();
        };

        $scope.$on(SNIPLET_FORM_EMIT_EVENTS.CREATION, () => {
            let diff = 7;
            if (!model.calendar.display.saturday) diff--;
            if (!model.calendar.display.synday) diff--;
            $scope.$broadcast(SNIPLET_FORM_EVENTS.SET_PARAMS, {
                student: window.item,
                start_date: model.calendar.firstDay,
                end_date: DateUtils.add(model.calendar.firstDay, diff)
            });
        });

        $scope.$on(SNIPLET_FORM_EMIT_EVENTS.FILTER, initCalendar);
        $scope.$on(SNIPLET_FORM_EMIT_EVENTS.EDIT, initCalendar);
        $scope.$on(SNIPLET_FORM_EMIT_EVENTS.DELETE, initCalendar);


        vm.selectItem = function (model, item) {
            const needsToLoadGroup = (window.item.groupId !== item.groupId) || item.type === 'GROUP';
            window.item = item;
            vm.filter.search.items = undefined;
            vm.filter.search.item = '';
            if (needsToLoadGroup) {
                initCalendar();
            } else {
                vm.filter.student = item;
                vm.loadCourses();
            }
        };

        vm.searchItem = async function (value) {
            const structureId = window.structure.id;
            try {
                vm.filter.search.items = await SearchService.search(structureId, value);
            } catch (err) {
                vm.filter.search.items = [];
            } finally {
                $scope.safeApply();
            }
        };

        $scope.hoverExemption = function ($event, exemption) {
            const {width, height} = getComputedStyle(hover);
            let {x, y} = $event.target.closest('.exemption-label').getBoundingClientRect();
            hover.style.top = `${y - parseInt(height)}px`;
            hover.style.left = `${x - (parseInt(width) / 4)}px`;
            hover.style.display = 'flex';
            vm.show.exemption = exemption;
            $scope.safeApply();
        };

        $scope.hoverOutExemption = function () {
            hover.style.display = 'none';
        };

        $scope.isAbsenceOnly = function (item): boolean {
            return item.absence && item.absenceReason === 0;
        };

        $scope.isAbsenceJustifiedOnly = function (item): boolean {
            return item.absence && item.absenceReason > 0;
        };

        $scope.isGlobalAbsence = function (item): boolean {
            return item._id === '0' && item.absence && item.absenceReason === 0;
        };

        $scope.isGlobalAbsenceReason = function (item): boolean {
            return item._id === '0' && item.absence && item.absenceReason > 0;
        };

        vm.editAbsenceForm = function (item, items): void {
            if (item._id !== "0") {
                let absenceItem = items
                    .find(absence =>
                        absence._id === '0' &&
                        DateUtils.format(absence.startDate, DateUtils.FORMAT["YEAR-MONTH-DAY"]) ===
                        DateUtils.format(item.startDate, DateUtils.FORMAT["YEAR-MONTH-DAY"])
                        &&
                        DateUtils.format(absence.endDate, DateUtils.FORMAT["YEAR-MONTH-DAY"]) ===
                        DateUtils.format(item.endDate, DateUtils.FORMAT["YEAR-MONTH-DAY"]));
                if (absenceItem === undefined) {
                    return;
                }
                if (item.startDate >= absenceItem.startDate && item.endDate <= absenceItem.endDate) {
                    $scope.$broadcast(ABSENCE_FORM_EVENTS.EDIT, absenceItem);
                } else {
                    return;
                }
            }
            $scope.$broadcast(ABSENCE_FORM_EVENTS.EDIT, item);
        };

        vm.formatExemptionDate = function (date) {
            return DateUtils.format(date, DateUtils.FORMAT["DAY-MONTH-YEAR"]);
        };

        const hover = document.getElementById('exemption-hover');
    }]);