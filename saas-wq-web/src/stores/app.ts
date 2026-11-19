import { ref } from 'vue'
import { defineStore } from 'pinia'

/** Shell preferences: the desktop sidebar state and the mobile drawer visibility. */
export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)

  const mobileMenuOpen = ref(false)

  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setSidebarCollapsed(value: boolean): void {
    sidebarCollapsed.value = value
  }

  function setMobileMenuOpen(value: boolean): void {
    mobileMenuOpen.value = value
  }

  return { sidebarCollapsed, mobileMenuOpen, toggleSidebar, setSidebarCollapsed, setMobileMenuOpen }
})
