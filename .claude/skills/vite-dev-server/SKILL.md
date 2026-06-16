# Vite Dev Server — Entcore AngularJS Module

Add hot-reload dev server to legacy Entcore AngularJS module that already has Vite prod builds (`vite.config.application.ts` / `vite.config.behaviours.ts`).

## Context

Apps run in Entcore platform. At runtime `ng-app.js` installs global objects (`window.entcore`, `window.angular`, …). Vite must NOT re-bundle those — loads duplicate runtimes, breaks AngularJS.

Dev server proxies API calls to staging using session tokens from local `.env`.

---

## Files to create

### `vite/plugins/` (copy verbatim — reusable across all modules)

| File | Purpose |
|---|---|
| `devProxy.ts` | Builds proxy config from `.env` tokens |
| `entcoreGlobals.ts` | Virtual modules for `entcore`/UMD globals |
| `appPrefixRewrite.ts` | Rewrites `/<module>/public/…` → `/…`, shims behaviours bundle |
| `injectAppPrefix.ts` | Injects `window.appPrefix` in `index.html` |

Four files identical across modules — module-agnostic, parameterized via plugin
options. Copy verbatim from this skill's [`plugins/`](./plugins/) directory into
the module's `vite/plugins/` (repo root for single-module apps, `<module>/vite/plugins/`
for monorepos). No edits needed; all per-module values pass through `vite.config.dev.ts`.

### `vite.config.dev.ts` (app-specific)

Customize per module:

```ts
const ENTCORE_EXPORTS = [/* named exports used by this module */];

const UMD_GLOBALS = {
  angular:    { source: "window.angular", named: ["extend"] },
  moment:     { source: "window.entcore.moment", named: [] },
  jquery:     { source: "window.entcore.$", named: [] },
  underscore: { source: "window.entcore._", named: [] },
};

const APP_NAME = "<module-name>"; // e.g. "presences"

// Proxy routes — common Entcore routes + app-specific backend routes:
routes: [
  "/conf/public",
  "^/(?=applications-list)",
  "^/(?=assets)",
  "^/(?=theme|locale|i18n|skin)",
  "^/(?=auth|appregistry|cas|userbook|directory|communication|conversation|portal|session|timeline|workspace|infra)",
  `^/${APP_NAME}/(?!public/)`,
  // Add sibling module routes ONLY if this module makes runtime HTTP calls to them
  // (TS path aliases like @incidents are compile-time only — no proxy route needed)
],
```

**`resolve.alias` — required for multi-module repos.** `vite.config.dev.ts` standalone (does NOT merge `vite.config.shared.ts`), so any path aliases module imports (`@common`, `@incidents`, …) must copy into dev config `resolve` block, else dev server fails "dependencies could not be resolved". Copy verbatim from shared config:

```ts
resolve: {
  extensions: [".ts", ".js"],
  alias: {
    "@common": path.resolve(__dirname, "../common/src/main/resources/ts"),
    "@presences": path.resolve(__dirname, "./src/main/resources/public/ts"),
    // … same aliases as vite.config.shared.ts
  },
},
```

Single-module apps with no `@`-aliased imports omit this block.

### `src/main/resources/public/index.html`

Dev server entry point. Must:
- Load `ng-app.js` synchronously (installs Entcore globals before app code)
- Reference `app.ts` and `behaviours.ts` as `type="module"`
- Match prod `ng-controller` and body classes

### `env.template` (repo root)

Add Vite-specific vars:

```
VITE_XSRF_TOKEN=     # User XSRF token from browser DevTools
VITE_ONE_SESSION_ID= # oneSessionId cookie value
VITE_RECETTE=        # Staging environment URL (e.g. https://recette.example.com)
```

---

## Integration (root `package.json`)

Add dev script matching existing `build:<module>` pattern:

```json
"dev:<module>": "vite --config <module>/vite.config.dev.ts"
```

Example: `"dev:presences": "vite --config presences/vite.config.dev.ts"`

---

## ENTCORE_EXPORTS — how to determine

Scan named imports from `'entcore'` across **every module the dev server bundles** — not just target module's own `src/`, but all `@`-aliased deps (`@common`, `@incidents`, …) too. Bundler follows those imports, so missing export there (e.g. `appPrefix` imported by `@common`) breaks at runtime with `module '…entcore' doesn't provide an export named 'X'`, which cascades (`app.ts` throws → controllers never register → `MainController is not
registered`).

```bash
grep -rhE "from ['\"]entcore['\"]" \
  ../common/src/main/resources/ts/ \
  ./src/main/resources/public/ts/ \
  ../incidents/src/main/resources/public/ts/ \
  ../massmailing/src/main/resources/public/ts/ \
  ../statistics-presences/src/main/resources/public/ts/ \
  --include="*.ts" \
  | grep -oP "(?<=\{)[^}]+" | tr ',' '\n' | sed -E 's/ as .*//' | tr -d ' ' \
  | grep -v '^$' | sort -u
```

Include only **value** imports — type-only imports (`IScope`, `Moment`, etc.) elided by esbuild, no need in list. Note `import {idiom
as lang}` exports `idiom`, not `lang` — strip ` as <alias>` before deduping.

---

## Proxy routes — how to determine

Beyond common Entcore routes, find every backend prefix module calls at runtime (each needs proxy route):

```bash
grep -rhoE "http\.(get|post|put|delete)\(\`/[a-z-]+" src/ --include="*.ts" \
  | grep -oE "/[a-z-]+$" | sort -u
```

Add `^/(?=<prefix>)` for each (e.g. presences also calls `/viescolaire`).

---

## Verify

```bash
node_modules/.bin/vite --config <module>/vite.config.dev.ts
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:4200/ts/app.ts   # expect 200
```

`app.ts` should transform with `import … from "/@id/__x00__entcore"` (proof entcoreGlobals plugin intercepted bare `entcore` import). "dependencies could not be resolved" error for `@common`/`@incidents` means `resolve.alias` block missing.

> Env note: native `esbuild`/`rollup` binaries platform-specific. If `node_modules` installed on another OS (e.g. Linux CI), run `yarn install` on your machine first — else Vite fails to start before any config error.

---

## How it works (key concepts)

**entcoreGlobals plugin**: intercepts `import X from "entcore"`, replaces with virtual module reading `window.entcore.X`. Same for angular/moment/etc. Stops Vite loading second copy of AngularJS.

**appPrefixRewrite plugin**: prod HTML links assets as `/<module>/public/template/…`. In dev Vite serves from `/` root, so plugin strips `/<module>/public` prefix. Blocks `/dist/` to stop stale build artifacts mixing with Vite output.

**Behaviours loading (critical).** entcore lazily fetches `/<module>/public/js/behaviours.js` via `http().get`; jQuery auto-evaluates JS response, which in prod runs `Behaviours.register(...)` (sniplets, rights). In dev no built bundle. Returning **404 actively harmful**: entcore error path sets `applicationsBehaviours[module] = {failed:true}`, which CLOBBERS the registration ESM `behaviours.ts` (loaded in `index.html`) already made — so sniplets resolve to `undefined` (`e.sniplets is undefined` for `presence-form`, `navigation`, …). So `appPrefixRewrite` plugin serves **200 classic shim** at that path — `import("/ts/behaviours.ts");` — keeping entcore on success path (re-reads registered object, flushes pending callbacks) and re-triggering ESM registration idempotently.

> Single-module app rendering no sniplet on initial load may not hit this (race resolves before any lookup), which is why 404-block can appear to work. Apps with portal sniplets (memento/navigation) fail without shim.

**injectAppPrefix plugin**: injects `window.appPrefix = "<module>"` into `index.html` before app scripts run, so bootstrap code computing paths from `window.appPrefix` works.

**devProxy**: when `.env` has `VITE_*` vars, proxies API calls to staging URL with session cookies. Without vars, falls back to `localhost:8090`. The `headers` it returns apply via `server.headers` to **every** dev response — so it sets only `set-cookie` (auth), never `Cache-Control`. A `max-age` there would also cache `template/*.html` files entcore fetches at runtime, making edits not reflect across reloads/restarts until expiry. Vite default `no-cache` is what you want for those templates.

> Symptom: HTML template edits don't show even after reload/restart. Cause: caching header on dev origin. After removing it, one hard reload (Cmd/Ctrl+Shift+R) clears copies browser cached before fix.