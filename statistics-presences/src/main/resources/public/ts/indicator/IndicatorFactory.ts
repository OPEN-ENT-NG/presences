import {Indicator} from "./Indicator";

export class IndicatorFactory {

    static create(name: string, ...args: any[]): Indicator {
        // Reflect.construct : compatible avec les classes ES2015 natives servies par Vite,
        // que l'on ne peut pas instancier via constructor.apply() (ancien pattern ES5/webpack).
        return Reflect.construct(window[name], args) as Indicator;
    }

    static register(clazz: Function) {
        window[clazz.name] = clazz;
    }
}