import http from 'axios';
import {model, notify, toasts} from 'entcore';

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

    async updateRegularisation() : Promise<boolean> {
        try {
            await http.put('/presences/absence/regularized', {
                regularized: this.counsellor_regularisation,
                ids: [this.id]
            })
            toasts.confirm('presences.absences.update_regularized');
            return true;
        } catch (err) {
            toasts.warning('presences.absences.update_regularized.error');
            this.counsellor_regularisation = !this.counsellor_regularisation;
            return false;
        }
    }

}