import { Link } from 'react-router'
import { BusinessMark } from '@/components/elements/BusinessMark'
import type { BusinessSummary } from '@/services/businesses'

type BusinessCardProps = {
  business: BusinessSummary
}

export function BusinessCard({ business }: BusinessCardProps) {
  return (
    <Link
      to={`/businesses/${business.slug}`}
      className="flex flex-col gap-5 rounded-[20px] border border-border bg-card p-6 transition-colors outline-none hover:border-foreground/20 focus-visible:ring-3 focus-visible:ring-ring/50"
    >
      <span className="flex items-center gap-3.5">
        <BusinessMark name={business.name} publicId={business.publicId} />
        <span className="flex min-w-0 flex-col gap-0.5">
          <span className="truncate text-[17px] font-bold tracking-[-0.02em]">
            {business.name}
          </span>
          <span className="truncate text-sm text-muted-foreground">
            {business.street}, {business.city}
          </span>
        </span>
      </span>
      <span className="flex justify-end border-t border-border pt-4">
        <span className="rounded-full bg-secondary px-2.5 py-1 text-xs font-semibold text-secondary-foreground">
          {business.category.name}
        </span>
      </span>
    </Link>
  )
}
