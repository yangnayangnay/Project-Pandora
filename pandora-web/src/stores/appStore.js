import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const loading = ref(false)
  const themeConfig = ref({})
  const sidebarCollapsed = ref(false)

  function setLoading(val) {
    loading.value = val
  }

  function setThemeConfig(config) {
    themeConfig.value = config
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return {
    loading,
    themeConfig,
    sidebarCollapsed,
    setLoading,
    setThemeConfig,
    toggleSidebar
  }
})