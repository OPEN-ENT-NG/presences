import { describe, expect, it, vi } from 'vitest';
import { renderWithRouter } from '~/mocks/renderWithRouter';
import { fireEvent, screen } from '~/mocks/setup';
import { ANGULAR_APP_URL, Registers } from '.';

const CONTEXT_QUERY =
  'courseId=course-1&structureId=structure-1&date=2026-10-13&startTime=08:30&endTime=11:45';

describe('Registers', () => {
  it('navigates back to the AngularJS application', async () => {
    const assign = vi.fn();
    vi.stubGlobal('location', { ...window.location, assign });

    renderWithRouter('/registers', <Registers />);

    expect(
      await screen.findByRole('heading', { name: 'presences.register' }),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'presences.back' }));

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
      await screen.findByText('presences.register.context'),
    ).toBeInTheDocument();
  });

  it('warns when the course context is missing', async () => {
    renderWithRouter('/registers', <Registers />);

    expect(
      await screen.findByText('presences.register.context.invalid'),
    ).toBeInTheDocument();
  });
});
