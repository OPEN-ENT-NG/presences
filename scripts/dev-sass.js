/**
 * Sass watcher for the module being developed (started by `yarn dev`).
 *
 * Only modules with a `sass/index.scss` produce a stylesheet: today that is
 * `presences` alone — the others only hold partials, with no entry point, so
 * there is nothing to compile. In that case we exit cleanly (code 0) rather
 * than failing `yarn dev`.
 *
 * The output is `<module>.dev.css`, kept apart from the `<module>.css` that
 * `yarn build:sass` produces: the latter is the artifact the server build
 * regenerates, and mixing the two would only make them indistinguishable.
 * Both are gitignored.
 */
const fs = require('fs');
const path = require('path');
const {spawn} = require('child_process');

const target = process.env.MODULE || 'presences';
const publicDir = path.resolve(__dirname, '..', target, 'src/main/resources/public');
const entry = path.join(publicDir, 'sass/index.scss');
const output = path.join(publicDir, 'css', target + '.dev.css');

if (!fs.existsSync(entry)) {
    console.log(`[sass] no entry point (${path.relative(process.cwd(), entry)}): nothing to watch for "${target}".`);
    process.exit(0);
}

// On Windows, .bin/sass is a generated shim (sass.cmd), not the binary itself.
const sassBin = path.resolve(__dirname, '../node_modules/.bin', process.platform === 'win32' ? 'sass.cmd' : 'sass');
const args = ['--load-path=node_modules/', '--no-source-map', '--watch', `${entry}:${output}`];

console.log(`[sass] watching ${path.relative(process.cwd(), entry)}`);

const child = spawn(sassBin, args, {stdio: 'inherit', cwd: path.resolve(__dirname, '..')});
child.on('error', (err) => {
    console.error(`[sass] cannot start "${sassBin}": ${err.message}`);
    process.exit(1);
});
child.on('exit', (code) => process.exit(code === null ? 0 : code));
