import { createApp } from "vue";
import { createPinia } from "pinia";
import piniaPluginPersistedstate from "pinia-plugin-persistedstate";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";
import { config as configureMarkdownEditor } from "md-editor-v3";

import App from "./App.vue";
import router from "./router";
import { applyMarkdownCompatibility } from "./utils/markdown/index.js";
import "./styles/index.css";
import "./styles/element-overrides.css";
import "./styles/article-preview.css";
import "katex/dist/katex.min.css";
import "highlight.js/styles/github-dark.css";

configureMarkdownEditor({
  markdownItConfig(md) {
    applyMarkdownCompatibility(md);
  }
});

const app = createApp(App);
const pinia = createPinia();
pinia.use(piniaPluginPersistedstate);

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.use(pinia);
app.use(ElementPlus);
app.use(router);
app.mount("#app");
