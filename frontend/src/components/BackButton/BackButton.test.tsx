import { vi } from 'vitest';
import { ANGULAR_APP_URL } from '~/config';
import { renderWithRoutes } from '~/mocks/renderWithRoutes';
import { fireEvent, screen } from '~/mocks/setup';
import { BackButton } from './BackButton';

const routes = [
  { path: '/previous', element: <div>previous page</div> },
  { path: '/page', element: <BackButton /> },
];

describe('BackButton', () => {
  it('goes back in the browser history', async () => {
    vi.spyOn(window.history, 'length', 'get').mockReturnValue(2);
    const { router } = renderWithRoutes(routes, ['/previous', '/page']);

    fireEvent.click(await screen.findByTestId('presences-button-back'));

    expect(await screen.findByText('previous page')).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/previous');
  });

  it('goes to the AngularJS application when there is no history', async () => {
    vi.spyOn(window.history, 'length', 'get').mockReturnValue(1);
    const assign = vi.fn();
    vi.stubGlobal('location', { ...window.location, assign });
    const { router } = renderWithRoutes(routes, ['/previous', '/page']);

    fireEvent.click(await screen.findByTestId('presences-button-back'));

    expect(assign).toHaveBeenCalledWith(ANGULAR_APP_URL);
    expect(router.state.location.pathname).toBe('/page');
    vi.unstubAllGlobals();
  });
});
