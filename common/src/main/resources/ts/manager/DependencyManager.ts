import {appPrefix, Behaviours, idiom} from "entcore";

export class DependencyManager {
    private appPrefix: string;
    private mods: string[] = ['presences', 'incidents', 'massmailing'];

    constructor() {
        this.appPrefix = appPrefix;
    }

    async load(): Promise<void[]> {
        const promises: Promise<void>[] = [];
        this.mods.forEach((mod) => {
            if (this.appPrefix !== mod) {
                promises.push(idiom.addBundlePromise(`/${mod}/i18n`));
                promises.push(Behaviours.load(mod));
            }
        });

        return await Promise.all(promises);
    }
}