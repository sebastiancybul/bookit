import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { HomePage } from '@/pages/HomePage'
import {
  listCategories,
  searchBusinesses,
  type BusinessPage,
} from '@/services/businesses'

vi.mock('@/services/businesses', () => ({
  listCategories: vi.fn(),
  searchBusinesses: vi.fn(),
}))

const onePage: BusinessPage = {
  content: [
    {
      publicId: 'k3f9x2ab',
      slug: 'k3f9x2ab-studio-urody-lodz',
      name: 'Studio Urody',
      category: {
        code: 'BEAUTY',
        name: 'Beauty',
      },
      city: 'Łódź',
      street: 'Piotrkowska 10',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
}

function renderHome(url: string) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })
  const router = createMemoryRouter([{ path: '/', element: <HomePage /> }], {
    initialEntries: [url],
  })
  render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return router
}

function searchParamsOf(router: ReturnType<typeof createMemoryRouter>) {
  return new URLSearchParams(router.state.location.search)
}

describe('HomePage', () => {
  beforeEach(() => {
    vi.mocked(listCategories).mockReset()
    vi.mocked(listCategories).mockResolvedValue([
      { code: 'BARBER', name: 'Barber' },
      { code: 'BEAUTY', name: 'Beauty' },
    ])
    vi.mocked(searchBusinesses).mockReset()
    vi.mocked(searchBusinesses).mockResolvedValue(onePage)
  })

  it('searches with the filters from the URL', async () => {
    renderHome('/?city=Łódź&category=BEAUTY&page=2')

    expect(
      await screen.findByRole('heading', { name: 'Businesses in Łódź' }),
    ).toBeInTheDocument()
    expect(await screen.findByText('Studio Urody')).toBeInTheDocument()
    expect(searchBusinesses).toHaveBeenCalledWith({
      city: 'Łódź',
      category: 'BEAUTY',
      page: 1,
    })
  })

  it('puts a new search in the URL and goes back to the first page', async () => {
    vi.mocked(searchBusinesses).mockImplementation(async ({ page = 0 }) => ({
      ...onePage,
      page,
      totalElements: 90,
      totalPages: 5,
    }))
    const user = userEvent.setup()
    const router = renderHome('/?page=3')
    await screen.findByText('Page 3 of 5')

    await user.type(screen.getByLabelText('City'), ' Kraków ')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    await waitFor(() =>
      expect(searchParamsOf(router).get('city')).toBe('Kraków'),
    )
    expect(searchParamsOf(router).has('page')).toBe(false)
    expect(searchBusinesses).toHaveBeenLastCalledWith({
      city: 'Kraków',
      category: undefined,
      page: 0,
    })
  })

  it('filters by category right after a chip is clicked', async () => {
    const user = userEvent.setup()
    const router = renderHome('/')

    await user.click(await screen.findByRole('button', { name: 'Barber' }))

    await waitFor(() =>
      expect(searchParamsOf(router).get('category')).toBe('BARBER'),
    )
    expect(screen.getByRole('button', { name: 'Barber' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
  })

  it('marks a lowercase category from the URL as selected', async () => {
    renderHome('/?category=barber')

    expect(
      await screen.findByRole('button', { name: 'Barber' }),
    ).toHaveAttribute('aria-pressed', 'true')
    expect(searchBusinesses).toHaveBeenCalledWith({
      city: undefined,
      category: 'BARBER',
      page: 0,
    })
  })

  it('moves a page number past the end to the last page', async () => {
    vi.mocked(searchBusinesses).mockImplementation(async ({ page = 0 }) => ({
      ...onePage,
      page,
      totalElements: 25,
      totalPages: 2,
    }))
    const router = renderHome('/?city=Łódź&page=9')

    await waitFor(() => expect(searchParamsOf(router).get('page')).toBe('2'))
    expect(searchParamsOf(router).get('city')).toBe('Łódź')
    expect(
      screen.queryByText('No businesses match your search.'),
    ).not.toBeInTheDocument()
  })

  it('tells the user when nothing matches', async () => {
    vi.mocked(searchBusinesses).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })
    renderHome('/?city=Nowhere')

    expect(
      await screen.findByText('No businesses match your search.'),
    ).toBeInTheDocument()
  })
})
