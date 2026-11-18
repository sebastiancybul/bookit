import { Link } from 'react-router'
import { buttonVariants } from '@/components/ui/button'

export function NotFoundPage() {
  return (
    <section className="mx-auto flex max-w-300 flex-col items-center gap-4 px-4 py-22 text-center sm:px-8">
      <h1 className="text-4xl">Page not found</h1>
      <p className="text-muted-foreground">
        The page you are looking for does not exist.
      </p>
      <Link to="/" className={buttonVariants({ size: 'lg' })}>
        Back to home
      </Link>
    </section>
  )
}
