import { create } from 'zustand'

export interface TakeawayParcel {
  id: string
  customerName: string
  customerPhone: string
  createdAt: string
  status: 'open' | 'billed'
}

interface TakeawayState {
  parcels: TakeawayParcel[]
  addParcel: (name: string, phone: string) => TakeawayParcel
  markBilled: (id: string) => void
  removeParcel: (id: string) => void
}

export const useTakeawayStore = create<TakeawayState>((set) => ({
  parcels: [],
  addParcel(name, phone) {
    const parcel: TakeawayParcel = {
      id: `parcel-${Date.now()}`,
      customerName: name.trim() || 'Walk-in',
      customerPhone: phone.trim(),
      createdAt: new Date().toISOString(),
      status: 'open',
    }
    set((s) => ({ parcels: [parcel, ...s.parcels] }))
    return parcel
  },
  markBilled(id) {
    set((s) => ({
      parcels: s.parcels.map((p) =>
        p.id === id ? { ...p, status: 'billed' } : p,
      ),
    }))
  },
  removeParcel(id) {
    set((s) => ({ parcels: s.parcels.filter((p) => p.id !== id) }))
  },
}))
