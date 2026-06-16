import path = require("path");
import { defineConfig, type UserConfig } from "vite";
import { createDevProxyConfig } from "./vite/plugins/devProxy";
import { entcoreGlobalsPlugin, type UmdGlobalsMap } from "./vite/plugins/entcoreGlobals";
import { appPrefixRewritePlugin } from "./vite/plugins/appPrefixRewrite";
import { injectAppPrefixPlugin } from "./vite/plugins/injectAppPrefix";

const APP_NAME = "presences";

// Named *value* exports pulled from `import { ... } from "entcore"` across every
// module the dev server bundles (presences + @common/@incidents/@massmailing/
// @statistics). Type-only imports are elided by esbuild and omitted here.
const ENTCORE_EXPORTS = [
  "_",
  "$",
  "angular",
  "appPrefix",
  "Behaviours",
  "Controller",
  "idiom",
  "init",
  "Me",
  "model",
  "moment",
  "ng",
  "notify",
  "routes",
  "template",
  "toasts",
  "ui",
];

// UMD packages externalized in prod -> browser globals installed by ng-app.js.
// `named` arrays are empty: no module imports these by bare package name (all go
// through `entcore`), but the mapping keeps Vite from resolving them to a second
// copy in node_modules if any import appears.
const UMD_GLOBALS: UmdGlobalsMap = {
  angular: { source: "window.angular", named: [] },
  moment: { source: "window.entcore.moment", named: [] },
  jquery: { source: "window.entcore.$", named: [] },
  underscore: { source: "window.entcore._", named: [] },
};

export default defineConfig(({ mode }) => {
  const { headers, proxy } = createDevProxyConfig({
    mode,
    routes: [
      "/conf/public",
      "^/(?=applications-list)",
      "^/(?=assets)",
      "^/(?=theme|locale|i18n|skin)",
      "^/(?=auth|appregistry|cas|userbook|directory|communication|conversation|portal|session|timeline|workspace|infra)",
      `^/${APP_NAME}/(?!public/)`,
      // presences makes runtime HTTP calls to the viescolaire backend (memento sniplet, etc.)
      "^/(?=viescolaire)",
    ],
  });

  return {
    root: path.resolve(__dirname, "./src/main/resources/public/"),
    plugins: [
      entcoreGlobalsPlugin({ entcoreExports: ENTCORE_EXPORTS, umdGlobals: UMD_GLOBALS }),
      appPrefixRewritePlugin({ appName: APP_NAME }),
      injectAppPrefixPlugin({ appName: APP_NAME }),
    ],
    server: {
      port: 4200,
      headers,
      proxy,
    },
    // vite.config.dev.ts is standalone (does NOT merge vite.config.shared.ts), so
    // path aliases must be copied verbatim from the shared config, else the dev
    // server fails with "dependencies could not be resolved".
    resolve: {
      extensions: [".ts", ".js"],
      alias: {
        "@common": path.resolve(__dirname, "../common/src/main/resources/ts"),
        "@presences": path.resolve(__dirname, "./src/main/resources/public/ts"),
        "@incidents": path.resolve(__dirname, "../incidents/src/main/resources/public/ts"),
        "@massmailing": path.resolve(__dirname, "../massmailing/src/main/resources/public/ts"),
        "@statistics": path.resolve(__dirname, "../statistics-presences/src/main/resources/public/ts"),
      },
    },
    // Only `entcore` is excluded: it is the window-global shim provided by the
    // entcoreGlobals plugin and must NOT be pre-bundled. Real npm CJS deps such
    // as `entcore-toolkit` MUST be pre-bundled so esbuild can synthesize their
    // named ESM exports — excluding them serves raw CJS and breaks named imports
    // (`doesn't provide an export named 'Eventer'`), which aborts app.ts before
    // it registers controllers (=> "MainController is not registered").
    optimizeDeps: {
      exclude: ["entcore"],
    },
  } satisfies UserConfig;
});
