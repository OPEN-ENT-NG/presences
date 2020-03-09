import {Course, CourseEvent, Notebook, TimeSlot} from "../../services";
import {angular, moment} from "entcore";
import {DateUtils} from "@common/utils";

declare const window: any;

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
            absenceElement.style.position = 'absolute';
        }
    }

    static positionAbsence(event: CourseEvent, item, slots: Array<TimeSlot>) {
        const {id} = item;
        const courseSlotsIndexes = window.model.calendar.getSlotsIndex(item.startDate, item.endDate);
        const eventSlotsIndexes = window.model.calendar.getSlotsIndex(event.start_date, event.end_date);
        if (courseSlotsIndexes.length === 0 || eventSlotsIndexes.length === 0) return;
        const topDifference = courseSlotsIndexes.indexOf(eventSlotsIndexes[0]);
        if (topDifference === -1) return;
        const topMargin = topDifference * window.entcore.calendar.dayHeight;
        const el = document.getElementById(event.id.toString());
        if (el) {
            el.style.top = `${topMargin}px`;
        }
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
            let courseScope = angular.element(course).scope();

            /* Coloring course in red if inside global absent bloc */
            absenceItems.forEach(absenceItem => {
                let absenceScope = angular.element(absenceItem).scope();
                let isBetweenDate = DateUtils.isBetween(
                    courseScope.item.startDate, courseScope.item.endDate,
                    absenceScope.item.startDate, absenceScope.item.endDate
                );
                let isMatchDate = DateUtils.isMatchDate(
                    courseScope.item.startDate, courseScope.item.endDate,
                    absenceScope.item.startDate, absenceScope.item.endDate
                );
                if (isBetweenDate || isMatchDate) {
                    course.querySelectorAll(".course-item")[0].classList.add("isAbsent");
                }
            });

            /* Coloring course in pink if inside global justified absent bloc */
            absenceReasonItems.forEach(absenceReasonItem => {
                let absenceReasonItemScope = angular.element(absenceReasonItem).scope();
                let isBetweenDate = DateUtils.isBetween(
                    courseScope.item.startDate, courseScope.item.endDate,
                    absenceReasonItemScope.item.startDate, absenceReasonItemScope.item.endDate
                );
                let isMatchDate = DateUtils.isMatchDate(
                    courseScope.item.startDate, courseScope.item.endDate,
                    absenceReasonItemScope.item.startDate, absenceReasonItemScope.item.endDate
                );
                if (isBetweenDate || isMatchDate) {
                    course.querySelectorAll(".course-item")[0].classList.add("isJustifiedAbsent");
                }
            });
        });
    }
}