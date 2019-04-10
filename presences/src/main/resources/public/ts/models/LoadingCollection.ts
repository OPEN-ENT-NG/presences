import {Eventer} from 'entcore-toolkit'

export interface LoadingCollection {
    eventer: Eventer;
    _loading: boolean;
}

export class LoadingCollection {
    constructor() {
        this.eventer = new Eventer();
        this._loading = false;
    }

    set loading(state: boolean) {
        this._loading = state;
        this.eventer.trigger(`loading::${state}`);
    }

    get loading() {
        return this._loading;
    }
}