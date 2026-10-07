import { QueryClient } from '@tanstack/react-query';
import { RouteObject, createBrowserRouter, redirect } from 'react-router-dom';

import { NotFound } from './errors/not-found';
import { PageError } from './errors/page-error';

const routes = (_queryClient: QueryClient): RouteObject[] => [
  /* Main route */
  {
    path: '/',
    async lazy() {
      const { loader, Root: Component } = await import('~/routes/root');
      return {
        loader,
        Component,
      };
    },
    errorElement: <PageError />,
    children: [
      {
        index: true,
        loader: () => redirect('/registers'),
      },
      /* Prise d'appel */
      {
        path: 'registers',
        async lazy() {
          const { Registers: Component } = await import('~/routes/registers');
          return { Component };
        },
      },
    ],
  },
  /* 404 Page */
  {
    path: '*',
    element: <NotFound />,
  },
];

/**
 * Préfixe dédié à l'application React : les routes de l'API Présences sont à
 * plat sous `/presences/*` (ex. `GET /presences/registers/:id`), le front ne
 * doit pas entrer en collision avec elles.
 */
export const basename = import.meta.env.PROD ? '/presences/app' : '/';

export const router = (queryClient: QueryClient) =>
  createBrowserRouter(routes(queryClient), {
    basename,
  });
