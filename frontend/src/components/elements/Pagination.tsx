import { cn } from 'cn'

type PageItem = number | 'gap'

function pageItems(page: number, totalPages: number): PageItem[] {
  const visible = new Set(
    [0, page - 1, page, page + 1, totalPages - 1].filter(
      (index) => index >= 0 && index < totalPages,
    ),
  )
  const sorted = [...visible].sort((a, b) => a - b)
  const items: PageItem[] = []
  sorted.forEach((index, position) => {
    if (position > 0 && index - sorted[position - 1] > 1) {
      items.push('gap')
    }
    items.push(index)
  })
  return items
}

const pillClassName =
  'h-10 rounded-full border text-sm focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none disabled:pointer-events-none disabled:opacity-40'

type PaginationProps = {
  page: number
  totalPages: number
  disabled?: boolean
  onPageChange: (page: number) => void
}

export function Pagination({
  page,
  totalPages,
  disabled = false,
  onPageChange,
}: PaginationProps) {
  if (totalPages <= 1) {
    return null
  }

  return (
    <nav
      aria-label="Pagination"
      className="flex flex-wrap justify-center gap-2"
    >
      <button
        type="button"
        disabled={disabled || page === 0}
        onClick={() => onPageChange(page - 1)}
        className={cn(
          pillClassName,
          'border-border px-4 font-medium hover:bg-secondary',
        )}
      >
        Previous
      </button>

      {pageItems(page, totalPages).map((item, position) =>
        item === 'gap' ? (
          <span
            key={`gap-${position}`}
            aria-hidden="true"
            className="flex h-10 items-center px-1 text-muted-foreground"
          >
            …
          </span>
        ) : (
          <button
            key={item}
            type="button"
            aria-label={`Page ${item + 1}`}
            aria-current={item === page ? 'page' : undefined}
            disabled={disabled}
            onClick={() => onPageChange(item)}
            className={cn(
              pillClassName,
              'w-10',
              item === page
                ? 'border-primary bg-primary font-semibold text-primary-foreground'
                : 'border-border hover:bg-secondary',
            )}
          >
            {item + 1}
          </button>
        ),
      )}

      <button
        type="button"
        disabled={disabled || page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
        className={cn(
          pillClassName,
          'border-border px-4 font-medium hover:bg-secondary',
        )}
      >
        Next
      </button>
    </nav>
  )
}
