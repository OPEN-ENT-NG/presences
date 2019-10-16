var gulp = require('gulp');
var webpack = require('webpack-stream');
var merge = require('merge2');
var rev = require('gulp-rev');
var revReplace = require("gulp-rev-replace");
var clean = require('gulp-clean');
var args = require('yargs').argv;

var apps = ['presences', 'incidents', 'massmailing'];

if (args.module) {
    apps = [args.module];
}

gulp.task('drop-cache', function () {
    var streams = [];
    apps.forEach(function (app) {
        streams.push(gulp.src(['./' + app + '/src/main/resources/public/dist'], {read: false}).pipe(clean()))
        streams.push(gulp.src(['./' + app + '/build'], {read: false}).pipe(clean()))
    });
    return merge(streams);
});

gulp.task('copy-files', ['drop-cache'], function () {
    var streams = [];
    apps.forEach(function (app) {
        streams
            .push(gulp.src('./node_modules/entcore/src/template/**/*.html').pipe(gulp.dest('./' + app + '/src/main/resources/public/template/entcore')));
        streams.push(gulp.src('./node_modules/entcore/bundle/*').pipe(gulp.dest('./' + app + '/src/main/resources/public/dist/entcore')));
    });
    return merge(streams);
});

gulp.task('webpack', ['copy-mdi-font'], function () {
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

gulp.task('copy-mdi-font', ['copy-files'], function () {
    return gulp.src('./node_modules/@mdi/font/fonts/*')
        .pipe(gulp.dest('./presences/src/main/resources/public/font/material-design/fonts'));
});

gulp.task('rev', ['webpack'], function () {
    var streams = [];
    apps.forEach(function (app) {
        streams.push(gulp.src('./' + app + '/src/main/resources/public/dist/**/*.js')
            .pipe(rev())
            .pipe(gulp.dest('./' + app + '/src/main/resources/public/dist'))
            .pipe(rev.manifest())
            .pipe(gulp.dest('./' + app)))
    });
    return merge(streams);
});

gulp.task('build', ['rev'], function () {
    var streams = [];
    apps.forEach(function (app) {
        streams.push(gulp.src("./" + app + "/src/main/resources/view-src/**/*.html")
            .pipe(revReplace({manifest: gulp.src("./" + app + "/rev-manifest.json")}))
            .pipe(gulp.dest("./" + app + "/src/main/resources/view")));
        streams.push(gulp.src("./" + app + "/src/main/resources/public/dist/behaviours.js")
            .pipe(gulp.dest("./" + app + "/src/main/resources/public/js")));
    });
    return merge(streams);
});