import {_, notify} from 'entcore';
import http from 'axios';
import {IStructure} from "@common/model/Viescolaire";

export class Student {
    id: string;
    firstName: string;
    lastName: string;
    name?: string;
    idClasse: string;
    classId?: string;
    displayName: string;
    classeName?: any;
    className?: string;
    classesNames?: Array<string>;
    classesIds?: Array<string>;
    birth?: string;
    structures?: Array<IStructure>;
    structure?: IStructure;

    constructor(o?: any) {
        if (o && typeof o === 'object') {
            for (let key in o) {
                if (key == 'idEleve') {
                    this.id = o['idEleve'];
                } else if (key == 'idClasse') {
                    if (o['idClasse'] && o['idClasse'].length !== 0) {
                        let idClass = [o['idClasse'][0]];
                        this.classeName = idClass.map(id => id.split('$')[1]);
                    } else {
                        this.classeName = [];
                    }
                } else {
                    this[key] = o[key];
                }
            }
        }
    }

    toString() {
        return this.hasOwnProperty("displayName") ? this.displayName + ' - ' + this.classeName :
            this.firstName + " " + this.lastName + ' - ' + this.classeName;
    }
}

export class Students {
    all: Student[];
    searchValue: string;

    constructor() {
        this.all = null;
        this.searchValue = null;
    }

    /**
     * Return student ids array of this.all
     */
    getIds() {
        return _.pluck(this.all, "studentId");
    }

    /**
     * Synchronize student provides by the structure
     * @param structureId structure id
     * @returns {Promise<void>}
     */
    async sync(structureId: string): Promise<void> {
        if (typeof structureId !== 'string') {
            return;
        }
        try {
            let url = `/viescolaire/classe/eleves?idEtablissement=${structureId}`;
            const {data} = await http.get(url);
            this.all = data.map((item) => {
                return new Student(item);
            });
            return;
        } catch (e) {
            notify.error('app.notify.e500');
        }
    }

    async search(structureId: String, text: String) {
        try {
            if ((text.trim() === '' || !text)) return;
            text = text.replace("\\s", "").toLowerCase();
            const {data} = await http.get(`/viescolaire/user/search?structureId=${structureId}&profile=Student&q=${text}&field=lastName&field=firstName&field=displayName`);
            this.all = data.map((item) => {
                return new Student(item);
            });
        } catch (err) {
            notify.error('presences.students.search.err');
            throw err;
        }
    }
}