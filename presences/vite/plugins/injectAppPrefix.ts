import type { Plugin } from "vite";

export type InjectAppPrefixPluginOptions = {
  /**
   * Application name used in URLs (e.g. "calendar" for /calendar/...).
   */
  appName: string;
  pluginName?: string;
};

/**
 * Injects `window.appPrefix` in `index.html` before app scripts execute.
 *
 * Why: some legacy bootstraps compute app prefix from `location.pathname`.
 * On Vite dev root (`/`), this may become empty and produce broken API/template
 * URLs. Setting `window.appPrefix` early keeps routing/path logic aligned with
 * production-style URLs (e.g. /calendar/...).
 *
 * Reuse by passing the module name with `appName`.
 */
export function injectAppPrefixPlugin({
  appName,
  pluginName = "inject-app-prefix",
}: InjectAppPrefixPluginOptions): Plugin {
  return {
    name: pluginName,
    transformIndexHtml: {
      order: "pre",
      handler: () => [
        {
          tag: "script",
          injectTo: "head-prepend",
          children: `window.appPrefix = "${appName}";`,
        },
      ],
    },
  };
}
