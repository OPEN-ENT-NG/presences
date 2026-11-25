import {
  AppHeader,
  Breadcrumb,
  Layout,
  LoadingScreen,
  useEdificeClient,
} from '@edifice.io/react';
import { Outlet } from 'react-router-dom';

export const Root = () => {
  const { init, currentApp } = useEdificeClient();

  if (!init) return <LoadingScreen position={false} />;

  return (
    <Layout>
      {currentApp && (
        <AppHeader>
          <Breadcrumb app={currentApp} />
        </AppHeader>
      )}
      <Outlet />
    </Layout>
  );
};

export default Root;
