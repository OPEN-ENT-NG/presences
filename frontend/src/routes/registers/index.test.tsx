import { describe, expect, it, vi } from 'vitest';
import { renderWithRouter } from '~/mocks/renderWithRouter';
import { fireEvent, screen } from '~/mocks/setup';
import { ANGULAR_APP_URL, Registers } from '.';

// Translations are loaded asynchronously (mocked /presences/i18n): leave them time to resolve.
const I18N_TIMEOUT = { timeout: 5000 };

const CONTEXT_QUERY =
  'courseId=course-1&structureId=structure-1&date=2026-10-13&startTime=08:30&endTime=11:45';

describe('Registers', () => {
  it('navigates back to the AngularJS application', async () => {
    const assign = vi.fn();
    vi.stubGlobal('location', { ...window.location, assign });

    renderWithRouter('/registers', <Registers />);

    expect(
      await screen.findByRole('heading', { name: 'Appels' }, I18N_TIMEOUT),
    ).toBeInTheDocument();

    const button = await screen.findByRole(
      'button',
      { name: /retour/i },
      I18N_TIMEOUT,
    );
    fireEvent.click(button);

    expect(assign).toHaveBeenCalledWith(ANGULAR_APP_URL);
    vi.unstubAllGlobals();
  });

  it('displays the course context read from the URL', async () => {
    renderWithRouter(
      '/registers',
      <Registers />,
      `/registers?${CONTEXT_QUERY}`,
    );

    expect(
      await screen.findByText(
        'Cours du 2026-10-13 de 08:30 à 11:45',
        {},
        I18N_TIMEOUT,
      ),
    ).toBeInTheDocument();
  });

  it('warns when the course context is missing', async () => {
    renderWithRouter('/registers', <Registers />);

    expect(
      await screen.findByText(
        "Le cours de la prise d'appel n'a pas pu être identifié.",
        {},
        I18N_TIMEOUT,
      ),
    ).toBeInTheDocument();
  });
});
