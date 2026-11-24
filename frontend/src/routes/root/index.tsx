import {
  AppHeader,
  Breadcrumb,
  Flex,
  LoadingScreen,
  PageLayout,
  useEdificeClient,
} from '@edifice.io/react';
import { Outlet } from 'react-router-dom';

export const loader = async () => {
  return null;
};

/** Shown while the loaders of the first navigation run (e.g. opening a register). */
export const HydrateFallback = () => <LoadingScreen position={false} />;

export const Root = () => {
  const { init, currentApp } = useEdificeClient();

  if (!init) return <LoadingScreen position={false} />;

  return (
    <Flex as={PageLayout} direction="column">
      <PageLayout.Header />
      {currentApp && (
        <PageLayout.Breadcrumb>
          <AppHeader>
            <Breadcrumb app={currentApp} />
          </AppHeader>
        </PageLayout.Breadcrumb>
      )}
      <Flex as={PageLayout.Content} fill direction="column">
        <Outlet />
      </Flex>
    </Flex>
  );
};

export default Root;
