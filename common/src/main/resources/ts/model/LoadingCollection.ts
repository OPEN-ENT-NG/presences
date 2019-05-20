import {Eventer} from 'entcore-toolkit'

export interface LoadingCollection {
    eventer: Eventer;
    _loading: boolean;
    _page: number;

    syncPagination(): Promise<any>;
}

export class LoadingCollection {
    constructor() {
        this.eventer = new Eventer();
        this._loading = false;
        this._page = 0;
    }

    set loading(state: boolean) {
        this._loading = state;
        this.eventer.trigger(`loading::${state}`);
    }

    get loading() {
        return this._loading;
    }

    set page(page: number) {
        this._page = page;
        this.syncPagination();
    }

    get page() {
        return this._page;
    }
}