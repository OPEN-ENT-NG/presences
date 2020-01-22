import http from 'axios';

export interface IPeriod {
    id: number,
    id_etablissement: string,
    timestamp_dt: string,
    timestamp_fn: string,
    date_fin_saisie: string,
    id_classe: string,
    id_type: number,
    date_conseil_classe: string,
    publication_bulletin: string,
    type: number,
    ordre: number,
    label?: string
}

interface IPeriodService {
    get(structure: string, group: string): Promise<Array<IPeriod>>;
}

export const PeriodService: IPeriodService = {
    async get(structure: string, group: string): Promise<Array<IPeriod>> {
        try {
            const {data} = await http.get(`/viescolaire/periodes?idGroupe=${group}&idEtablissement=${structure}`);
            return data as Array<IPeriod>;
        } catch (e) {
            throw e;
        }
    }
};