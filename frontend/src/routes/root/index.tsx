import { Layout, LoadingScreen, useEdificeClient } from '@edifice.io/react';
import { Outlet } from 'react-router-dom';

export const loader = async () => {
  return null;
};

export const Root = () => {
  const { init } = useEdificeClient();

  if (!init) return <LoadingScreen position={false} />;

  return (
    <Layout>
      <Outlet />
    </Layout>
  );
};

export default Root;
