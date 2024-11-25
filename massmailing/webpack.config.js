var path = require('path');

module.exports = {
    entry: {
        application: './massmailing/src/main/resources/public/ts/app.ts',
        behaviours: './massmailing/src/main/resources/public/ts/behaviours.ts'
    },
    output: {
        filename: '[name].js',
        path: __dirname + 'dest'
    },
    externals: {
        "entcore/entcore": "entcore",
        "entcore": "entcore",
        "moment": "entcore",
        "underscore": "entcore",
        "jquery": "entcore",
        "angular": "angular"
    },
    resolve: {
        modulesDirectories: ['node_modules'],
        extensions: ['', '.ts', '.js'],
        root: path.resolve(__dirname),
        alias: {
            "@common": path.resolve(__dirname, '../common/src/main/resources/ts'),
            "@presences": path.resolve(__dirname, '../presences/src/main/resources/public/ts'),
            "@incidents": path.resolve(__dirname, '../incidents/src/main/resources/public/ts'),
        }
    },
    devtool: "source-map",
    module: {
        loaders: [
            {
                test: /\.ts$/,
                loader: 'ts-loader'
            }
        ]
    }
}
