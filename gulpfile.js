var gulp = require('gulp');
var webpack = require('webpack-stream');
var merge = require('merge2');
const replace = require('gulp-replace');
var clean = require('gulp-clean');
var args = require('yargs').argv;

var apps = ['presences', 'incidents', 'massmailing', 'statistics-presences'];

if (args.targetModule) {
    console.log("using arg:", args.targetModule);
    apps = [args.targetModule];
}

gulp.task('drop-cache', function () {
    var streams = [];
    apps.forEach(function (app) {
        streams.push(gulp.src(['./' + app + '/src/main/resources/public/dist'], {read: false}).pipe(clean()))
        streams.push(gulp.src(['./' + app + '/build'], {read: false}).pipe(clean()))
    });
    return merge(streams);
});

gulp.task('webpack', ['drop-cache'], function () {
    var streams = [];
    apps.forEach(function (app) {
        streams.push(gulp.src('./' + app + '/src/main/resources/public/**/*.ts')
            .pipe(webpack(require('./' + app + '/webpack.config.js')))
            .on('error', function handleError() {
                this.emit('end'); // Recover from errors
            })
            .pipe(gulp.dest('./' + app + '/src/main/resources/public/dist')))
    });
    return merge(streams);
});

gulp.task('build', ['webpack'], function () {
    var streams = [];
    apps.forEach(function (app) {
        streams.push(gulp.src("./" + app + "/src/main/resources/view-src/**/*.+(html|json)")
            .pipe(replace('@@VERSION', Date.now()))
            .pipe(gulp.dest("./" + app + "/src/main/resources/view")));
        streams.push(gulp.src("./" + app + "/src/main/resources/public/dist/behaviours.js")
            .pipe(gulp.dest("./" + app + "/src/main/resources/public/js")));
    });
    return merge(streams);
});

/* ------------------------------------------------------------------------- *
 * Développement local (cf. dev-server.js et README « Développement local »)
 *
 * Un seul module est reconstruit à la fois : les 4 bundles partagent common/
 * et des alias croisés, tout recompiler à chaque save coûterait ~20s.
 * Module ciblé : --targetModule=<module> ou MODULE=<module> (défaut presences).
 * ------------------------------------------------------------------------- */

var devApp = args.targetModule || process.env.MODULE || 'presences';

gulp.task('copy-behaviours-dev', function () {
    return gulp.src('./' + devApp + '/src/main/resources/public/dist/behaviours.js')
        .pipe(gulp.dest('./' + devApp + '/src/main/resources/public/js'));
});

// Pas de 'drop-cache' ici : webpack doit réécrire dist/ en place pour que le
// watcher de browser-sync voie des évènements "change" propres (auto-reload).
gulp.task('watch-dev', function () {
    var config = require('./' + devApp + '/webpack.config.js');
    // Mode watch natif de webpack : compilation incrémentale (~1s) et suivi des
    // dépendances réelles, donc une modif dans common/ ou dans un module aliasé
    // (@incidents, @statistics...) reconstruit bien le bundle ciblé.
    config.watch = true;

    console.log('[watch-dev] module: ' + devApp);

    webpack(config)
        // On log sans émettre 'end' : un emit('end') fermerait le stream et le
        // watch s'arrêterait à la première erreur de compilation TypeScript.
        .on('error', function (err) {
            console.error('[watch-dev] ' + (err && err.message ? err.message : err));
        })
        .pipe(gulp.dest('./' + devApp + '/src/main/resources/public/dist'));

    gulp.watch('./' + devApp + '/src/main/resources/public/dist/*.js', ['copy-behaviours-dev']);
});
