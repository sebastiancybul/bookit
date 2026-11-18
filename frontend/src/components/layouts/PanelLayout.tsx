import { Link, NavLink, Outlet } from 'react-router'
import { cn } from 'cn'
import { Logo } from '@/components/elements/Logo'

const menu = [{ to: '/owner', label: 'Overview', end: true }]

export function PanelLayout() {
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

        <Link
          to="/"
          className="mt-auto rounded-[10px] px-3 py-2.5 text-sm font-medium text-muted-foreground hover:bg-secondary"
        >
          ← Back to client view
        </Link>
      </aside>

      <main className="min-w-0 flex-1 px-4 py-10 sm:px-10">
        <Outlet />
      </main>
    </div>
  )
}
