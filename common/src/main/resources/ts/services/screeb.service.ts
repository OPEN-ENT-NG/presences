import {appPrefix, model, ng, Service} from 'entcore';
import http from 'axios';
import {
    eventTrack,
    identityProperties,
    identityReset,
    init,
    load,
    messageStart,
    PropertyRecord,
    surveyClose,
    surveyStart,
    targetingDebug,
} from '@screeb/sdk-browser';

const SESSION_POLL_MS: number = 50;
const SESSION_TIMEOUT_MS: number = 15000;

// entcore populates `model.me` asynchronously (~300ms after the page loads): at
// app.ts evaluation time it is still undefined. We don't use `Me.onSessionReady()` or
// `model.one('userinfo-loaded')` to wait for it: `unbind` identifies handlers by comparing
// their `toString()`, but all wrappers created by `one()` share the same source. The first
// handler that fires therefore unregisters another handler from the array `trigger` is
// currently iterating, silently skipping part of it (reproduced here: 3 handlers
// registered, only 1 executed). Hence the polling wait, which is immune to this event bus.
function whenSessionReady(): Promise<void> {
    const start: number = Date.now();
    return new Promise<void>((resolve, reject) => {
        const check = (): void => {
            if (model.me && model.me.userId) {
                resolve();
            } else if (Date.now() - start > SESSION_TIMEOUT_MS) {
                reject(new Error('entcore session unavailable after ' + SESSION_TIMEOUT_MS + 'ms'));
            } else {
                setTimeout(check, SESSION_POLL_MS);
            }
        };
        check();
    });
}

// Privacy: the userId sent to Screeb is SHA-256 hashed and truncated
// to 16 hex characters (rule shared by all Edifice integrations).
async function hashUserId(userId: string): Promise<string> {
    const hashBuffer = await crypto.subtle.digest(
        'SHA-256',
        new TextEncoder().encode(userId),
    );
    return Array.from(new Uint8Array(hashBuffer))
        .map((b: number) => ('0' + b.toString(16)).slice(-2))
        .join('')
        .slice(0, 16);
}

export interface IScreebService {
    initFromPublicConf(): Promise<void>;
    trackEvent(name: string, properties?: PropertyRecord): void;
    triggerSurvey(surveyId: string, hooks?: any, hiddenFields?: PropertyRecord): void;
    triggerMessage(messageId: string, hooks?: any): void;
    closeSurvey(): void;
    debugTargeting(): void;
    setIdentityProperties(properties: PropertyRecord): void;
    reset(): void;
}

export const screebService: IScreebService = {
    // Screeb is opt-in per platform: without a screeb-app-id in the module's
    // publicConf, nothing is loaded (no network call to Screeb).
    initFromPublicConf: async (): Promise<void> => {
        await whenSessionReady();
        const res = await http.get(`/${appPrefix}/conf/public`);
        const appId: string = res.data && res.data['screeb-app-id'];
        if (!appId) {
            return;
        }
        await load();
        const hashedId: string = await hashUserId(model.me.userId);
        await init(appId, hashedId, {profile: model.me.type});
    },

    trackEvent: (name: string, properties?: PropertyRecord): void => {
        eventTrack(name, properties);
    },

    triggerSurvey: (surveyId: string, hooks?: any, hiddenFields?: PropertyRecord): void => {
        surveyStart(surveyId, undefined, undefined, hiddenFields, hooks);
    },

    triggerMessage: (messageId: string, hooks?: any): void => {
        messageStart(messageId, undefined, undefined, hooks);
    },

    closeSurvey: (): void => {
        surveyClose();
    },

    debugTargeting: (): void => {
        targetingDebug();
    },

    setIdentityProperties: (properties: PropertyRecord): void => {
        identityProperties(properties);
    },

    reset: (): void => {
        identityReset();
    },
};

export const ScreebService: Service = ng.service('ScreebService', (): IScreebService => screebService);
