import { isRouteErrorResponse, useRouteError } from 'react-router'
import { buttonVariants } from '@/components/ui/button'

export function ErrorPage() {
  const error = useRouteError()
  const title = isRouteErrorResponse(error)
    ? `${error.status} ${error.statusText}`
    : 'Something went wrong'

  return (
    <main className="flex min-h-svh flex-col items-center justify-center gap-4 px-4 text-center">
      <h1 className="text-4xl">{title}</h1>
      <p className="text-muted-foreground">
        Try again, or go back to the home page.
      </p>
      <a href="/" className={buttonVariants({ size: 'lg' })}>
        Back to home
      </a>
    </main>
  )
}
