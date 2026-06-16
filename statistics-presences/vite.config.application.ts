import path = require("path");
import { mergeConfig, type UserConfig } from "vite";
import sharedConfig from "./vite.config.shared";

export default mergeConfig(sharedConfig, {
  build: {
    cssCodeSplit: false,
    rollupOptions: {
      input: {
        application: path.resolve(__dirname, "./src/main/resources/public/ts/app.ts"),
      },
      output: {
        name: "statistics-presences",
        assetFileNames: "css/statistics-presences[extname]",
      },
    },
  },
} satisfies UserConfig);
