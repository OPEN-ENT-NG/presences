export interface Template {
    viewPath: string;
    containers: any;

    getCompletePath(view: string, isPortal?: boolean): string;

    loadPortalTemplates(): void;

    open: (name: any, view?: any) => void;
    contains: (name: any, view: any) => boolean;
    isEmpty: (name: any) => boolean;
    getPath: (view: any) => string;
    close: (name: any) => void;
    watch: (container: any, fn: any) => void;
}