import { create } from 'zustand'

type Severity = 'success' | 'error' | 'warning' | 'info'

interface Notification {
  message: string
  severity: Severity
}

interface NotificationStore {
  notification: Notification | null
  notify: (message: string, severity?: Severity) => void
  notifyError: (message: string) => void
  notifySuccess: (message: string) => void
  clear: () => void
}

export const useNotificationStore = create<NotificationStore>((set) => ({
  notification: null,
  notify: (message, severity = 'info') => set({ notification: { message, severity } }),
  notifyError: (message) => set({ notification: { message, severity: 'error' } }),
  notifySuccess: (message) => set({ notification: { message, severity: 'success' } }),
  clear: () => set({ notification: null }),
}))
