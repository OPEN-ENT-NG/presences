import { LoadingScreen, PageLayout, useEdificeClient } from '@edifice.io/react';
import { Outlet } from 'react-router-dom';

export const loader = async () => {
  return null;
};

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
