export interface TimeSlotData {
    index: number;
    div: Element;
    divTimeSlots?: Element;
    timeslot: any;
    startDate: string;
    start: number;
    startMinutes: number;
    endDate: string;
    end: number;
    endMinutes: number;
    check?: boolean;
    type?: { event: string, id: number };
    isCourse?: boolean;
}