export interface TimeSlotData {
    index: number;
    div: Element;
    divTimeSlots?: Element;
    timeslot: any;
    startDate: string;
    start: number;
    endDate: string;
    end: number;
    check?: boolean;
    type?: { event: string, id: number };
    sameId?: number;
}