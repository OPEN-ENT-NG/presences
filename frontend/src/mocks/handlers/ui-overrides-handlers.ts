import { HttpResponse, http } from 'msw';

/**
 * Mock mode only (`browser.ts`), not the tests: overrides the default handlers.
 * The page layout asks for the `rgpdCookies` preference on every mount and
 * shows the cookies banner unless `showInfoTip` is false. Answer as a user who
 * already closed it, like on the recette: closing it is not persisted offline.
 */
export const uiOverridesHandlers = [
  // The current application, shown by the breadcrumb
  http.get('/applications-list', () => {
    return HttpResponse.json({
      apps: [
        {
          name: 'Presences',
          address: '/presences',
          icon: 'presences-large',
          target: '',
          displayName: 'presences',
          display: true,
          prefix: '/presences',
          casType: null,
          scope: [''],
          isExternal: false,
        },
      ],
    });
  }),

  http.get('/userbook/preference/rgpdCookies', () => {
    return HttpResponse.json({ preference: '{"showInfoTip":false}' });
  }),
];
