import http from 'axios';
import {ng} from "entcore";

export interface IPeriod {
    id?: number,
    id_etablissement?: string,
    timestamp_dt: string,
    timestamp_fn: string,
    date_fin_saisie?: string,
    id_classe?: string,
    id_type?: number,
    date_conseil_classe?: string,
    publication_bulletin?: string,
    type?: number,
    ordre?: number,
    label?: string
}

export interface IPeriodService {
    get(structure: string, group: string): Promise<Array<IPeriod>>;

    getPeriods(structure: string, group: Array<string>): Promise<Array<IPeriod>>;
}

export const PeriodService: IPeriodService = {
    async get(structure: string, group: string): Promise<Array<IPeriod>> {
        try {
            const {data} = await http.get(`/viescolaire/periodes?idGroupe=${group}&idEtablissement=${structure}`);
            return data as Array<IPeriod>;
        } catch (e) {
            throw e;
        }
    },

    async getPeriods(structure: string, group: Array<string>): Promise<Array<IPeriod>> {
        try {
            let idGroups: string = '';
            if (group) {
                group.forEach((g: string) => {
                    idGroups += `&idGroupe=${g}`;
                });
            }
            const {data} = await http.get(`/viescolaire/periodes?idEtablissement=${structure}${idGroups}`);
            return data as Array<IPeriod>;
        } catch (e) {
            throw e;
        }
    }
};

export const periodeService = ng.service('PeriodService', (): IPeriodService => PeriodService);