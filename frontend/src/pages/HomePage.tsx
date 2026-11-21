import { useRef } from 'react'
import { Navigate, useSearchParams } from 'react-router'
import { cn } from 'cn'
import { BusinessCard } from '@/components/elements/BusinessCard'
import { Pagination } from '@/components/elements/Pagination'
import { SearchBar } from '@/components/elements/SearchBar'
import { useBusinessSearch } from '@/hooks/useBusinessSearch'
import type { BusinessSearch } from '@/services/businesses'

const MAX_PAGE_INDEX = 10_000
const MAX_CITY_LENGTH = 80
const MAX_CATEGORY_LENGTH = 30

function pageIndexFrom(value: string | null): number {
  const pageNumber = Number(value)
  if (!Number.isInteger(pageNumber) || pageNumber < 1) {
    return 0
  }
  return Math.min(pageNumber - 1, MAX_PAGE_INDEX)
}

export function HomePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const resultsRef = useRef<HTMLElement>(null)

  const city = (searchParams.get('city') ?? '').trim().slice(0, MAX_CITY_LENGTH)
  const category = (searchParams.get('category') ?? '')
    .trim()
    .slice(0, MAX_CATEGORY_LENGTH)
    .toUpperCase()
  const page = pageIndexFrom(searchParams.get('page'))

  const { data, isPending, isError, isPlaceholderData } = useBusinessSearch({
    city: city || undefined,
    category: category || undefined,
    page,
  })

  const search = (next: BusinessSearch) => {
    const params = new URLSearchParams()
    if (next.city) {
      params.set('city', next.city)
    }
    if (next.category) {
      params.set('category', next.category)
    }
    setSearchParams(params)
  }

  const changePage = (nextPage: number) => {
    const params = new URLSearchParams(searchParams)
    if (nextPage === 0) {
      params.delete('page')
    } else {
      params.set('page', String(nextPage + 1))
    }
    setSearchParams(params)
    resultsRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  if (
    data &&
    !isPlaceholderData &&
    data.totalPages > 0 &&
    page >= data.totalPages
  ) {
    const params = new URLSearchParams(searchParams)
    if (data.totalPages === 1) {
      params.delete('page')
    } else {
      params.set('page', String(data.totalPages))
    }
    return <Navigate to={{ search: params.toString() }} replace />
  }

  return (
    <>
      <section className="mx-auto flex max-w-300 flex-col items-center gap-6 px-4 pt-22 pb-16 text-center sm:px-8">
        <h1 className="max-w-205 text-5xl leading-[1.02] sm:text-7xl">
          Book the people you trust.
        </h1>
        <p className="max-w-140 text-lg leading-relaxed text-muted-foreground">
          Find barbers, stylists, therapists and physios near you. Pick the
          person, pick the time, done.
        </p>
        <SearchBar
          key={`${city}|${category}`}
          city={city}
          category={category}
          onSearch={search}
        />
      </section>

      <section
        ref={resultsRef}
        aria-labelledby="results-heading"
        className="scroll-mt-4 border-t border-border bg-muted"
      >
        <div className="mx-auto flex max-w-300 flex-col gap-6 px-4 pt-14 pb-22 sm:px-8">
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div className="flex flex-col gap-1">
              <h2 id="results-heading" className="text-[28px]">
                {city ? `Businesses in ${city}` : 'All businesses'}
              </h2>
              <span className="text-sm text-muted-foreground">
                Sorted by name
              </span>
            </div>
            {data && data.totalPages > 0 && (
              <span className="text-sm text-muted-foreground">
                Page {data.page + 1} of {data.totalPages}
              </span>
            )}
          </div>

          {isPending && <p className="text-muted-foreground">Loading…</p>}

          {isError && !data && (
            <p
              role="alert"
              className="rounded-lg bg-destructive/10 px-3.5 py-3 text-sm font-medium text-destructive"
            >
              We couldn't load businesses. Please try again.
            </p>
          )}

          {data && data.content.length === 0 && (
            <p className="rounded-[20px] border border-border bg-card px-6 py-10 text-center text-muted-foreground">
              No businesses match your search.
            </p>
          )}

          {data && data.content.length > 0 && (
            <ul
              className={cn(
                'grid grid-cols-[repeat(auto-fill,minmax(min(100%,20rem),1fr))] gap-4 transition-opacity',
                isPlaceholderData && 'opacity-60',
              )}
            >
              {data.content.map((business) => (
                <li key={business.publicId} className="flex flex-col">
                  <BusinessCard business={business} />
                </li>
              ))}
            </ul>
          )}

          {data && (
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              disabled={isPlaceholderData}
              onPageChange={changePage}
            />
          )}
        </div>
      </section>
    </>
  )
}
