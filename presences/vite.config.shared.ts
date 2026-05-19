import path = require("path");
import type { UserConfig } from "vite";

export default {
  root: path.resolve(__dirname, "./src/main/resources/public/"),
  build: {
    outDir: ".",
    sourcemap: true,
    minify: false,
    emptyOutDir: false,
    rollupOptions: {
      output: {
        entryFileNames: "dist/[name].js",
        format: "umd",
        globals: {
          entcore: "entcore",
          "entcore/entcore": "entcore",
          moment: "entcore",
          underscore: "entcore",
          jquery: "entcore",
          angular: "angular",
        },
      },
      external: [
        "entcore/entcore",
        "entcore",
        "moment",
        "underscore",
        "jquery",
        "angular",
      ],
    },
  },
  resolve: {
    extensions: [".ts", ".js"],
    alias: {
      "@common": path.resolve(__dirname, "../common/src/main/resources/ts"),
      "@presences": path.resolve(__dirname, "./src/main/resources/public/ts"),
      "@incidents": path.resolve(__dirname, "../incidents/src/main/resources/public/ts"),
      "@massmailing": path.resolve(__dirname, "../massmailing/src/main/resources/public/ts"),
      "@statistics": path.resolve(__dirname, "../statistics-presences/src/main/resources/public/ts"),
    },
  },
  optimizeDeps: {
    exclude: ["entcore", "entcore-toolkit"],
  },
} satisfies UserConfig;
