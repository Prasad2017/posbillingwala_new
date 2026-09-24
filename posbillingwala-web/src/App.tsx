import { useEffect, type ReactNode } from 'react'
import {
  BrowserRouter,
  Navigate,
  Outlet,
  Route,
  Routes,
  useLocation,
} from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { bindAuthHandlers } from '@/api/client'
import { AppShell } from '@/app/AppShell'
import {
  LoginPage,
  MpinPage,
  RegisterPage,
  StaffLoginPage,
} from '@/features/auth/AuthPages'
import { HomePage } from '@/features/home/HomePage'
import { PosPage } from '@/features/pos/PosPage'
import { PaymentPage } from '@/features/pos/PaymentPage'
import { KotPreviewPage } from '@/features/pos/KotPreviewPage'
import { TablesPage } from '@/features/tables/TablesPage'
import { SplitBillPage } from '@/features/tables/SplitBillPage'
import { TakeawayPage } from '@/features/takeaway/TakeawayPage'
import { MessPage } from '@/features/mess/MessPage'
import {
  MealSessionsPage,
  MessPaymentsPage,
  MessScanPage,
  MessTokensTodayPage,
} from '@/features/mess/MessExtraPages'
import { MastersPage } from '@/features/masters/MastersPage'
import { InventoryPage } from '@/features/inventory/InventoryPage'
import { ReportsPage } from '@/features/reports/ReportsPage'
import { InvoiceDetailPage } from '@/features/reports/InvoiceDetailPage'
import { SettingsPage } from '@/features/settings/SettingsPage'
import { useAuthStore } from '@/stores/authStore'
import { startOnlineListener } from '@/stores/onlineStore'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
  },
})

function AuthBootstrap({ children }: { children: ReactNode }) {
  useEffect(() => {
    bindAuthHandlers({
      getToken: () => useAuthStore.getState().authToken,
      getStaffId: () => useAuthStore.getState().staff?.staffId ?? null,
      refreshToken: () => useAuthStore.getState().refreshToken(),
      softLogout: () => useAuthStore.getState().softLogout(),
    })
    return startOnlineListener()
  }, [])
  return children
}

function RequireAuth({
  allow,
}: {
  allow: Array<'authenticated' | 'needsMpin' | 'needsStaff' | 'anonymous'>
}) {
  const phase = useAuthStore((s) => s.phase)
  const location = useLocation()
  if (!allow.includes(phase)) {
    if (phase === 'authenticated') return <Navigate to="/" replace />
    if (phase === 'needsStaff') return <Navigate to="/staff-login" replace />
    if (phase === 'needsMpin') return <Navigate to="/mpin" replace />
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return <Outlet />
}

function AppRoutes() {
  return (
    <Routes>
      <Route element={<RequireAuth allow={['anonymous', 'needsMpin']} />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
      <Route element={<RequireAuth allow={['needsMpin']} />}>
        <Route path="/mpin" element={<MpinPage />} />
      </Route>
      <Route element={<RequireAuth allow={['needsStaff']} />}>
        <Route path="/staff-login" element={<StaffLoginPage />} />
      </Route>

      <Route element={<RequireAuth allow={['authenticated']} />}>
        <Route element={<AppShell />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/pos" element={<PosPage />} />
          <Route path="/pos/payment" element={<PaymentPage />} />
          <Route path="/pos/kot" element={<KotPreviewPage />} />
          <Route path="/tables" element={<TablesPage />} />
          <Route path="/tables/billing" element={<PosPage />} />
          <Route path="/tables/payment" element={<PaymentPage />} />
          <Route path="/tables/split-bill" element={<SplitBillPage />} />
          <Route path="/takeaway" element={<TakeawayPage />} />
          <Route path="/takeaway/billing" element={<PosPage />} />
          <Route path="/takeaway/payment" element={<PaymentPage />} />
          <Route path="/mess" element={<MessPage />} />
          <Route path="/mess/meal-sessions" element={<MealSessionsPage />} />
          <Route path="/mess/payments" element={<MessPaymentsPage />} />
          <Route path="/mess/scan" element={<MessScanPage />} />
          <Route path="/mess/meal-tokens-today" element={<MessTokensTodayPage />} />
          <Route path="/masters" element={<MastersPage />} />
          <Route path="/inventory" element={<InventoryPage />} />
          <Route path="/expenses" element={<InventoryPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/reports/invoice/:invoiceId" element={<InvoiceDetailPage />} />
          <Route path="/settings/*" element={<SettingsPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthBootstrap>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </AuthBootstrap>
    </QueryClientProvider>
  )
}
