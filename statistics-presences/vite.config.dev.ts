import { defineConfig } from "vite";
import path = require("path");
import { appPrefixRewritePlugin } from "../vite/plugins/appPrefixRewrite";
import { createDevProxyConfig } from "../vite/plugins/devProxy";
import { entcoreGlobalsPlugin } from "../vite/plugins/entcoreGlobals";
import { injectAppPrefixPlugin } from "../vite/plugins/injectAppPrefix";
import { htmlTemplateReloadPlugin } from "../vite/plugins/htmlTemplateReload";

const publicRoot = path.resolve(__dirname, "src/main/resources/public");

const ENTCORE_EXPORTS = [
  "$", "_", "angular", "appPrefix", "Behaviours", "Controller",
  "Document", "idiom", "idiomaslang", "init", "Me",
  "model", "moment", "ng", "notify", "Rights", "routes",
  "Shareable", "template", "toasts", "ui",
];

const UMD_GLOBALS = {
  angular: { source: "window.angular", named: ["extend"] },
  moment: { source: "window.entcore.moment", named: [] },
  jquery: { source: "window.entcore.$", named: [] },
  underscore: { source: "window.entcore._", named: [] },
};

const APP_NAME = "statistics-presences";

export default ({ mode }: { mode: string }) => {
  const { headers, proxy } = createDevProxyConfig({
    mode,
    routes: [
      "/conf/public",
      "^/(?=applications-list)",
      "^/(?=assets)",
      "^/(?=theme|locale|i18n|skin)",
      "^/(?=auth|appregistry|cas|userbook|directory|communication|conversation|portal|session|timeline|workspace|infra)",
      "^/(?=presences|incidents|massmailing|viescolaire)",
      "^/statistics-presences/(?!public/)",
    ],
  });

  return defineConfig({
    root: publicRoot,
    appType: "spa",
    optimizeDeps: {
      exclude: ["entcore", "angular", "moment", "jquery", "underscore"],
    },
    resolve: {
      extensions: [".ts", ".js"],
      alias: {
        "@common": path.resolve(__dirname, "../common/src/main/resources/ts"),
        "@statistics": path.resolve(__dirname, "./src/main/resources/public/ts"),
        "@presences": path.resolve(__dirname, "../presences/src/main/resources/public/ts"),
        "@incidents": path.resolve(__dirname, "../incidents/src/main/resources/public/ts"),
      },
    },
    server: {
      port: 4200,
      host: "localhost",
      headers,
      proxy,
    },
    plugins: [
      entcoreGlobalsPlugin({
        entcoreExports: ENTCORE_EXPORTS,
        umdGlobals: UMD_GLOBALS,
      }),
      injectAppPrefixPlugin({ appName: APP_NAME }),
      appPrefixRewritePlugin({
        appName: APP_NAME,
        stubbedPaths: /^\/(presences|incidents|massmailing|viescolaire|statistics-presences)\/public\/js\/behaviours/,
      }),
      htmlTemplateReloadPlugin()
    ],
  });
};
