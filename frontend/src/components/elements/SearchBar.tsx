import { useState, type FormEvent } from 'react'
import { cn } from 'cn'
import { useCategories } from '@/hooks/useCategories'
import type { BusinessSearch } from '@/services/businesses'

type SearchBarProps = {
  city: string
  category: string
  onSearch: (search: BusinessSearch) => void
}

export function SearchBar({ city, category, onSearch }: SearchBarProps) {
  const categoriesQuery = useCategories()
  const [cityInput, setCityInput] = useState(city)
  const [categoryInput, setCategoryInput] = useState(category)

  const search = (nextCategory: string) => {
    onSearch({
      city: cityInput.trim() || undefined,
      category: nextCategory || undefined,
    })
  }

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    search(categoryInput)
  }

  const pickCategory = (code: string) => {
    setCategoryInput(code)
    search(code)
  }

  const chips = [{ code: '', name: 'All' }, ...(categoriesQuery.data ?? [])]

  return (
    <div className="flex w-full max-w-180 flex-col items-center gap-5">
      <form
        role="search"
        onSubmit={submit}
        className="flex w-full flex-col gap-1 rounded-3xl border border-border bg-background p-1.5 shadow-[0_1px_2px_rgba(0,0,0,0.04),0_12px_32px_-16px_rgba(0,0,0,0.12)] sm:flex-row sm:items-center sm:rounded-full"
      >
        <div className="flex flex-1 flex-col items-start px-5 py-1.5">
          <label htmlFor="search-city" className="text-xs font-semibold">
            City
          </label>
          <input
            id="search-city"
            type="search"
            autoComplete="address-level2"
            maxLength={80}
            placeholder="Any city"
            value={cityInput}
            onChange={(event) => setCityInput(event.target.value)}
            className="w-full bg-transparent py-0.5 text-[15px] text-foreground outline-none placeholder:text-muted-foreground"
          />
        </div>

        <span
          aria-hidden="true"
          className="hidden h-8 w-px bg-border sm:block"
        />

        <div className="flex flex-1 flex-col items-start px-5 py-1.5">
          <label htmlFor="search-category" className="text-xs font-semibold">
            Category
          </label>
          <select
            id="search-category"
            value={categoryInput}
            onChange={(event) => setCategoryInput(event.target.value)}
            className="w-full bg-transparent py-0.5 text-[15px] text-foreground outline-none"
          >
            <option value="">All categories</option>
            {categoriesQuery.data?.map((option) => (
              <option key={option.code} value={option.code}>
                {option.name}
              </option>
            ))}
          </select>
        </div>

        <button
          type="submit"
          className="h-13 rounded-full bg-primary px-6.5 text-[15px] font-semibold text-primary-foreground hover:bg-primary/85 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
        >
          Search
        </button>
      </form>

      <div className="flex flex-wrap justify-center gap-2">
        {chips.map((chip) => (
          <button
            key={chip.code}
            type="button"
            aria-pressed={categoryInput === chip.code}
            onClick={() => pickCategory(chip.code)}
            className={cn(
              'rounded-full border px-4 py-2 text-sm font-medium focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none',
              categoryInput === chip.code
                ? 'border-primary bg-primary text-primary-foreground'
                : 'border-border text-foreground hover:bg-secondary',
            )}
          >
            {chip.name}
          </button>
        ))}
      </div>
    </div>
  )
}
