import path = require("path");
import type { Plugin, UserConfig } from "vite";

const viteBannerPlugin: Plugin = {
  name: "vite-banner",
  generateBundle(_, bundle) {
    Object.values(bundle).forEach((chunk) => {
      if (chunk.type === "chunk") {
        chunk.code = "/* Built with Vite */\n" + chunk.code;
      }
    });
  },
};

export default {
  root: path.resolve(__dirname, "./src/main/resources/public/"),
  plugins: [viteBannerPlugin],
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
      "@massmailing": path.resolve(__dirname, "./src/main/resources/public/ts"),
      "@presences": path.resolve(__dirname, "../presences/src/main/resources/public/ts"),
      "@incidents": path.resolve(__dirname, "../incidents/src/main/resources/public/ts"),
    },
  },
  optimizeDeps: {
    exclude: ["entcore", "entcore-toolkit"],
  },
} satisfies UserConfig;
