/**
 * Serveur de développement local.
 *
 *   Navigateur ──▶ http://localhost:3000/<module>
 *                    │
 *                    ├─ /<module>/public/**  → disque local (dist/, js/, css/, template/, img/)
 *                    │                          uniquement le module en cours de développement
 *                    └─ tout le reste        → proxy vers la recette (page, ng-app.js,
 *                                               thème, i18n, API, ET les 3 autres modules)
 *
 * Seul le module ciblé est servi depuis le disque. Les autres modules du bundle
 * (ils se référencent entre eux au runtime : sniplets, behaviours.js, images)
 * viennent de la recette, car le watcher n'en reconstruit qu'un : leurs dist/
 * locaux sont ceux du dernier build complet, souvent vieux de plusieurs semaines
 * et issus d'une autre branche. Les servir donnerait un mélange de versions
 * silencieux — typiquement un écran vide sur une app qui fonctionne en recette.
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
    console.error('Module inconnu : "' + target + '". Attendu : ' + MODULES.join(', '));
    process.exit(1);
}

// Copié de entcore/admin proxy-development.conf.js (pas de dépendance dotenv)
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
        'Fichier .env manquant. Copier .env.template vers .env puis lancer ' +
        '`dev-auth-fetcher connect` (ou le skill auth-user-frontend).'
    );
    process.exit(1);
}
const env = parseEnvFile(fs.readFileSync(envPath, 'utf-8'));
const {VITE_RECETTE, VITE_XSRF_TOKEN, VITE_ONE_SESSION_ID} = env;
if (!VITE_RECETTE) {
    console.error('VITE_RECETTE absent du .env');
    process.exit(1);
}

// Le .env généré par dev-auth-fetcher peut contenir un slash final, qui donnerait
// des URL en `//presences` côté recette (404).
const recette = VITE_RECETTE.replace(/\/+$/, '');
const appAddress = '/' + target;
const cookie = `oneSessionId=${VITE_ONE_SESSION_ID}; authenticated=true; XSRF-TOKEN=${VITE_XSRF_TOKEN}`;

// Proxy manuel plutôt que l'option `proxy` de browser-sync : celle-ci bascule le
// serveur local en https dès que la cible est en https (non désactivable via
// `https: false`), ce qui déclenche un certificat auto-signé et l'avertissement
// « connexion non privée ». Passer par http-proxy dans un middleware garde le
// serveur local en http tout en proxyfiant vers la recette en https.
const proxy = httpProxy.createProxyServer({
    target: recette,
    changeOrigin: true,
    secure: true,
    autoRewrite: true,
    // Le serveur local est en http, la recette en https. `autoRewrite` réécrit
    // l'HOST des redirections (vers localhost:3000) mais conserve leur PROTOCOLE.
    // Sans ceci, une redirection (typiquement /auth/login quand la session a
    // expiré) envoie le navigateur sur https://localhost:3000 → « ce site ne peut
    // pas fournir de connexion sécurisée », sans indice sur la cause réelle.
    protocolRewrite: 'http',
    // La page HTML est réécrite pour y injecter le client browser-sync et la
    // feuille de style locale (cf. buildInjection plus bas) : c'est donc à nous
    // d'écrire la réponse, http-proxy n'y touche plus.
    selfHandleResponse: true,
});

// Seuls les documents de navigation sont réécrits. Les templates partiels chargés
// par Angular sont aussi du text/html : sans ce filtre, chacun se verrait injecter
// une copie du client browser-sync.
const wantsHtmlDocument = (req) => (req.headers.accept || '').indexOf('text/html') !== -1;

// Injecte la session de recette sur chaque requête sortante (pattern admin)
proxy.on('proxyReq', (proxyReq, req) => {
    proxyReq.setHeader('cookie', cookie);
    proxyReq.setHeader('X-XSRF-TOKEN', VITE_XSRF_TOKEN || '');
    // Réponse non compressée pour pouvoir la réécrire (documents HTML seulement)
    if (wantsHtmlDocument(req)) {
        proxyReq.setHeader('accept-encoding', 'identity');
    }
});

// Une session expirée ne se manifeste QUE par une redirection vers /auth/login :
// on la signale explicitement, sinon le navigateur affiche une page blanche.
let sessionExpiredLogged = false;
proxy.on('proxyRes', (proxyRes, req, res) => {
    const location = proxyRes.headers['location'];
    if (!sessionExpiredLogged && location && location.indexOf('/auth/login') !== -1) {
        sessionExpiredLogged = true;
        console.error(
            '\nSession de recette expirée (redirection vers /auth/login).\n' +
            'Régénérer le .env avec `dev-auth-fetcher connect`, puis relancer.\n'
        );
    }
    // Épingle le set-cookie de la réponse : évite la rotation de session
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
    console.error('Erreur de proxy vers la recette :', err.message);
    if (!res.headersSent) res.writeHead(502, {'Content-Type': 'text/plain'});
    res.end('Erreur de proxy vers la recette : ' + err.message);
});

// Sert les dist/, template/, js/, css/, img/ locaux du module ciblé ; un 404
// retombe sur le proxy (donc template/entcore/*, absent du repo, vient bien de
// la recette).
// Les options de serve-static doivent être portées par l'entrée elle-même
// (`options`) : browser-sync ignore l'option globale `serveStaticOptions` pour
// les entrées de type objet.
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
        `\n[!] ${target}/src/main/resources/public/dist/application.js est absent : ` +
        `le premier build de webpack est peut-être encore en cours.\n` +
        `    Tant qu'il manque, c'est le bundle de la recette qui est servi.\n`
    );
}

// Injection de la feuille de style locale.
//
// Le CSS applicatif n'est pas servi depuis /<module>/public/css/ : il est compilé
// dans le theme.css du skin (ode-themes), que la recette sert et que ng-app.js
// ajoute au <head> à l'exécution. Sans l'injection ci-dessous, recompiler le Sass
// en local n'a donc strictement aucun effet visible.
//
// Le link est ajouté par un script, et non en dur dans le HTML : ng-app.js insère
// #theme après coup, et à spécificité égale c'est le dernier <link> du DOM qui
// gagne. On attend donc #theme pour se placer juste après lui, puis on arrête
// d'observer : maintenir le link en dernière position en permanence entre en
// conflit avec le remplacement de <link> que fait browser-sync pour injecter le
// CSS — les deux se relancent mutuellement et finissent par figer la page.
const localCss = path.join(publicDir, 'css', target + '.css');
const hasLocalCss = fs.existsSync(localCss);
const cssUrl = `${appAddress}/public/css/${target}.css`;
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
        if (observer) observer.disconnect(); // le thème n'est inséré qu'une fois
    }

    link();
    observer = new MutationObserver(place);
    observer.observe(document.head, {childList: true});
    place();
})();</script>`;

if (!hasLocalCss) {
    console.warn(
        `\n[!] ${path.relative(__dirname, localCss)} est absent : le CSS affiché sera ` +
        `celui du thème de la recette.\n    Générer la feuille avec \`yarn build:sass\` ` +
        `si le module en possède une.\n`
    );
}

// Ce qui est ajouté au <head> de la page proxyfiée.
//
// Le client browser-sync en fait partie : sans option `server` ni `proxy`,
// browser-sync tourne en mode "snippet" et ne réécrit aucune réponse — il se
// contente d'afficher le snippet en console en attendant qu'on le colle dans la
// page. Sans lui, aucun client n'est connecté et l'option `files` ne recharge
// donc jamais rien.
function buildInjection() {
    const bsSnippet = browserSync.getOption('snippet') || '';
    return (hasLocalCss ? localCssSnippet : '') + bsSnippet;
}

browserSync.init(
    {
        port: PORT,
        startPath: appAddress,
        // Ne pas confier l'ouverture du navigateur à browser-sync (cf. README,
        // section « ça ouvre le port 3001 ») : elle est faite manuellement plus bas.
        open: false,
        ui: false,
        ghostMode: false,
        notify: false,
        serveStatic,
        middleware: [(req, res) => proxy.web(req, res)],
        // Auto-reload quand webpack ré-émet le bundle, ou qu'un template change.
        // Les .css sont injectés à chaud par browser-sync, sans rechargement.
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
        console.log('\n  Module      : ' + target);
        console.log('  Recette     : ' + recette);
        console.log('  Application : ' + appUrl + '\n');
        const opener = process.platform === 'darwin' ? 'open'
            : process.platform === 'win32' ? 'start ""'
                : 'xdg-open';
        exec(`${opener} "${appUrl}"`, (err) => {
            if (err) console.log('Ouvrir manuellement : ' + appUrl);
        });
    }
);
