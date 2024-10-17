import {idiom} from 'entcore';

export class Toast {
    text: String;
    status: String;

    /**
     *
     * @param {String} text
     * @param {String} status (can be info, confirm)
     */
    constructor(text: String, status: String = 'info') {
        this.text = (idiom.translate(text) as String);
        this.status = status;
    }
}