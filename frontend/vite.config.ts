import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';
import { defineConfig } from 'vitest/config';
import { createDevProxyConfig } from './vite/plugins/devProxy.ts';
import { serveLocalI18n } from './vite/plugins/serveLocalI18n.ts';

// https://vitejs.dev/config/
export default ({ mode }: { mode: string }) => {
  const { headers, proxy } = createDevProxyConfig({
    mode,
    routes: [
      '/applications-list',
      '/conf/public',
      '^/(?=help-1d|help-2d)',
      '^/(?=assets)',
      '^/(?=theme|locale|i18n|skin)',
      '^/(?=auth|appregistry|cas|userbook|directory|communication|conversation|portal|session|timeline|workspace|infra|zendeskGuide)',
      '/explorer',
      '/presences',
    ],
  });

  return defineConfig({
    base: mode === 'production' ? '/presences' : '',
    root: import.meta.dirname,
    cacheDir: './node_modules/.vite/presences',

    resolve: {
      tsconfigPaths: true,
      dedupe: [
        'react',
        'react-dom',
        '@edifice.io/react',
        '@edifice.io/client',
        '@tanstack/react-query',
        'react-hook-form',
        'react-i18next',
      ],
      alias: {
        '@images': resolve(
          import.meta.dirname,
          'node_modules/@edifice.io/bootstrap/dist/images',
        ),
      },
    },

    server: {
      fs: {
        /**
         * Allow the server to access the node_modules folder (for the images)
         * This is a solution to allow the server to access the images and fonts of the bootstrap package for 1D theme
         */
        allow: ['../../'],
      },
      proxy,
      port: 4200,
      headers,
      host: 'localhost',
    },

    preview: {
      port: 4300,
      headers,
      host: 'localhost',
    },

    plugins: [
      serveLocalI18n({
        route: '/presences/i18n',
        filePath: resolve(
          import.meta.dirname,
          '../presences/src/main/resources/i18n/fr.json',
        ),
      }),
      // Common translations (default namespace), served from a sibling
      // entcore checkout like timeline; falls back to the proxy otherwise.
      serveLocalI18n({
        route: '/i18n',
        filePath: resolve(
          import.meta.dirname,
          '../../entcore/portal/backend/src/main/resources/i18n/fr.json',
        ),
      }),
      react(),
    ],

    build: {
      outDir: './dist',
      emptyOutDir: true,
      reportCompressedSize: true,
      commonjsOptions: {
        transformMixedEsModules: true,
      },
      // Dossier dédié : ne pas entrer en collision avec les dossiers de
      // l'AngularJS sous public/ (css, dist, img, js, template, ts).
      assetsDir: 'public/app',
      chunkSizeWarningLimit: 4000,
    },

    test: {
      environment: 'jsdom',
      globals: true,
      include: ['src/**/*.test.{ts,tsx}'],
      setupFiles: ['./src/mocks/setup.ts'],
      watch: false,
      clearMocks: true,
      restoreMocks: true,
      reporters: ['default'],
      coverage: {
        reportsDirectory: './coverage/presences',
        provider: 'v8',
      },
      server: {
        deps: {
          inline: ['@edifice.io/react'],
        },
      },
    },
  });
};
