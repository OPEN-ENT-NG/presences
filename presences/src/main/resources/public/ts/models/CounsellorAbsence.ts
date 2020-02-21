import http from 'axios';
import {notify} from "entcore";

export class CounsellorAbsence {
    id: number;
    start_date: string;
    end_date: string;
    student_id: string;
    reason_id: number | null;
    counsellor_regularisation: boolean;
    student: {
        id: string,
        displayName: string
    };

    async updateReason() {
        try {
            await http.put('/presences/absence/reason', {reasonId: this.reason_id, ids: [this.id]});
        } catch (err) {
            notify.error('presences.absences.update_reason.error');
        }
    }

    async updateRegularisation() {
        try {
            await http.put('/presences/absence/regularized', {
                regularized: this.counsellor_regularisation,
                ids: [this.id]
            });
        } catch (err) {
            notify.error('presences.absences.update_regularized.error');
        }
    }

}