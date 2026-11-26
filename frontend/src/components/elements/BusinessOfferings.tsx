import { Link } from 'react-router'
import { buttonVariants } from '@/components/ui/button'
import { useBusinessOfferings } from '@/hooks/useBusinessOfferings'
import { formatDuration, formatPrice } from '@/lib/format'

type BusinessOfferingsProps = {
  slug: string
}

export function BusinessOfferings({ slug }: BusinessOfferingsProps) {
  const { data: offerings, isPending, isError } = useBusinessOfferings(slug)

  return (
    <section
      aria-labelledby="services-heading"
      className="flex min-w-0 flex-[999_1_35rem] flex-col gap-4"
    >
      <h2 id="services-heading" className="text-2xl">
        Services
      </h2>

      {isPending && <p className="text-muted-foreground">Loading…</p>}

      {isError && !offerings && (
        <p role="alert" className="text-muted-foreground">
          We couldn't load the services. Please refresh the page.
        </p>
      )}

      {offerings && offerings.length === 0 && (
        <p className="text-muted-foreground">
          This business hasn't added any services yet.
        </p>
      )}

      {offerings && offerings.length > 0 && (
        <ul className="overflow-hidden rounded-[20px] border border-border">
          {offerings.map((offering) => (
            <li
              key={offering.id}
              className="flex flex-wrap items-center justify-between gap-4 border-b border-border px-6 py-5 last:border-b-0"
            >
              <div className="flex min-w-0 flex-[1_1_16rem] flex-col gap-1">
                <span className="text-base font-semibold">{offering.name}</span>
                <span className="text-sm text-secondary-foreground">
                  {formatDuration(offering.durationMinutes)}
                  {offering.description && ` · ${offering.description}`}
                </span>
              </div>
              <div className="flex items-center gap-4">
                <span className="text-base font-bold">
                  {formatPrice(offering.priceAmount, offering.currency)}
                </span>
                <Link
                  to={`/businesses/${slug}/book?offeringId=${offering.id}`}
                  aria-label={`Book ${offering.name}`}
                  className={buttonVariants({ size: 'sm' })}
                >
                  Book
                </Link>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
