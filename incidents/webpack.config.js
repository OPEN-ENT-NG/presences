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
        "moment": "entcore",
        "underscore": "entcore",
        "jquery": "entcore",
        "angular": "angular"
    },
    resolve: {
        modulesDirectories: ['node_modules'],
        root: path.resolve(__dirname),
        extensions: ['', '.ts', '.js']
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