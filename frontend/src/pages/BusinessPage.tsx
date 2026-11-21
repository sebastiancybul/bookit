import { Link, Navigate, useParams } from 'react-router'
import { ArrowLeftIcon } from 'lucide-react'
import { ApiError } from '@/api/client'
import { BusinessMark } from '@/components/elements/BusinessMark'
import { buttonVariants } from '@/components/ui/button'
import { useBusiness } from '@/hooks/useBusiness'

export function BusinessPage() {
  const { slug = '' } = useParams()
  const { data: business, isPending, error } = useBusiness(slug)

  if (business && business.slug !== slug) {
    return <Navigate to={`/businesses/${business.slug}`} replace />
  }

  return (
    <div className="mx-auto flex max-w-300 flex-col gap-10 px-4 pt-8 pb-24 sm:px-8">
      <Link
        to="/"
        className="flex items-center gap-1.5 self-start text-sm font-medium text-muted-foreground hover:text-foreground"
      >
        <ArrowLeftIcon aria-hidden="true" className="size-4" />
        All businesses
      </Link>

      {isPending && <p className="text-muted-foreground">Loading…</p>}

      {error && !business && (
        <div className="flex flex-col items-start gap-4">
          <h1 className="text-3xl">
            {error instanceof ApiError && error.status === 404
              ? 'Business not found'
              : 'Something went wrong'}
          </h1>
          <p className="text-muted-foreground">
            {error instanceof ApiError && error.status === 404
              ? 'This business does not exist or is no longer listed.'
              : "We couldn't load this business. Please try again."}
          </p>
          <Link to="/" className={buttonVariants()}>
            Browse businesses
          </Link>
        </div>
      )}

      {business && (
        <>
          <section className="flex flex-wrap items-center gap-7">
            <BusinessMark
              name={business.name}
              publicId={business.publicId}
              size="lg"
            />
            <div className="flex min-w-0 flex-[999_1_26rem] flex-col gap-2.5">
              <div className="flex flex-wrap items-center gap-2.5">
                <h1 className="text-4xl leading-[1.05] sm:text-[44px]">
                  {business.name}
                </h1>
                <span className="rounded-full bg-secondary px-3 py-1 text-[13px] font-semibold text-secondary-foreground">
                  {business.category.name}
                </span>
              </div>
              <p className="text-[15px] text-muted-foreground">
                {business.street}, {business.postalCode} {business.city} ·{' '}
                <a
                  href={`tel:${business.phone}`}
                  className="font-medium text-foreground hover:underline"
                >
                  {business.phone}
                </a>
                {business.email && (
                  <>
                    {' · '}
                    <a
                      href={`mailto:${business.email}`}
                      className="font-medium text-foreground hover:underline"
                    >
                      {business.email}
                    </a>
                  </>
                )}
              </p>
              {business.description && (
                <p className="max-w-160 leading-relaxed whitespace-pre-line">
                  {business.description}
                </p>
              )}
            </div>
          </section>

          <aside
            aria-labelledby="cancellation-heading"
            className="flex max-w-120 flex-col gap-3 rounded-[20px] bg-muted p-6"
          >
            <h2 id="cancellation-heading" className="text-base font-bold">
              Cancellation policy
            </h2>
            <p className="text-sm leading-relaxed text-secondary-foreground">
              {business.freeCancellationHours > 0 ? (
                <>
                  Free cancellation up to{' '}
                  <strong>{business.freeCancellationHours} hours</strong> before
                  your visit. After that, please call the business.
                </>
              ) : (
                'You can cancel any time before your visit.'
              )}
            </p>
          </aside>
        </>
      )}
    </div>
  )
}
