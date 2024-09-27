module.exports = {
    "transform": {
        ".(ts|tsx)": "<rootDir>/node_modules/ts-jest/preprocessor.js"
    },
    "testRegex": "(/__tests__/.*|\\.(test|spec))\\.(ts|tsx|js)$",
    "moduleFileExtensions": [
        "ts",
        "tsx",
        "js"
    ],
    "testPathIgnorePatterns": [
        "/node_modules/",
        "<rootDir>/presences/build/",
        "<rootDir>/presences/out/",
        "<rootDir>/incidents/build/",
        "<rootDir>/incidents/out/",
        "<rootDir>/common/build/"
    ],
    "verbose": true,
    "testURL": "http://localhost/",
    "coverageDirectory": "coverage/front",
    "coverageReporters": [
        "text",
        "cobertura"
    ],
    "moduleNameMapper": {
        "^@common(.*)$": "<rootDir>/common/src/main/resources/ts$1",
        "^@incidents(.*)$": "<rootDir>/incidents/src/main/resources/public/ts$1",
        "^@presences(.*)$": "<rootDir>/app-presences/src/main/resources/public/ts$1",
        "^@massmailing(.*)$": "<rootDir>/massmailing/src/main/resources/public/ts$1",
        "^@statistics(.*)$": "<rootDir>/statistics-presences/src/main/resources/public/ts$1",
        '^axios$': require.resolve('axios')
    }
};
