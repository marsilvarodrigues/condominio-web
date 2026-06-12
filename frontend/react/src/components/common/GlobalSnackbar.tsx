import { Alert, Snackbar } from '@mui/material'
import { useNotificationStore } from '@/store/notificationStore'

/**
 * Renders a single global Snackbar driven by {@link useNotificationStore}.
 * Mount once near the root of the app — it manages its own visibility.
 */
export function GlobalSnackbar() {
  const { notification, clear } = useNotificationStore()

  return (
    <Snackbar
      open={!!notification}
      autoHideDuration={notification?.severity === 'error' ? 8000 : 4000}
      onClose={clear}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
    >
      {notification ? (
        <Alert severity={notification.severity} onClose={clear} sx={{ width: '100%' }} variant="filled">
          {notification.message}
        </Alert>
      ) : (
        <span />
      )}
    </Snackbar>
  )
}
