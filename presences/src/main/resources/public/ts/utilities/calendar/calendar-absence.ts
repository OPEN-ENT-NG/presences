import {angular, moment} from "entcore";
import {ABSENCE_FORM_EVENTS} from "../../sniplets";
import {SNIPLET_FORM_EMIT_EVENTS} from "@common/model";
import {TimeSlotData} from "../../models/";
import {EventsUtils} from "../../utilities/";
import {DateUtils} from "@common/utils";

/**
 * ⚠ This class is used for calendar while doing ACTION such as
 * [ANIMATION DRAG & DROP + click time-slot] to create absence ⚠
 */

export class CalendarAbsenceUtils {

    /**
     * Click on any timeslot to declare an absence
     */
    public static async actionAbsenceTimeSlot($scope): Promise<void> {
        let slots = $('.days .timeslot');
        slots.click(function () {
            let _scope = angular.element(arguments[0].target).scope();
            let absenceDate = moment(_scope['day'].date).toDate();
            let startTime = moment(new Date).set({
                hours: _scope['timeslot'].start,
                minute: 0,
                second: 0,
                millisecond: 0
            }).toDate();
            let endTime = moment(new Date).set({
                hours: _scope['timeslot'].end,
                minute: 0,
                second: 0,
                millisecond: 0
            }).toDate();
            let form = {
                startDate: absenceDate,
                endDate: absenceDate,
                startTime: startTime,
                endTime: endTime
            };
            $scope.$broadcast(ABSENCE_FORM_EVENTS.OPEN, form);
        })
    }

    /**
     * [Animation] slide to drag & drop any time slot to declare absence
     */
    public static async actionDragAbsence($scope): Promise<void> {
        let isClickHold = false;
        let slots = $('.days .timeslot, .course-item-container');
        let timeSlots: TimeSlotData[] = [];
        slots.each((i: number, e: Element) => {
            let _scope = angular.element(e).scope();
            if (e.className.startsWith("timeslot")) {
                let timeSlot: TimeSlotData = CalendarAbsenceUtils.getTimeSlotData(i, e, _scope);
                timeSlots.push(timeSlot);
            } else {
                let timeSlot: TimeSlotData = CalendarAbsenceUtils.getCourseSlotData(i, e, _scope);
                timeSlots.push(timeSlot);
            }
        });
        timeSlots.forEach(ts => {
            if (ts.div.className.startsWith("course")) {
                let timeslotIndex = timeSlots.findIndex(t =>
                    DateUtils.isMatchDate(t.startDate, t.endDate, ts.startDate, ts.endDate) &&
                    t.div.className.startsWith("timeslot"));
                ts.index = timeSlots[timeslotIndex].index;
            }
        });
        timeSlots.sort((a, b) => a.index - b.index);
        let timeSlotsFetched: TimeSlotData[] = [];
        let mdIndex;
        slots /* click on timeslot first time */
            .mousedown((e: JQueryMouseEventObject) => {
                isClickHold = true;
                CalendarAbsenceUtils.resetTimeSlotsFetched(timeSlots, timeSlotsFetched);
                if (isClickHold) {
                    /* store position mouse event down initial */
                    mdIndex = CalendarAbsenceUtils.findElement(timeSlots, e);
                }
            }) /* moving current timeslot on timeslots */
            .mouseenter((e: JQueryMouseEventObject) => {
                if (isClickHold) {
                    CalendarAbsenceUtils.resetTimeSlotsFetched(timeSlots, timeSlotsFetched);
                    CalendarAbsenceUtils.colorTimeSlots(mdIndex, e, timeSlotsFetched, timeSlots);
                }
            })
            .mouseup(() => {
                isClickHold = false;
                let form = CalendarAbsenceUtils.formatForm(timeSlotsFetched);
                if (form) {
                    $scope.$broadcast(ABSENCE_FORM_EVENTS.OPEN, form);
                }
            });
        $scope.$on(SNIPLET_FORM_EMIT_EVENTS.CANCEL, () => CalendarAbsenceUtils.resetTimeSlotsFetched(timeSlots, timeSlotsFetched));
    }

    /**
     * [Animation] Function that change CSS and fetch its object timeslot to timeSlotsFetched array
     */
    private static colorTimeSlots(mdIndex: number, e: JQueryMouseEventObject,
                                  timeSlotsFetched: TimeSlotData[], timeSlots: TimeSlotData[]) {
        /* store position mouse event enter */
        let meIndex = CalendarAbsenceUtils.findElement(timeSlots, e);
        let foundMd = false;
        let foundMs = false;

        timeSlots.forEach(t => {
            let currentCheck = false;
            if (t.index === mdIndex) {
                foundMd = true;
                currentCheck = true;
            }
            if (t.index === meIndex) {
                foundMs = true;
                currentCheck = true;
            }
            t.check = (foundMd || foundMs) && (!(foundMd && foundMs) || currentCheck);
        });

        timeSlots.filter(t => t.check).forEach(tc => {
            tc.div.classList.add('action-drag-absence');
            timeSlotsFetched.push(tc);
        });
    }

    private static findElement(timeSlots: TimeSlotData[], e) {
        return timeSlots.find(t => t.div === e.currentTarget).index;
    }

    /**
     * Format form object (startDate, endDate, startTime, endTime)
     */
    private static formatForm(timeSlotsFetched: any[]) {
        if (timeSlotsFetched.length === 0) return;
        let start = timeSlotsFetched[0].startDate;
        let end = timeSlotsFetched[timeSlotsFetched.length - 1].endDate;
        let startTime = timeSlotsFetched[0].start;
        let endTime = timeSlotsFetched[timeSlotsFetched.length - 1].end;

        let absence = timeSlotsFetched
            .filter(t => t.index === timeSlotsFetched[0].index)
            .find(t => "type" in t && t.type.event === EventsUtils.ALL_EVENTS.absence);
        let type: { event: string, id: number } = absence !== undefined ? absence.type : {event: "", id: null};

        return {
            absenceId: type.id,
            type: type,
            startDate: moment(start).toDate(),
            endDate: moment(end).toDate(),
            startTime: moment(new Date).set({
                hours: startTime,
                minute: 0,
                second: 0,
                millisecond: 0
            }).toDate(),
            endTime: moment(new Date).set({
                hours: endTime,
                minute: 0,
                second: 0,
                millisecond: 0
            }).toDate()
        }
    }

    /**
     * get timeslots object (div, startDate, start, endDate, end)
     */
    private static getTimeSlotData(index: number, target: Element, _scope: any): TimeSlotData {
        let timeSlot: TimeSlotData = {} as TimeSlotData;
        let timeSlotScope = _scope['timeslot'];
        let dayScope = _scope['day'];

        timeSlot.index = index;
        timeSlot.div = target;
        timeSlot.check = false;
        timeSlot.timeslot = timeSlotScope;
        timeSlot.startDate = moment(dayScope.date).format('YYYY-MM-DD') + ' ' + moment(new Date).set({
            hours: timeSlotScope.start,
            minute: 0,
            second: 0,
            millisecond: 0
        }).format('HH:mm');
        timeSlot.start = timeSlotScope.start;
        timeSlot.endDate = moment(dayScope.date).format('YYYY-MM-DD') + ' ' + moment(new Date).set({
            hours: timeSlotScope.end,
            minute: 0,
            second: 0,
            millisecond: 0
        }).format('HH:mm');
        timeSlot.end = timeSlotScope.end;

        return timeSlot;
    }

    /**
     * get timeslots object (div, startDate, start, endDate, end)
     */
    private static getCourseSlotData(index: number, target: Element, _scope: any): TimeSlotData {
        let timeSlot: TimeSlotData = {} as TimeSlotData;
        let type: { event: string, id: number } = {event: "", id: null};
        if ('absenceId' in _scope.item) {
            type = {
                event: EventsUtils.ALL_EVENTS.absence,
                id: _scope.item.absenceId
            }
        }
        timeSlot.index = index;
        timeSlot.div = target;
        timeSlot.check = false;
        timeSlot.timeslot = {};
        timeSlot.startDate = _scope.item.startDate;
        timeSlot.start = parseInt(_scope.item.startMomentTime.split(':')[0]);
        timeSlot.endDate = _scope.item.endDate;
        timeSlot.end = parseInt(_scope.item.endMomentTime.split(':')[0]);
        timeSlot.type = type;

        return timeSlot;
    }

    /**
     * Remove CSS and clear array
     */
    private static resetTimeSlotsFetched(timeSlots: TimeSlotData[], timeSlotsFetched) {
        timeSlots.forEach(t => {
            t.div.classList.remove('action-drag-absence');
            t.check = false;
        });
        // emptying array
        timeSlotsFetched.length = 0;
    }
}