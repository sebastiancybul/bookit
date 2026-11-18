import { Navigate, Outlet, useLocation } from 'react-router'
import { useMe } from '@/hooks/useMe'

type Role = 'ROLE_CLIENT' | 'ROLE_EMPLOYEE' | 'ROLE_OWNER' | 'ROLE_ADMIN'

type ProtectedRouteProps = {
  requiredRole?: Role
}

export function ProtectedRoute({ requiredRole }: ProtectedRouteProps) {
  const { data: user, isPending, isError, error } = useMe()
  const location = useLocation()

  if (isPending) {
    return null
  }

  if (isError) {
    throw error
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (requiredRole && !user.roles.includes(requiredRole)) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
