import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '~/mocks/setup';
import { ANGULAR_APP_URL, Registers } from '.';

describe('Registers', () => {
  it('navigates back to the AngularJS application', async () => {
    const assign = vi.fn();
    vi.stubGlobal('location', { ...window.location, assign });

    render(<Registers />);

    expect(
      await screen.findByRole('heading', { name: 'Appels' }),
    ).toBeInTheDocument();

    const button = await screen.findByRole(
      'button',
      { name: /retour/i },
      { timeout: 5000 },
    );
    fireEvent.click(button);

    expect(assign).toHaveBeenCalledWith(ANGULAR_APP_URL);
    vi.unstubAllGlobals();
  });
});
