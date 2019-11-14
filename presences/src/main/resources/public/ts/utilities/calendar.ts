import {Course, Notebook} from "../services";
import {moment} from "entcore";
import {DateUtils} from "@common/utils";

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

    /**
     * Rendering absence's state (red or pink)
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