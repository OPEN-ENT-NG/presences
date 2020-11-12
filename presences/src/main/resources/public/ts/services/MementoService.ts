import http from 'axios';

export interface IPeriodSummary {
    absence_rate: number,
    months: Array<IEventSummary>;
}

export interface IEventSummary {
    month: number,
    types: {
        UNREGULARIZED?: number,
        REGULARIZED?: number,
        NO_REASON?: number,
        LATENESS?: number,
        DEPARTURE?: number
    }
}

export interface IMementoService {
    /**
     * Retrieve event summary based on given data
     *
     * @param student student identifier
     * @param structure structure identifier
     * @param start start date
     * @param end end date
     * @param types type list
     */
    getStudentEventsSummary(student: string, structure: string, start: string, end: string, types: Array<String>): Promise<IPeriodSummary>;
}

export const MementoService: IMementoService = {
    async getStudentEventsSummary(student: string, structure: string, start: string, end: string, types: Array<String>): Promise<IPeriodSummary> {
        try {
            if (types.length === 0) return {absence_rate: 0, months: []};
            let typeFilter = '';
            types.forEach(type => typeFilter += `&type=${type}`)
            const {data} = await http.get(`/presences/memento/students/${student}/absences/summary?structure=${structure}&start=${start}&end=${end}${typeFilter}`);
            return data as IPeriodSummary;
        } catch (e) {
            throw e;
        }
    }

}