# Vite Dev Server — Entcore AngularJS Module

Adds a hot-reloading dev server to a legacy Entcore AngularJS module that
already has Vite production builds (`vite.config.application.ts` /
`vite.config.behaviours.ts`).

## Context

These apps run in an Entcore platform. At runtime, `ng-app.js` installs global
objects (`window.entcore`, `window.angular`, …). Vite must NOT re-bundle those
— doing so loads duplicate runtimes and breaks AngularJS.

The dev server proxies API calls to a staging environment using session tokens
from a local `.env` file.

---

## Files to create

### `vite/plugins/` (copy verbatim — reusable across all modules)

| File | Purpose |
|---|---|
| `devProxy.ts` | Builds proxy config from `.env` tokens |
| `entcoreGlobals.ts` | Virtual modules for `entcore`/UMD globals |
| `appPrefixRewrite.ts` | Rewrites `/app/public/…` → `/…` in dev |
| `injectAppPrefix.ts` | Injects `window.appPrefix` in `index.html` |

These four files are identical across modules. Copy from `calendar/vite/plugins/`.

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

**`resolve.alias` — required for multi-module repos.** `vite.config.dev.ts` is
standalone (it does NOT merge `vite.config.shared.ts`), so any path aliases the
module imports (`@common`, `@incidents`, …) must be copied into the dev config's
`resolve` block, or the dev server fails with "dependencies could not be
resolved". Copy them verbatim from the shared config:

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

Single-module apps with no `@`-aliased imports can omit this block.

### `src/main/resources/public/index.html`

Entry point for the dev server. Must:
- Load `ng-app.js` synchronously (installs Entcore globals before app code)
- Reference `app.ts` and `behaviours.ts` as `type="module"`
- Match the production `ng-controller` and body classes

### `env.template` (repo root)

Add Vite-specific variables:

```
VITE_XSRF_TOKEN=     # User XSRF token from browser DevTools
VITE_ONE_SESSION_ID= # oneSessionId cookie value
VITE_RECETTE=        # Staging environment URL (e.g. https://recette.example.com)
```

---

## Integration (root `package.json`)

Add dev script consistent with existing `build:<module>` pattern:

```json
"dev:<module>": "vite --config <module>/vite.config.dev.ts"
```

Example: `"dev:presences": "vite --config presences/vite.config.dev.ts"`

---

## ENTCORE_EXPORTS — how to determine

Scan named imports from `'entcore'` across **every module the dev server
bundles** — not just the target module's own `src/`, but all `@`-aliased deps
(`@common`, `@incidents`, …) too. The bundler follows those imports, so a
missing export there (e.g. `appPrefix` imported by `@common`) breaks at runtime
with `module '…entcore' doesn't provide an export named 'X'`, which then
cascades (`app.ts` throws → controllers never register → `MainController is not
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

Include only **value** imports — type-only imports (`IScope`, `Moment`, etc.)
are elided by esbuild and don't need to be in the list. Note that `import {idiom
as lang}` exports `idiom`, not `lang` — strip ` as <alias>` before deduping.

---

## Proxy routes — how to determine

Beyond the common Entcore routes, find every backend prefix the module calls at
runtime (each needs a proxy route):

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

`app.ts` should transform with `import … from "/@id/__x00__entcore"` (proof the
entcoreGlobals plugin intercepted the bare `entcore` import). A "dependencies
could not be resolved" error for `@common`/`@incidents` means the `resolve.alias`
block is missing.

> Env note: native `esbuild`/`rollup` binaries are platform-specific. If
> `node_modules` was installed on another OS (e.g. Linux CI), run `yarn install`
> on your machine first — otherwise Vite fails to start before any config error.

---

## How it works (key concepts)

**entcoreGlobals plugin**: intercepts `import X from "entcore"` and replaces it
with a virtual module that reads `window.entcore.X`. Same for angular/moment/etc.
Prevents Vite from loading a second copy of AngularJS.

**appPrefixRewrite plugin**: production HTML links assets as
`/<module>/public/template/…`. In dev, Vite serves from `/` root, so the
plugin strips the `/<module>/public` prefix. Blocks `/dist/` to prevent stale
build artifacts from mixing with Vite output.

**Behaviours loading (critical).** entcore lazily fetches
`/<module>/public/js/behaviours.js` via `http().get`; jQuery auto-evaluates the
JS response, which in production runs `Behaviours.register(...)` (sniplets,
rights). In dev there is no built bundle. Returning **404 is actively harmful**:
entcore's error path sets `applicationsBehaviours[module] = {failed:true}`,
which CLOBBERS the registration the ESM `behaviours.ts` (loaded in `index.html`)
already made — so sniplets resolve to `undefined` (`e.sniplets is undefined`
for `presence-form`, `navigation`, …). The `appPrefixRewrite` plugin therefore
serves a **200 classic shim** at that path — `import("/ts/behaviours.ts");` —
which keeps entcore on its success path (re-reads the registered object, flushes
pending callbacks) and re-triggers the ESM registration idempotently.

> A single-module app that renders no sniplet on initial load may not hit this
> (the race resolves before any lookup), which is why a 404-block can appear to
> work. Apps with portal sniplets (memento/navigation) fail without the shim.

**injectAppPrefix plugin**: injects `window.appPrefix = "<module>"` into
`index.html` before app scripts run, so any bootstrap code that computes paths
from `window.appPrefix` works correctly.

**devProxy**: when `.env` has `VITE_*` vars, proxies API calls to the staging
URL with session cookies. Without vars, falls back to `localhost:8090`. The
`headers` it returns are applied via `server.headers` to **every** dev
response — so it sets only `set-cookie` (auth), never `Cache-Control`. A
`max-age` there would also cache the `template/*.html` files entcore fetches at
runtime, making edits not reflect across reloads/restarts until expiry. Vite's
default `no-cache` is what you want for those templates.

> Symptom: HTML template edits don't show up even after reload/restart. Cause:
> a caching header on the dev origin. After removing it, one hard reload
> (Cmd/Ctrl+Shift+R) clears copies the browser cached before the fix.
