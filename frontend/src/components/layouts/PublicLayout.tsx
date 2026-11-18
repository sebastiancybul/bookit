import { Link, Outlet } from 'react-router'
import { buttonVariants } from '@/components/ui/button'
import { Logo } from '@/components/elements/Logo'

export function PublicLayout() {
  return (
    <div className="flex min-h-svh flex-col">
      <header className="border-b border-border bg-background">
        <div className="mx-auto flex max-w-300 items-center justify-between gap-4 px-4 py-4 sm:px-8">
          <Logo />
          <nav aria-label="Account" className="flex items-center gap-2">
            <Link to="/login" className={buttonVariants({ variant: 'ghost' })}>
              Log in
            </Link>
            <Link to="/register" className={buttonVariants()}>
              Sign up
            </Link>
          </nav>
        </div>
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="border-t border-border">
        <div className="mx-auto max-w-300 px-4 py-7 text-sm text-muted-foreground sm:px-8">
          © bookit
        </div>
      </footer>
    </div>
  )
}
