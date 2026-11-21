import { Link } from 'react-router'
import { buttonVariants } from '@/components/ui/button'
import { useMyBusiness } from '@/hooks/useMyBusiness'

export function OwnerDashboardPage() {
  const { data: business, isPending, isError } = useMyBusiness()

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-4xl">Overview</h1>
        <p className="text-muted-foreground">Your business at a glance.</p>
      </div>

      {isPending && <p className="text-muted-foreground">Loading…</p>}

      {isError && !business && (
        <p
          role="alert"
          className="rounded-lg bg-destructive/10 px-3.5 py-3 text-sm font-medium text-destructive"
        >
          We couldn't load your business. Please refresh the page.
        </p>
      )}

      {business && (
        <article className="flex flex-col gap-6 rounded-[20px] border border-border bg-card p-6 sm:p-8">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div className="flex flex-col gap-1">
              <span className="text-sm font-semibold text-muted-foreground">
                {business.category.name}
              </span>
              <h2 className="text-2xl">{business.name}</h2>
              <p className="text-muted-foreground">
                {business.street}, {business.postalCode} {business.city}
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Link
                to="/owner/business"
                className={buttonVariants({ variant: 'outline' })}
              >
                Edit details
              </Link>
              <Link
                to={`/businesses/${business.slug}`}
                className={buttonVariants()}
              >
                View public page
              </Link>
            </div>
          </div>

          {business.description && (
            <p className="leading-relaxed whitespace-pre-line">
              {business.description}
            </p>
          )}

          <dl className="grid gap-4 border-t border-border pt-6 sm:grid-cols-2 lg:grid-cols-4">
            <div className="flex flex-col gap-1">
              <dt className="text-sm text-muted-foreground">Phone</dt>
              <dd className="font-medium">{business.phone}</dd>
            </div>
            <div className="flex flex-col gap-1">
              <dt className="text-sm text-muted-foreground">Contact email</dt>
              <dd className="font-medium break-all">
                {business.email ?? 'Not set'}
              </dd>
            </div>
            <div className="flex flex-col gap-1">
              <dt className="text-sm text-muted-foreground">Booking slots</dt>
              <dd className="font-medium">
                Every {business.slotIntervalMinutes} min
              </dd>
            </div>
            <div className="flex flex-col gap-1">
              <dt className="text-sm text-muted-foreground">
                Free cancellation
              </dt>
              <dd className="font-medium">
                Up to {business.freeCancellationHours} h before
              </dd>
            </div>
          </dl>
        </article>
      )}
    </div>
  )
}
