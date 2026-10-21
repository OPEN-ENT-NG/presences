import {
  RouteObject,
  RouterProvider,
  createMemoryRouter,
} from 'react-router-dom';
import { render } from './setup';

/** Renders a route table, to test loaders and redirections with a browser-like history. */
export const renderWithRoutes = (
  routes: RouteObject[],
  initialEntries: string[],
  initialIndex = initialEntries.length - 1,
) => {
  const router = createMemoryRouter(routes, { initialEntries, initialIndex });

  return {
    router,
    ...render(<RouterProvider router={router} />),
  };
};
