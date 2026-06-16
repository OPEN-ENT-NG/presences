import type { Plugin } from "vite";

export type AppPrefixRewritePluginOptions = {
  /**
   * Application name used in URLs (e.g. "calendar" for /calendar/public/...).
   */
  appName: string;
  /**
   * Optional matcher for paths that must be hard-blocked (404) in dev. Used to
   * prevent serving stale build artifacts that can conflict with Vite modules.
   * Defaults to the prod `dist/` output dir.
   */
  blockedPattern?: RegExp;
  /**
   * Optional matcher for the lazily-fetched behaviours bundle. entcore fetches
   * `/<module>/public/js/behaviours.js` via `http().get` at runtime; jQuery
   * auto-evaluates the JS response. In dev there is no built bundle.
   */
  behavioursPattern?: RegExp;
  pluginName?: string;
};

/**
 * Rewrites production-like asset URLs to Vite-local paths during dev, and
 * shims the behaviours bundle so entcore's lazy fetch stays on its success path.
 *
 * URL rewrite example:
 * - incoming: /calendar/public/template/top-menu.html
 * - rewritten: /template/top-menu.html
 *
 * Behaviours shim (critical): entcore lazily fetches
 * `/<module>/public/js/behaviours.js`. Returning 404 is ACTIVELY HARMFUL —
 * entcore's error path sets `applicationsBehaviours[module] = {failed:true}`,
 * which clobbers the registration that the ESM `behaviours.ts` (loaded in
 * `index.html`) already made, so sniplets resolve to `undefined`
 * (`e.sniplets is undefined`). Instead we serve a 200 classic shim
 * (`import("/ts/behaviours.ts");`) that keeps entcore on the success path
 * (it re-reads the registered object and flushes pending callbacks) and
 * re-triggers the ESM registration idempotently.
 *
 * > A single-module app that renders no sniplet on initial load may not hit
 * > this (the race resolves before any lookup), which is why a plain 404-block
 * > can appear to work. Apps with portal sniplets (memento/navigation) fail
 * > without the shim.
 */
export function appPrefixRewritePlugin({
  appName,
  blockedPattern = /^\/dist\//,
  behavioursPattern = /^\/js\/behaviours/,
  pluginName = "app-prefix-rewrite",
}: AppPrefixRewritePluginOptions): Plugin {
  const publicPrefix = `/${appName}/public/`;
  const publicBase = `/${appName}/public`;

  return {
    name: pluginName,
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const request = req as typeof req & { url?: string };
        if (request.url?.startsWith(publicPrefix)) {
          request.url = request.url.slice(publicBase.length);
        }
        // Serve a classic 200 shim for the lazily-fetched behaviours bundle.
        if (request.url && behavioursPattern.test(request.url)) {
          res.statusCode = 200;
          res.setHeader("Content-Type", "application/javascript");
          res.end(`import("/ts/behaviours.ts");`);
          return;
        }
        // Hard-block stale build artifacts so they never mix with Vite output.
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
