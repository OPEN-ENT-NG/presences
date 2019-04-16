import {Mix} from 'entcore-toolkit';
import http from 'axios';

export class Audience {
    name: string;
    id: string;
    type: string;

    constructor(name: string, id?, type?) {
        this.name = name;
        if (id)
            this.id = id;
        else if (type)
            this.type = type;
    }

    toString(): string {
        return this.name;
    }
}

export class Audiences {
    all: Audience[];

    constructor() {
        this.all = [];
    }

    /**
     * Synchronize groups belongs to the parameter structure
     * @param structureId structure id
     * @returns {Promise<void>}
     */
    async sync(structureId: string) {
        try {
            let audiences = await http.get('/viescolaire/classes?idEtablissement=' + structureId);
            this.all = Mix.castArrayAs(Audience, audiences.data);
        } catch (e) {
            throw e;
        }
    }
}

