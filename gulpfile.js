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
 * Local development (see dev-server.js and the README's local development section)
 *
 * One module is rebuilt at a time: the 4 bundles share common/ and cross-module
 * aliases, so recompiling all of them on every save would cost ~20s.
 * Target module: --targetModule=<module> or MODULE=<module> (default presences).
 * ------------------------------------------------------------------------- */

var devApp = args.targetModule || process.env.MODULE || 'presences';

gulp.task('copy-behaviours-dev', function () {
    return gulp.src('./' + devApp + '/src/main/resources/public/dist/behaviours.js')
        .pipe(gulp.dest('./' + devApp + '/src/main/resources/public/js'));
});

// No 'drop-cache' here: webpack must overwrite dist/ in place so that
// browser-sync's watcher sees clean "change" events (auto-reload).
gulp.task('watch-dev', function () {
    var config = require('./' + devApp + '/webpack.config.js');
    // webpack's own watch mode: incremental compilation (~1s) and real dependency
    // tracking, so a change in common/ or in an aliased module (@incidents,
    // @statistics...) does rebuild the target bundle.
    config.watch = true;

    console.log('[watch-dev] module: ' + devApp);

    webpack(config)
        // Log without emitting 'end': emit('end') would close the stream and the
        // watch would stop at the first TypeScript compilation error.
        .on('error', function (err) {
            console.error('[watch-dev] ' + (err && err.message ? err.message : err));
        })
        .pipe(gulp.dest('./' + devApp + '/src/main/resources/public/dist'));

    gulp.watch('./' + devApp + '/src/main/resources/public/dist/*.js', ['copy-behaviours-dev']);
});
