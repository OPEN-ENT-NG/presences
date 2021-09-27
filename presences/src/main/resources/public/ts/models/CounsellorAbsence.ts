import http from 'axios';
import {model, notify} from 'entcore';

declare let window: any;

export class CounsellorAbsence {
    id: number;
    start_date: string;
    end_date: string;
    student_id: string;
    reason_id: number | null;
    counsellor_regularisation: boolean;
    student: {
        id: string,
        displayName: string,
        className: string,
        name?: string
    };

    async updateReason() {
        try {
            await http.put('/presences/absence/reason', {reasonId: this.reason_id, ids: [this.id]});
        } catch (err) {
            notify.error('presences.absences.update_reason.error');
        }
    }

    async updateAbsence(): Promise<void> {
        try {
            await http.put('/presences/absence/' + this.id,
                {
                    end_date: this.end_date,
                    start_date: this.start_date,
                    owner: model.me.userId,
                    reason_id: this.reason_id,
                    structure_id: window.structure.id,
                    student_id: this.student_id
                });
        } catch (err) {
            notify.error('presences.absences.update.error');
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