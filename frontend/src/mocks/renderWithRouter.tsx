import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { render } from './setup';

export const renderWithRouter = (
  path = '/',
  element: JSX.Element,
  initialEntry = path,
) => {
  const routes = [
    {
      path,
      element,
    },
  ];

  const router = createMemoryRouter(routes, {
    initialEntries: [initialEntry],
  });

  return {
    /**
     * We use our customRender fn to wrap Router with our Providers
     */
    ...render(<RouterProvider router={router} />),
  };
};
