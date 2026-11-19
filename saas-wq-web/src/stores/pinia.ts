import { createPinia } from 'pinia'

/**
 * Single Pinia instance shared by the app and by non-component modules such as
 * the Axios interceptors, which need reactive stores outside of a setup scope.
 */
export const pinia = createPinia()
