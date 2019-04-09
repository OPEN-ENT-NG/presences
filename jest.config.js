module.exports = {
    "transform": {
        ".(ts|tsx)": "<rootDir>/node_modules/ts-jest/preprocessor.js"
    },
    "testRegex": "(/__tests__/.*|\\.(test|spec))\\.(ts|tsx|js)$",
    "moduleNameMapper": {
        "@model": "<rootDir>/src/main/resources/public/ts/models/index.ts"
    },
    "moduleFileExtensions": [
        "ts",
        "tsx",
        "js"
    ],
    "verbose": true,
    "testURL": "http://localhost/"
};
