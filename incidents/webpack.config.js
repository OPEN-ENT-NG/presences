var path = require('path');

module.exports = {
    entry: {
        application: './incidents/src/main/resources/public/ts/app.ts',
        behaviours: './incidents/src/main/resources/public/ts/behaviours.ts'
    },
    output: {
        filename: '[name].js',

        path: __dirname + 'dest'
    },
    externals: {
        "entcore/entcore": "entcore",
        "entcore": "entcore",
        "underscore": "entcore",
        "jquery": "entcore",
        "angular": "angular"
    },
    resolve: {
        modulesDirectories: ['node_modules'],
        root: path.resolve(__dirname),
        extensions: ['', '.ts', '.js'],
        alias: {
            "@common": path.resolve(__dirname, '../common/src/main/resources/ts'),
            "@incidents": path.resolve(__dirname, '../incidents/src/main/resources/public/ts')
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
};