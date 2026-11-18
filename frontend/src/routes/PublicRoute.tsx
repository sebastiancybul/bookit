import { Navigate, Outlet, useLocation, type Location } from 'react-router'
import { useMe } from '@/hooks/useMe'

type RedirectState = {
  from?: Location
}

export function PublicRoute() {
  const { data: user, isPending, isError, error } = useMe()
  const location = useLocation()

  if (isPending) {
    return null
  }

  if (isError) {
    throw error
  }

  if (user) {
    const state = location.state as RedirectState | null
    return <Navigate to={state?.from ?? '/'} replace />
  }

  return <Outlet />
}
