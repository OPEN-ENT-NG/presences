import {Course, CourseEvent, Notebook} from "../../services";
import {angular, moment} from "entcore";
import {DateUtils} from "@common/utils";
import {Absence, EventType, ITimeSlotWithAbsence} from "../../models";
import {ABSENCE_FORM_EVENTS} from "@common/core/enum/presences-event";

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

    public static getDeltaPX(aDate, bDate, bDuration, totalHeight): number {
        let aTimestamp = DateUtils.getDateFromMoment(aDate).getTime();
        let bTimestamp = DateUtils.getDateFromMoment(bDate).getTime();

        let minutesDiff = Math.abs(Math.floor((((aTimestamp - bTimestamp) / 1000) / 60)));

        return Math.floor((minutesDiff * totalHeight) / bDuration);
    }

    public static absenceEvents(course: Course): CourseEvent[] {
        return course.events.filter((event: CourseEvent) => event.type_id === EventType.ABSENCE);
    }

    public static addEventIdInSplitCourse(item: Course, events: CourseEvent[]) {
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


    /* ##############################
       ## ABSENCE POSITIONING PART ##
       ############################## */

    static initPositionAbsence(absences: Array<Absence>, courses: Array<Course>, structureId: any, timeSlotHeight: any, $scope: any) {
        let $timeSlots = $('.days .timeslot');
        let timeslotsByAbsences: Map<number, Array<ITimeSlotWithAbsence>> = new Map();

        $timeSlots.each((i: number, slot: HTMLElement) => {
            let elemSlot = angular.element(slot).scope();
            let startTime = CalendarUtils.getSlotYMDMomentFormat(elemSlot, elemSlot.timeslot.start, elemSlot.timeslot.startMinutes);
            let endTime = CalendarUtils.getSlotYMDMomentFormat(elemSlot, elemSlot.timeslot.end, elemSlot.timeslot.endMinutes);
            let absence = CalendarUtils.getAbsenceBetweenDates(absences, startTime, endTime);

            if (absence) {
                CalendarUtils.addAbsenceInCourses(absence, courses);
                CalendarUtils.addTimeslotMappedToAbsence(timeslotsByAbsences, absence, slot, structureId);

            } else {
                CalendarUtils.clearSlot(slot);
            }
        });

        timeslotsByAbsences.forEach((timeslotByAbsences: Array<ITimeSlotWithAbsence>) => {
            CalendarUtils.sortTimeSlotsByDate(timeslotByAbsences);
            CalendarUtils.setTimeSlotsStyleByAbsence(timeslotByAbsences, timeSlotHeight, $scope);
        });
    }

    public static getSlotYMDMomentFormat(elemSlot, hour: number, minute: number) {
        return moment(elemSlot.day.date).set({
            'hour': hour,
            'minute': minute
        });
    }

    public static getAbsenceBetweenDates(absences: Array<Absence>, startTime: any, endTime: any) {
        // If an absence include the current timeslot "absence" is set, else it is undefined.
        return absences.find((absence: Absence) => {
            return DateUtils.isBetweenTimeStamp(startTime, endTime, moment(absence.start_date), moment(absence.end_date))
        });
    }

    public static addAbsenceInCourses(absence: Absence, courses: Array<Course>) {
        courses.forEach((course: Course, i: number) => {
            if (DateUtils.isBetweenTimeStamp(moment(absence.start_date), moment(absence.end_date), moment(course.startDate), moment(course.endDate))
                && !course.absences.find((a: Absence) => a.id === absence.id)) {
                course.absences.push(absence);
            }
        });
    }


    public static addTimeslotMappedToAbsence(timeslotsByAbsences: Map<number, Array<ITimeSlotWithAbsence>>, absence: Absence, slot, structureId: string) {
        let elemSlot = angular.element(slot).scope();
        let tsStartTime = CalendarUtils.getSlotYMDMomentFormat(elemSlot, elemSlot.timeslot.start, elemSlot.timeslot.startMinutes);
        let tsEndTime = CalendarUtils.getSlotYMDMomentFormat(elemSlot, elemSlot.timeslot.end, elemSlot.timeslot.endMinutes);

        let tsStartTimestamp = DateUtils.getDateFromMoment(tsStartTime).getTime();
        let tsEndTimestamp = DateUtils.getDateFromMoment(tsEndTime).getTime();

        let startMoment = moment(absence.start_date).format(DateUtils.FORMAT["HOUR-MIN-SEC"]);
        let startMomentTime = startMoment ? startMoment : "00:00:00";
        let endMoment = moment(absence.end_date).format(DateUtils.FORMAT["HOUR-MIN-SEC"]);
        let endMomentTime = endMoment ? endMoment : "23:59:59";


        let absenceItem = {
            is_periodic: false,
            absence: true,
            locked: true,
            absenceId: absence.id,
            absenceReason: absence.reason_id ? absence.reason_id : 0,
            structureId: structureId,
            event: [],
            startDate: absence.start_date,
            startMomentDate: absence.start_date,
            startMomentTime,
            startTimestamp: DateUtils.getDateFromMoment(moment(absence.start_date)).getTime(),
            endDate: absence.end_date,
            endMomentTime,
            endTimestamp: DateUtils.getDateFromMoment(moment(absence.end_date)).getTime()
        };
        let timeslotInfos = {
            absence: absenceItem, // Info utils for edit modal.
            slotElement: slot, // Element dom concerning the slot
            tsStartMoment: tsStartTime, // Start date of the slot (moment typed)
            tsEndMoment: tsEndTime, // End date of the slot (moment typed)
            tsStartTimestamp, // Start date of the slot (thanks timestamp)
            tsEndTimestamp, // End date of the slot (thanks timestamp)
            slotPosition: elemSlot.$index
        };

        // Here we group timeslotsInfos by absences.
        timeslotsByAbsences.has(absence.id) ? timeslotsByAbsences.get(absence.id).push(timeslotInfos) : timeslotsByAbsences.set(absence.id, [timeslotInfos]);
    }

    public static clearSlot(slot: HTMLElement) {
        while (slot.firstChild) slot.removeChild(slot.lastChild);
        slot.setAttribute("style",
            "background-color: #000000; "
        );
    }

    public static sortTimeSlotsByDate(timeslotByAbsences: Array<ITimeSlotWithAbsence>) {
        timeslotByAbsences.sort((tsA: ITimeSlotWithAbsence, tsB: ITimeSlotWithAbsence) => {
            return tsA.tsStartTimestamp - tsB.tsEndTimestamp;
        });
    }

    public static setTimeSlotsStyleByAbsence(timeslotByAbsences: Array<ITimeSlotWithAbsence>, timeSlotHeight: any, $scope) {
        let tsMinutesDurationStart = Math.floor((((timeslotByAbsences[0].tsEndTimestamp - timeslotByAbsences[0].tsStartTimestamp) / 1000) / 60));
        let deltaFirstHeight = CalendarUtils.getDeltaPX(moment(timeslotByAbsences[0].absence.startDate), timeslotByAbsences[0].tsStartMoment, tsMinutesDurationStart, timeSlotHeight);

        let lastAbsenceSlot = timeslotByAbsences[timeslotByAbsences.length - 1];
        let tsMinutesDurationEnd = Math.floor((((lastAbsenceSlot.tsEndTimestamp - lastAbsenceSlot.tsStartTimestamp) / 1000) / 60));
        let deltaLastHeight = CalendarUtils.getDeltaPX(moment(lastAbsenceSlot.absence.endDate), lastAbsenceSlot.tsEndMoment, tsMinutesDurationEnd, timeSlotHeight);

        // Set timeslot style
        timeslotByAbsences.forEach((timeslot: ITimeSlotWithAbsence, i: number) => {
            let height = timeSlotHeight;
            let top = 0;
            let color = timeslot.absence.absenceReason === 0 ? "#e61610" : "#ff8a84";

            let style = "" +
                "background-color: " + color + " !important; " +
                "border-bottom: solid 1px " + color + " !important;";

            if (i === timeslotByAbsences.length - 1 && timeslot.absence.endTimestamp <= timeslot.tsEndTimestamp) {
                height -= deltaLastHeight;
                style += "border-bottom-left-radius: 10px !important; " +
                    "border-bottom-right-radius: 10px !important; ";
            }
            if (i === 0 && timeslot.tsStartTimestamp <= timeslot.absence.startTimestamp) {
                height -= deltaFirstHeight;
                top += deltaFirstHeight;
                style += "border-top-left-radius: 10px !important; " +
                    "border-top-right-radius: 10px !important; ";
            }

            style += "height: " + height + "px; " +
                "margin-top: " + top + "px; ";

            let div = document.createElement("div");

            div.setAttribute("style", style);

            // event listener to open edit modal.
            div.addEventListener("click", () => {
                $scope.$broadcast(
                    timeslot.absence.type_id ? ABSENCE_FORM_EVENTS.EDIT_EVENT : ABSENCE_FORM_EVENTS.EDIT,
                    timeslot.absence);
            });
            while (timeslot.slotElement.firstChild) timeslot.slotElement.removeChild(timeslot.slotElement.lastChild);
            timeslot.slotElement.style["justify-content"] = "unset";
            timeslot.slotElement.appendChild(div);
        });
    }
}