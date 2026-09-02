/**
 * Local development server.
 *
 *   Browser ──▶ http://localhost:3000/<module>
 *                 │
 *                 ├─ /<module>/public/**  → local disk (dist/, js/, css/, template/, img/)
 *                 │                          the module being developed only
 *                 └─ everything else      → proxied to the recette (page, ng-app.js,
 *                                            theme, i18n, API, AND the 3 other modules)
 *
 * Only the target module is served from disk. The other modules of the bundle
 * (they reference each other at runtime: sniplets, behaviours.js, images) come
 * from the recette, because the watcher rebuilds only one of them: their local
 * dist/ is whatever the last full build produced, often weeks old and from
 * another branch. Serving those would silently mix versions — typically an
 * empty screen on an app that works fine on the recette.
 */
const {exec} = require('child_process');
const fs = require('fs');
const path = require('path');
const httpProxy = require('http-proxy');
const browserSync = require('browser-sync').create();

const MODULES = ['presences', 'incidents', 'massmailing', 'statistics-presences'];
const PORT = Number(process.env.PORT) || 3000;

const target = process.env.MODULE || 'presences';
if (MODULES.indexOf(target) === -1) {
    console.error('Unknown module: "' + target + '". Expected one of: ' + MODULES.join(', '));
    process.exit(1);
}

// Copied from entcore/admin proxy-development.conf.js (no dotenv dependency)
const parseEnvFile = (content) => {
    const result = {};
    for (const line of content.split(/\r?\n/)) {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith('#')) continue;
        const eqIndex = trimmed.indexOf('=');
        if (eqIndex === -1) continue;
        const key = trimmed.slice(0, eqIndex).trim();
        let value = trimmed.slice(eqIndex + 1).trim();
        if ((value.startsWith('"') && value.endsWith('"')) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            value = value.slice(1, -1);
        }
        result[key] = value;
    }
    return result;
};

const envPath = path.resolve(__dirname, '.env');
if (!fs.existsSync(envPath)) {
    console.error(
        'Missing .env file. Copy .env.template to .env then run ' +
        '`dev-auth-fetcher connect` (or the auth-user-frontend skill).'
    );
    process.exit(1);
}
const env = parseEnvFile(fs.readFileSync(envPath, 'utf-8'));
const {VITE_RECETTE, VITE_XSRF_TOKEN, VITE_ONE_SESSION_ID} = env;
if (!VITE_RECETTE) {
    console.error('VITE_RECETTE missing in .env');
    process.exit(1);
}

// dev-auth-fetcher writes the URL with a trailing slash, which would produce
// `//presences` upstream (404).
const recette = VITE_RECETTE.replace(/\/+$/, '');
const appAddress = '/' + target;
const cookie = `oneSessionId=${VITE_ONE_SESSION_ID}; authenticated=true; XSRF-TOKEN=${VITE_XSRF_TOKEN}`;

// Manual proxy rather than browser-sync's `proxy` option: that one forces the
// local server to https as soon as the target is https (not disableable via
// `https: false`), which triggers a self-signed certificate and the browser's
// "connection not private" warning. Going through http-proxy in a middleware
// keeps the local server on http while proxying to the https recette.
const proxy = httpProxy.createProxyServer({
    target: recette,
    changeOrigin: true,
    secure: true,
    autoRewrite: true,
    // The local server is http, the recette is https. `autoRewrite` rewrites the
    // HOST of redirects (to localhost:3000) but keeps their PROTOCOL. Without
    // this, a redirect (typically /auth/login once the session has expired)
    // sends the browser to https://localhost:3000 → "this site can't provide a
    // secure connection", with no hint about the actual cause.
    protocolRewrite: 'http',
    // The HTML page is rewritten to inject the browser-sync client and the local
    // stylesheet (see buildInjection below), so writing the response is up to
    // us — http-proxy no longer touches it.
    selfHandleResponse: true,
});

// Only navigation documents are rewritten. Angular's partial templates are also
// text/html: without this filter, each one would get its own copy of the
// browser-sync client.
const wantsHtmlDocument = (req) => (req.headers.accept || '').indexOf('text/html') !== -1;

// Injects the recette session into every outgoing request (admin pattern)
proxy.on('proxyReq', (proxyReq, req) => {
    proxyReq.setHeader('cookie', cookie);
    proxyReq.setHeader('X-XSRF-TOKEN', VITE_XSRF_TOKEN || '');
    // Uncompressed response so we can rewrite it (HTML documents only)
    if (wantsHtmlDocument(req)) {
        proxyReq.setHeader('accept-encoding', 'identity');
    }
});

// An expired session shows up ONLY as a redirect to /auth/login: flag it
// explicitly, otherwise the browser just shows a blank page.
let sessionExpiredLogged = false;
proxy.on('proxyRes', (proxyRes, req, res) => {
    const location = proxyRes.headers['location'];
    if (!sessionExpiredLogged && location && location.indexOf('/auth/login') !== -1) {
        sessionExpiredLogged = true;
        console.error(
            '\nRecette session expired (redirected to /auth/login).\n' +
            'Regenerate .env with `dev-auth-fetcher connect`, then restart.\n'
        );
    }
    // Pins the set-cookie on the response: avoids session rotation
    proxyRes.headers['set-cookie'] = [
        `oneSessionId=${VITE_ONE_SESSION_ID}`,
        `XSRF-TOKEN=${VITE_XSRF_TOKEN}`,
        'authenticated=true',
    ];

    const contentType = proxyRes.headers['content-type'] || '';
    const rewritable = wantsHtmlDocument(req)
        && contentType.indexOf('text/html') !== -1
        && !proxyRes.headers['content-encoding'];

    if (!rewritable) {
        res.writeHead(proxyRes.statusCode, proxyRes.headers);
        proxyRes.pipe(res);
        return;
    }

    const chunks = [];
    proxyRes.on('data', (chunk) => chunks.push(chunk));
    proxyRes.on('end', () => {
        let body = Buffer.concat(chunks).toString('utf8');
        const injection = buildInjection();
        body = body.indexOf('</head>') !== -1
            ? body.replace('</head>', injection + '</head>')
            : body + injection;

        const headers = Object.assign({}, proxyRes.headers);
        headers['content-length'] = Buffer.byteLength(body);
        res.writeHead(proxyRes.statusCode, headers);
        res.end(body);
    });
});

proxy.on('error', (err, req, res) => {
    console.error('Proxy error to recette:', err.message);
    if (!res.headersSent) res.writeHead(502, {'Content-Type': 'text/plain'});
    res.end('Proxy error to recette: ' + err.message);
});

// Serves the target module's local dist/, template/, js/, css/, img/; a 404
// falls through to the proxy (so template/entcore/*, absent from the repo, does
// come from the recette).
// serve-static options must be carried by the entry itself (`options`):
// browser-sync ignores the global `serveStaticOptions` option for object entries.
const publicDir = path.resolve(__dirname, target, 'src/main/resources/public');
const serveStatic = [{
    route: `${appAddress}/public`,
    dir: publicDir,
    options: {
        cacheControl: false,
        setHeaders: (res) => res.setHeader('Cache-Control', 'no-store'),
    },
}];

if (!fs.existsSync(path.join(publicDir, 'dist/application.js'))) {
    console.warn(
        `\n[!] ${target}/src/main/resources/public/dist/application.js is missing: ` +
        `webpack's first build may still be running.\n` +
        `    Until it lands, the recette's bundle is served instead.\n`
    );
}

// Local stylesheet injection.
//
// The app CSS is not served from /<module>/public/css/: it is compiled into the
// skin's theme.css (ode-themes), which the recette serves and ng-app.js appends
// to <head> at runtime. Without the injection below, recompiling the Sass
// locally has no visible effect whatsoever.
//
// The link is added by a script rather than hardcoded in the HTML: ng-app.js
// inserts #theme afterwards, and at equal specificity the last <link> in the DOM
// wins. So we wait for #theme to place ourselves right after it, then stop
// observing: permanently keeping the link last conflicts with the <link> swap
// browser-sync performs to inject CSS — the two retrigger each other and end up
// freezing the page.
//
// What gets injected is the watcher's output (`<module>.dev.css`), not the
// `<module>.css` of `yarn build:sass`. The choice is based on the presence of
// the Sass entry point rather than of the file itself: on first launch the
// server is ready before Sass has written its first output.
const cssFile = fs.existsSync(path.join(publicDir, 'sass/index.scss'))
    ? target + '.dev.css'
    : target + '.css';
const localCss = path.join(publicDir, 'css', cssFile);
// Re-evaluated on every page served: freezing this at startup would disable the
// injection for the whole session if Sass has not finished compiling yet.
const hasLocalCss = () => fs.existsSync(localCss);
const cssUrl = `${appAddress}/public/css/${cssFile}`;
const localCssSnippet = `<script>(function () {
    var ID = 'dev-local-css';
    var observer;

    function link() {
        var el = document.getElementById(ID);
        if (!el) {
            el = document.createElement('link');
            el.id = ID;
            el.rel = 'stylesheet';
            el.href = '${cssUrl}';
            document.head.appendChild(el);
        }
        return el;
    }

    function place() {
        var theme = document.getElementById('theme');
        if (!theme) return;
        var el = link();
        if (theme.compareDocumentPosition(el) & Node.DOCUMENT_POSITION_PRECEDING) {
            theme.parentNode.insertBefore(el, theme.nextSibling);
        }
        if (observer) observer.disconnect(); // the theme is only inserted once
    }

    link();
    observer = new MutationObserver(place);
    observer.observe(document.head, {childList: true});
    place();
})();</script>`;

if (!hasLocalCss()) {
    console.warn(
        `\n[!] ${path.relative(__dirname, localCss)} does not exist yet: until it does, ` +
        `the recette's theme CSS is what you see.\n    The Sass watcher writes it within ` +
        `a second; if the module has no local Sass, this is permanent.\n`
    );
}

// What gets appended to the proxied page's <head>.
//
// The browser-sync client is part of it: with no `server` nor `proxy` option,
// browser-sync runs in "snippet" mode and rewrites no response — it merely
// prints the snippet to the console and waits for someone to paste it into the
// page. Without it no client is connected, so the `files` option never reloads
// anything.
function buildInjection() {
    const bsSnippet = browserSync.getOption('snippet') || '';
    return (hasLocalCss() ? localCssSnippet : '') + bsSnippet;
}

browserSync.init(
    {
        port: PORT,
        startPath: appAddress,
        // Do not let browser-sync open the browser (see the README, "it opens
        // port 3001" section): it is done manually below instead.
        open: false,
        ui: false,
        ghostMode: false,
        notify: false,
        serveStatic,
        middleware: [(req, res) => proxy.web(req, res)],
        // Auto-reload when webpack re-emits the bundle, or a template changes.
        // .css files are hot-injected by browser-sync, without a page reload.
        files: [
            `${target}/src/main/resources/public/dist/*.js`,
            `${target}/src/main/resources/public/js/behaviours.js`,
            `${target}/src/main/resources/public/template/**/*.html`,
            `${target}/src/main/resources/public/css/*.css`,
        ],
        watchEvents: ['change', 'add'],
        reloadDebounce: 500,
    },
    () => {
        const appUrl = `http://localhost:${PORT}${appAddress}`;
        console.log('\n  Module     : ' + target);
        console.log('  Recette    : ' + recette);
        console.log('  Application: ' + appUrl + '\n');
        const opener = process.platform === 'darwin' ? 'open'
            : process.platform === 'win32' ? 'start ""'
                : 'xdg-open';
        exec(`${opener} "${appUrl}"`, (err) => {
            if (err) console.log('Open manually: ' + appUrl);
        });
    }
);
