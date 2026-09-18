export interface MockHttpResponseOptions {
    status?: number;
    url?: string;
    method?: string;
    data?: any;
}

/**
 * Builds a fake HttpResponse (entcore-toolkit's `http` bridge shape), for use with
 * `(http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(...))` once `entcore-toolkit`
 * is jest-mocked. Replaces the pre-migration axios-mock-adapter pattern: mocking the app's own
 * `axios` import never intercepted requests made through the bridge (entcore-toolkit bundles its
 * own internal axios instance, a separate object graph), so URL/method mocking must happen at the
 * `http` module boundary instead.
 */
export function mockHttpResponse<T>(data: T, opts: MockHttpResponseOptions = {}): any {
    const {status = 200, url = '', method = 'get', data: requestData} = opts;
    return {
        data,
        status,
        statusText: 'OK',
        headers: {},
        config: {
            url,
            method,
            data: requestData,
        },
    };
}
