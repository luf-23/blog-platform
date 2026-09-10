import { computed, watch } from "vue";
import { useAppStore } from "../store/app.js";

const THEMES = ["light", "dark"];

function applyTheme(theme) {
  if (typeof document === "undefined") return;
  const target = THEMES.includes(theme) ? theme : "light";
  document.documentElement.setAttribute("data-theme", target);
}

export function useTheme() {
  const appStore = useAppStore();

  const theme = computed({
    get: () => appStore.theme,
    set: (val) => appStore.setTheme(val)
  });

  const isDark = computed(() => theme.value === "dark");

  function toggle() {
    appStore.setTheme(isDark.value ? "light" : "dark");
  }

  watch(
    theme,
    (val) => {
      applyTheme(val);
    },
    { immediate: true }
  );

  return { theme, isDark, toggle, toggleTheme: toggle };
}

export function initTheme(initial = "light") {
  applyTheme(initial);
}
