import {Course, CourseEvent, Notebook, TimeSlot} from "../services";
import {_, angular, moment} from "entcore";
import {DateUtils} from "@common/utils";
import {ABSENCE_FORM_EVENTS} from "../sniplets";

export class CalendarUtils {

    /**
     * Rendering legends html with forgotten notebook if it does match
     */
    static renderLegends(legends: NodeList, notebooks: Notebook[]): void {
        let regex = /[0-3]?[0-9]\/[0-3]?[0-9]/gi; // regex to get value (e.g "05/11")in a string
        this.removeLegendsIcon(legends);
        Array.from(legends).forEach((legend: HTMLElement) => {
            let dayValue = legend.textContent.match(regex)[0]; // => value = 05/11
            notebooks.forEach(notebook => {
                let notebookDate = moment(notebook.date).format(DateUtils.FORMAT["DAY-MONTH"]);
                if (dayValue === notebookDate) {
                    this.assignLegendsIcon(legend, notebook);
                }
            });
        });
    }

    private static assignLegendsIcon(legend: HTMLElement, notebook: Notebook) {
        legend.style.setProperty("background-color", "#b0ead5", "important");
        legend.style.cursor = 'pointer';
        legend.setAttribute('forgotten-id', notebook.id.toString());
        legend.className = "forgotten-notebook-legends";
    }

    private static removeLegendsIcon(legends: NodeList) {
        Array.from(legends).forEach((legend: HTMLElement) => {
            legend.style.removeProperty("background-color");
            legend.style.cursor = 'unset';
            legend.removeAttribute('forgotten-id');
            legend.className = "";
        });
    }

    private static hasHalfTime(event: CourseEvent, slot: TimeSlot): boolean {
        let eventStartTime = DateUtils.format(event.start_date, DateUtils.FORMAT["HOUR-MINUTES"]);
        let eventEndTime = DateUtils.format(event.end_date, DateUtils.FORMAT["HOUR-MINUTES"]);
        let slotStartTime = DateUtils.format(DateUtils.getTimeFormat(slot.startHour), DateUtils.FORMAT["HOUR-MINUTES"]);
        let slotEndTime = DateUtils.format(DateUtils.getTimeFormat(slot.endHour), DateUtils.FORMAT["HOUR-MINUTES"]);
        return eventStartTime > slotStartTime || eventEndTime < slotEndTime;
    }


    private static getMinuteInEventTime(startDate: string, endDate: string): number {
        return Math.abs(DateUtils.format(startDate, DateUtils.FORMAT["HOUR-MINUTES"]).split(":")[1] -
            DateUtils.format(endDate, DateUtils.FORMAT["HOUR-MINUTES"]).split(":")[1]);
    }

    private static eventAbsenceHasSameTime(item: Course, event: CourseEvent): boolean {
        let eventStartTime = DateUtils.format(event.start_date, DateUtils.FORMAT["HOUR-MINUTES"]);
        let eventEndTime = DateUtils.format(event.end_date, DateUtils.FORMAT["HOUR-MINUTES"]);
        return ((eventStartTime === item.startMomentTime) && (eventEndTime === item.endMomentTime));
    }

    /**
     * Rendering event containing absences in course
     */
    static renderAbsenceFromCourse(item: Course, event: CourseEvent, slots: Array<TimeSlot>) {
        let absenceElement = document.getElementById(event.id.toString());
        if (absenceElement && event.reason_id != null) {
            absenceElement.style.backgroundColor = "#ff8a84"; // $presences-pink;
        }
        if (this.eventAbsenceHasSameTime(item, event)) {
            return;
        }
        const daySlotHeight = document.querySelector(".day").clientHeight;
        const oneTimeSlotHeight = daySlotHeight / slots.length;
        let slotsIndexFetched = [];
        slots.forEach(slot => {
            let slotStart = DateUtils.getTimeFormat(slot.startHour);
            let slotEnd = DateUtils.getTimeFormat(slot.endHour);
            if (DateUtils.isBetween(event.start_date, event.end_date, slotStart, slotEnd,
                DateUtils.FORMAT["HOUR-MINUTES"], DateUtils.FORMAT["HOUR-MINUTES"])) {
                slotsIndexFetched.push(slots.findIndex(slotsItem => slotsItem._id === slot._id));
            }
        });
        if (absenceElement) {
            if (this.hasHalfTime(event, slots[slotsIndexFetched[0]])) {
                absenceElement.style.height = `${((this.getMinuteInEventTime(event.start_date, event.end_date)) * oneTimeSlotHeight) / 60}px`;
            } else {
                absenceElement.style.height = `${oneTimeSlotHeight * slotsIndexFetched.length}px`;
            }
            absenceElement.style.position = 'relative';
        }
    }

    public static actionAbsenceTimeSlot($scope) {
        $('.timeslots .timeslot').click(function () {
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
                date: absenceDate,
                startTime: startTime,
                endTime: endTime
            };
            $scope.$broadcast(ABSENCE_FORM_EVENTS.OPEN, form);
        })
    }

    static positionAbsence(event: CourseEvent, item, slots: Array<TimeSlot>) {

        function getSplitCourseEventsIds(itemSplitCourse: Course[]): number[] {
            let splitCourseEventsId = [];
            itemSplitCourse.map(item => {
                if (item.eventId) {
                    splitCourseEventsId.push(item.eventId);
                }
            });
            return splitCourseEventsId
        }

        const daySlotHeight = document.querySelector(".day").clientHeight;
        const oneTimeSlotHeight = daySlotHeight / slots.length;
        const eventStartDate = DateUtils.format(event.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
        const eventEndDate = DateUtils.format(event.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);

        let splitCourseEventsId = getSplitCourseEventsIds(item.splitCourses);

        let eventsPositionFetched = [];
        item.splitCourses.forEach((itemCourse: Course) => {
            const courseStartDate = DateUtils.format(itemCourse.startDate, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
            const courseEndDate = DateUtils.format(itemCourse.endDate, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
            if (eventStartDate === courseStartDate && eventEndDate === courseEndDate) {
                let absenceElement = document.getElementById(event.id.toString());
                if (absenceElement) {

                    let newEventsPosition = JSON.parse(JSON.stringify(eventsPositionFetched));
                    eventsPositionFetched.forEach((eventPosition: { minuteEvent: number, eventId: number }) => {
                        splitCourseEventsId.forEach(splitCourseEventId => {
                            if (splitCourseEventId === eventPosition.eventId) {
                                newEventsPosition = newEventsPosition.filter(item => item.eventId !== 0);
                                let newEventsPositionIndex = newEventsPosition
                                    .findIndex(newEventPosition => newEventPosition.eventId === eventPosition.eventId);
                                if (newEventsPositionIndex !== -1)
                                    newEventsPosition.splice(newEventsPositionIndex, 1);
                            }
                        })
                    });
                    let marginTop = newEventsPosition.length > 0 ?
                        _.pluck(newEventsPosition, 'minuteEvent')
                            .reduce((accumulator, currentValue) => accumulator.minuteEvent + currentValue.minuteEvent) : 0;

                    absenceElement.style.marginTop = `${(marginTop * oneTimeSlotHeight) / 60}px`;
                    return;
                }
            }
            let getMinuteEventTime = this.getMinuteInEventTime(itemCourse.startDate, itemCourse.endDate) === 0 ?
                60 : this.getMinuteInEventTime(itemCourse.startDate, itemCourse.endDate);
            eventsPositionFetched.push({
                minuteEvent: getMinuteEventTime,
                eventId: itemCourse.eventId ? itemCourse.eventId : 0,
            });
        });
    }

    static addEventIdInSplitCourse(item: Course, events: CourseEvent[]) {
        item.splitCourses.forEach((course: Course) => {
            const courseStartDate = DateUtils.format(course.startDate, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
            const courseEndDate = DateUtils.format(course.endDate, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
            events.forEach(event => {
                const eventStartDate = DateUtils.format(event.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
                const eventEndDate = DateUtils.format(event.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
                if (courseStartDate === eventStartDate && courseEndDate === eventEndDate) {
                    course.eventId = event.id;
                }
            })
        })
    }

    /**
     * Rendering absence's state (red or pink)
     * @param {Course} item the current course
     */
    static changeAbsenceView(item: Course) {
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
                if (CalendarUtils.isItemInside(course, absenceItem)) {
                    course.querySelectorAll(".course-item")[0].classList.add("isAbsent");
                }
            });

            /* Coloring course in pink if inside global justified absent bloc */
            absenceReasonItems.forEach(absenceReasonItem => {
                if (CalendarUtils.isItemInside(course, absenceReasonItem)) {
                    course.querySelectorAll(".course-item")[0].classList.add("isJustifiedAbsent");
                }
            });
        });
    }

    /**
     * Return true if bloc is inside div
     */
    static isItemInside(item, itemAbsence): boolean {
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
}