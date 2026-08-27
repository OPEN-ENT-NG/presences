/**
 * Watcher Sass du module en cours de développement (lancé par `yarn dev`).
 *
 * Seuls les modules ayant un `sass/index.scss` produisent une feuille de style :
 * aujourd'hui c'est le cas de `presences` uniquement — les autres modules n'ont
 * que des partiels, sans point d'entrée, donc rien à compiler. Dans ce cas on
 * sort proprement (code 0) plutôt que de faire échouer `yarn dev`.
 */
const fs = require('fs');
const path = require('path');
const {spawn} = require('child_process');

const target = process.env.MODULE || 'presences';
const publicDir = path.resolve(__dirname, '..', target, 'src/main/resources/public');
const entry = path.join(publicDir, 'sass/index.scss');
const output = path.join(publicDir, 'css', target + '.css');

if (!fs.existsSync(entry)) {
    console.log(`[sass] aucun point d'entrée (${path.relative(process.cwd(), entry)}) : rien à surveiller pour "${target}".`);
    process.exit(0);
}

// Sous Windows, .bin/sass est un script généré (sass.cmd), pas le binaire direct.
const sassBin = path.resolve(__dirname, '../node_modules/.bin', process.platform === 'win32' ? 'sass.cmd' : 'sass');
const args = ['--load-path=node_modules/', '--no-source-map', '--watch', `${entry}:${output}`];

console.log(`[sass] surveillance de ${path.relative(process.cwd(), entry)}`);

const child = spawn(sassBin, args, {stdio: 'inherit', cwd: path.resolve(__dirname, '..')});
child.on('error', (err) => {
    console.error(`[sass] impossible de lancer "${sassBin}" : ${err.message}`);
    process.exit(1);
});
child.on('exit', (code) => process.exit(code === null ? 0 : code));
