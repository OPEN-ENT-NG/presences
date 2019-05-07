import {LoadingCollection} from '../LoadingCollection'

describe('LoadingCollection', () => {
    const collection = new LoadingCollection();

    test('It should init loading to false', () => {
        expect(collection._loading).toBeFalsy();
        expect(collection.loading).toBeFalsy();
    });

    test(`It should trigger 'loading::true' event on switching loading to true`, () => {
        const loadingState = true;
        const triggerCallback = jest.fn();
        collection.eventer.on(`loading::${loadingState}`, triggerCallback);
        collection.loading = true;
        expect(triggerCallback).toHaveBeenCalled();
    });
});