import {Indicator} from "./Indicator";

export class IndicatorFactory {

    static create(name: string, ...args: any[]): Indicator {
        const instance: Indicator = Object.create(window[name].prototype);
        instance.constructor.apply(instance, args);
        return instance;
    }

    static register(clazz: Function) {
        window[clazz.name] = clazz;
    }
}