import {toasts} from "entcore";

export class BlobUtil {

    private fileName: string;
    private file: Blob;
    private fileURL: string;

    constructor(fileName: string, data: any[], blobPropertyBag: BlobPropertyBag) {
        this.fileName = fileName;
        this.file = new Blob(data, blobPropertyBag);
        this.fileURL = window.URL.createObjectURL(this.file);
    }

    public downloadPdf(errorMessage?: string): void {
        if (this.file.size !== 0) {
            const link: HTMLAnchorElement = document.createElement('a');
            link.href = this.fileURL;
            link.download = this.fileName;
            link.click();
            link.remove();
        } else {
            if (errorMessage) {
                toasts.warning(errorMessage);
            }
        }
    }

    public static getFileNameByContentDisposition(content: string): string {
        const regex: RegExp = /filename[^;=\n]*=(UTF-8(['"]*))?(.*)/;
        const matches: RegExpExecArray = regex.exec(content);
        let filename: string;
        if (matches != null && matches[3]) {
            filename = matches[3].replace(/['"]/g, '');
        }
        return decodeURI(filename);
    }
}