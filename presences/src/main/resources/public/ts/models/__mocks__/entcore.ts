import {Moment} from "moment";

declare let require: any;

export const moment: Moment = require("moment");

interface IController {
    name: string;
    contents: any;
}
interface IDirective {
    name: string;
    contents: any;
}
interface IService {
    name: string;
    contents: any;
}

const controllers: Array<IController> = [];
const directives: Array<IDirective> = [];
const services: Array<IService> = [];

export const ng = {
    service: jest.fn((name: string, contents: any): void => {
        this.ng.services.push({name, contents});
    }),
    directive: jest.fn((name: string, contents: any): void => {
        this.ng.directives.push({name, contents});
    }),
    controller: jest.fn((name: string, contents: any): void => {
        this.ng.controllers.push({name, contents});
    }),
    // init services, controller and directives
    initMockedModules: jest.fn((app: any): void => {
        this.ng.services.forEach((s) => app.service(s.name, s.contents));
        this.ng.directives.forEach((d) => app.directive(d.name, d.contents));
        this.ng.controllers.forEach((c) => app.controller(c.name, c.contents));
    }),
    controllers: controllers,
    directives: directives,
    services: services,
};

export const model = {
    calendar: {
        dayForWeek: '2017-01-12T14:00:00.000+01:00'
    },
    me: {
        userId: '7b6459f5-2765-45b5-8086-d5b3f422e69e',
        type: 'PERSEDUCNAT',
        hasWorkflow: jest.fn(() => true),
        hasRight: jest.fn(() => true),
        structures: [],
        structureNames: []
    },
};

export const idiom = {
    translate: jest.fn((key : string) => key)
};