import {Notebook} from "../services";

export interface IForgottenNotebookResponse {
    limit: number;
    offset: number;
    all: Array<Notebook>;
    totals: number;
}