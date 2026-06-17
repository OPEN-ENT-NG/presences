import path = require("path");
import type { Plugin } from "vite";

export type HtmlTemplateReloadPluginOptions = {
  pluginName?: string;
};

/**
 * Vite's default full-reload for .html files sends `path: "/<file>"` and its
 * client only calls pageReload() when that path matches location.pathname
 * (multi-page-app guard, see client.mjs "full-reload" case). AngularJS
 * templateUrl partials (e.g. side-bar.html) are served at an arbitrary path
 * that never matches the app's root page, so that guard silently swallows
 * the reload. Sending path: "*" bypasses the guard and forces the reload.
 */
export function htmlTemplateReloadPlugin({
  pluginName = "html-template-reload",
}: HtmlTemplateReloadPluginOptions = {}): Plugin {
  return {
    name: pluginName,
    configureServer(server) {
      server.watcher.on("change", (file) => {
        if (file.endsWith(".html") && path.basename(file) !== "index.html") {
          server.ws.send({ type: "full-reload", path: "*" });
        }
      });
    },
  };
}
