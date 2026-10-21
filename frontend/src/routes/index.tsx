import { QueryClient } from '@tanstack/react-query';
import { RouteObject, createBrowserRouter, redirect } from 'react-router-dom';

import { NotFound } from './errors/not-found';
import { PageError } from './errors/page-error';

export const routes = (queryClient: QueryClient): RouteObject[] => [
  /* Main route */
  {
    path: '/',
    async lazy() {
      const {
        loader,
        Root: Component,
        HydrateFallback,
      } = await import('~/routes/root');
      return {
        loader,
        Component,
        HydrateFallback,
      };
    },
    errorElement: <PageError />,
    children: [
      {
        index: true,
        loader: () => redirect('/registers'),
      },
      /* Register entry point: opens the course given by the AngularJS page */
      {
        path: 'registers',
        async lazy() {
          const { loader } = await import('~/routes/registers');
          return { loader };
        },
      },
      /* Opened register */
      {
        path: 'registers/:registerId',
        async lazy() {
          const { loader, Register: Component } =
            await import('~/routes/register');
          return { loader: loader(queryClient), Component };
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
 * Dedicated prefix for the React application: the Présences API routes are
 * flat under `/presences/*` (e.g. `GET /presences/registers/:id`), the front
 * must not collide with them.
 */
export const basename = import.meta.env.PROD ? '/presences/app' : '/';

export const router = (queryClient: QueryClient) =>
  createBrowserRouter(routes(queryClient), {
    basename,
  });
