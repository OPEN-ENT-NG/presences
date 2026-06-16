import type { Plugin } from "vite";

export type UmdGlobalsMap = Record<string, { source: string; named: string[] }>;

export type EntcoreGlobalsPluginOptions = {
  /**
   * Named runtime exports expected from `import { ... } from "entcore"`.
   * Example: ["template", "notify", "routes"].
   */
  entcoreExports: string[];
  /**
   * External package name -> browser global mapping used in dev.
   * `source` is the JS expression to evaluate and `named` is the subset of
   * value imports that must be re-exported as named exports.
   */
  umdGlobals: UmdGlobalsMap;
  pluginName?: string;
};

/**
 * Prevents Vite from resolving entcore/UMD deps to node_modules packages.
 *
 * Why: in Entcore-like apps, global scripts (e.g. `ng-app.js`) already expose
 * `window.entcore`, `window.angular`, etc. If Vite bundles those deps again,
 * it may load duplicate runtimes (notably AngularJS) and break the app.
 *
 * What this plugin does:
 * - `import ... from "entcore"` -> virtual module backed by `window.entcore`
 * - `import ... from "entcore/*"` -> empty module for deep type-only imports
 * - `import ... from "<umdName>"` -> virtual module backed by configured global
 *
 * This plugin is framework-agnostic and can be reused in any module where the
 * globals are installed before application code executes.
 */
export function entcoreGlobalsPlugin({
  entcoreExports,
  umdGlobals,
  pluginName = "entcore-globals",
}: EntcoreGlobalsPluginOptions): Plugin {
  return {
    name: pluginName,
    enforce: "pre",
    resolveId(id) {
      if (id === "entcore" || id.startsWith("entcore/")) return `\0${id}`;
      if (id in umdGlobals) return `\0umd:${id}`;
    },
    load(id) {
      if (id === "\0entcore") {
        const named = entcoreExports
          .map((name) => `export const ${name} = _e.${name};`)
          .join("\n");
        return `const _e = window.entcore;\n${named}\nexport default _e;`;
      }
      if (id.startsWith("\0entcore/")) {
        return "export default {};";
      }
      if (id.startsWith("\0umd:")) {
        const name = id.slice("\0umd:".length);
        const global = umdGlobals[name];
        if (!global) return null;
        const exports = global.named
          .map((n) => `export const ${n} = _g.${n};`)
          .join("\n");
        return `const _g = ${global.source};\n${exports}\nexport default _g;`;
      }
    },
  };
}
