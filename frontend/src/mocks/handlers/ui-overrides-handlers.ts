import { HttpResponse, http } from 'msw';

/**
 * Mock mode only (`browser.ts`), not the tests: overrides the default handlers.
 * The page layout asks for the `rgpdCookies` preference on every mount and
 * shows the cookies banner unless `showInfoTip` is false. Answer as a user who
 * already closed it, like on the recette: closing it is not persisted offline.
 */
export const uiOverridesHandlers = [
  http.get('/userbook/preference/rgpdCookies', () => {
    return HttpResponse.json({ preference: '{"showInfoTip":false}' });
  }),
];
