import { createBrowserRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { PanelLayout } from '@/components/layouts/PanelLayout'
import { PublicLayout } from '@/components/layouts/PublicLayout'
import { ErrorPage } from '@/pages/ErrorPage'
import { HomePage } from '@/pages/HomePage'
import { LoginPage } from '@/pages/LoginPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { OwnerDashboardPage } from '@/pages/owner/OwnerDashboardPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { PublicRoute } from '@/routes/PublicRoute'

const router = createBrowserRouter([
  {
    ErrorBoundary: ErrorPage,
    children: [
      {
        Component: PublicLayout,
        children: [
          { index: true, Component: HomePage },
          {
            Component: PublicRoute,
            children: [
              { path: 'login', Component: LoginPage },
              { path: 'register', Component: RegisterPage },
            ],
          },
          { path: '*', Component: NotFoundPage },
        ],
      },
      {
        element: <ProtectedRoute requiredRole="ROLE_OWNER" />,
        children: [
          {
            path: 'owner',
            Component: PanelLayout,
            children: [{ index: true, Component: OwnerDashboardPage }],
          },
        ],
      },
    ],
  },
])

export function AppRoutes() {
  return <RouterProvider router={router} />
}
