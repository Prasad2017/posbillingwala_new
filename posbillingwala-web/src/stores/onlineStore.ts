import { create } from 'zustand'

interface OnlineState {
  online: boolean
  setOnline: (online: boolean) => void
}

export const useOnlineStore = create<OnlineState>((set) => ({
  online: typeof navigator === 'undefined' ? true : navigator.onLine,
  setOnline(online) {
    set({ online })
  },
}))

export function startOnlineListener() {
  const update = () => useOnlineStore.getState().setOnline(navigator.onLine)
  window.addEventListener('online', update)
  window.addEventListener('offline', update)
  update()
  return () => {
    window.removeEventListener('online', update)
    window.removeEventListener('offline', update)
  }
}
