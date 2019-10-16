export interface Idiom {
    translate: (key: any) => any;
    addBundle: (path: any, callback?: any) => void;
    addTranslations: (folder: any, callback?: any) => void;
    addKeys: (keys: any) => void;
    removeAccents: (str: any) => any;
    addDirectives: (module: any) => void;
    addBundlePromise: (path: string) => Promise<void>;
}

