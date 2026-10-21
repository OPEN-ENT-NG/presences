import { LoadingScreen, PageLayout, useEdificeClient } from '@edifice.io/react';
import { Outlet } from 'react-router-dom';

export const loader = async () => {
  return null;
};

/** Shown while the loaders of the first navigation run (e.g. opening a register). */
export const HydrateFallback = () => <LoadingScreen position={false} />;

export const Root = () => {
  const { init } = useEdificeClient();

  if (!init) return <LoadingScreen position={false} />;

  return (
    <PageLayout>
      <PageLayout.Header />
      <Outlet />
    </PageLayout>
  );
};

export default Root;
