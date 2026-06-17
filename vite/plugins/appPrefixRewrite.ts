import type { Plugin } from "vite";

export type AppPrefixRewritePluginOptions = {
  /**
   * Application name used in URLs (e.g. "presences" for /presences/public/...).
   */
  appName: string;
  /**
   * Optional matcher for paths that must be blocked in dev. Useful to prevent
   * serving stale build artifacts that can conflict with Vite modules.
   */
  blockedPattern?: RegExp;
  /**
   * Optional matcher for paths that must be stubbed with an empty JS response
   * instead of being blocked (404) or proxied. Useful for cross-module
   * behaviours.js files that would otherwise load duplicate AngularJS copies.
   *
   * Example: /^\/(presences|incidents|massmailing)\/public\/js\/behaviours/
   */
  stubbedPaths?: RegExp;
  pluginName?: string;
};

/**
 * Rewrites production-like asset URLs to Vite-local paths during dev.
 *
 * Example:
 * - incoming: /presences/public/template/top-menu.html
 * - rewritten: /template/top-menu.html
 *
 * Also blocks paths matched by `blockedPattern` (default: dist/ and
 * js/behaviours*) to avoid mixing old generated files with Vite-transformed
 * sources.
 *
 * When `stubbedPaths` is set, matched paths are served as empty JS (200)
 * instead of being blocked. Evaluated before the prefix rewrite.
 */
export function appPrefixRewritePlugin({
  appName,
  blockedPattern = /^\/(dist\/|js\/behaviours)/,
  stubbedPaths,
  pluginName = "app-prefix-rewrite",
}: AppPrefixRewritePluginOptions): Plugin {
  const publicPrefix = `/${appName}/public/`;
  const publicBase = `/${appName}/public`;

  return {
    name: pluginName,
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const request = req as typeof req & { url?: string };
        if (stubbedPaths && request.url && stubbedPaths.test(request.url)) {
          res.setHeader("Content-Type", "application/javascript");
          res.statusCode = 200;
          res.end("/* dev: behaviours stub */");
          return;
        }
        if (request.url?.startsWith(publicPrefix)) {
          request.url = request.url.slice(publicBase.length);
        }
        if (request.url && blockedPattern.test(request.url)) {
          res.statusCode = 404;
          res.end();
          return;
        }
        next();
      });
    },
  };
}
