import {model} from 'entcore';

export class Structure {
    id: string;
    name: string;

    /**
     * Structure constructor. Can take an id and a name in parameter
     * @param id structure id
     * @param name structure name
     */
    constructor(id?: string, name?: string) {
        if (typeof id === 'string') {
            this.id = id;
        }
        if (typeof name === 'string') {
            this.name = name;
        }
    }
}

export class Structures {
    all: Structure[];

    constructor(arr?: Structure[]) {
        this.all = [];
        if (arr instanceof Structure) {
            this.all = arr;
        }
    }

    sync() {
        for (let i = 0; i < model.me.structures.length; i++) {
            this.all.push(new Structure(model.me.structures[i], model.me.structureNames[i]));
        }
        return;
    }

    /**
     * Returns first structure occurrence in the class
     * @returns {Structure} first structure contained in 'all' array
     */
    first(): Structure {
        return this.all[0];
    }
}