import type { Plugin } from 'vite';

export type MockModeOptions = {
  /** Entry points that work with the mocked data, the first one is opened. */
  entryPoints: { label: string; path: string }[];
};

/**
 * Dev-only: in mock mode, prints the URLs that work with the mocked data,
 * under the Vite ones. Pair it with `server.open` set to the first entry point.
 */
export function mockMode({ entryPoints }: MockModeOptions): Plugin {
  return {
    name: 'mock-mode',
    apply: 'serve',
    configureServer(server) {
      const printUrls = server.printUrls.bind(server);
      server.printUrls = () => {
        printUrls();
        const origin = server.resolvedUrls?.local[0]?.replace(/\/$/, '') ?? '';
        const lines = entryPoints.map(
          ({ label, path }) => `  ${label}\n    ${origin}${path}`,
        );
        server.config.logger.info(
          `\n  Mock mode (offline), URLs to try:\n\n${lines.join('\n\n')}\n`,
        );
      };
    },
  };
}
