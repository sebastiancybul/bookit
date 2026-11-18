import { Link, NavLink, Outlet } from 'react-router'
import { cn } from 'cn'
import { ArrowLeftIcon } from 'lucide-react'
import { Logo } from '@/components/elements/Logo'
import { useLogout } from '@/hooks/useLogout'

const menu = [{ to: '/owner', label: 'Overview', end: true }]

export function PanelLayout() {
  const logoutMutation = useLogout()

  return (
    <div className="flex min-h-svh flex-col bg-muted md:flex-row">
      <aside className="flex flex-col gap-1 border-b border-border bg-background px-3.5 py-5 md:w-62 md:shrink-0 md:border-r md:border-b-0">
        <div className="px-2.5 pt-1 pb-5">
          <Logo />
        </div>

        <nav aria-label="Owner panel" className="flex flex-col gap-1">
          {menu.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                cn(
                  'rounded-[10px] px-3 py-2.5 text-sm',
                  isActive
                    ? 'bg-secondary font-bold text-foreground'
                    : 'font-medium text-secondary-foreground hover:bg-secondary',
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="mt-auto flex flex-col gap-1">
          <Link
            to="/"
            className="flex items-center gap-2 rounded-[10px] px-3 py-2.5 text-sm font-medium text-muted-foreground hover:bg-secondary"
          >
            <ArrowLeftIcon aria-hidden="true" className="size-4" />
            Back to client view
          </Link>
          <button
            type="button"
            disabled={logoutMutation.isPending}
            onClick={() => logoutMutation.mutate()}
            className="rounded-[10px] px-3 py-2.5 text-left text-sm font-medium text-muted-foreground hover:bg-secondary disabled:opacity-50"
          >
            Log out
          </button>
        </div>
      </aside>

      <main className="min-w-0 flex-1 px-4 py-10 sm:px-10">
        <Outlet />
      </main>
    </div>
  )
}
