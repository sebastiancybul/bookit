import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/client'
import { BusinessPage } from '@/pages/BusinessPage'
import { getBusiness, type Business } from '@/services/businesses'

vi.mock('@/services/businesses', () => ({
  getBusiness: vi.fn(),
}))

const business: Business = {
  publicId: 'k3f9x2ab',
  slug: 'k3f9x2ab-barber-shop-krakow',
  name: 'Barber Shop',
  description: 'Classic cuts and beard trims.',
  category: {
    code: 'BARBER',
    name: 'Barber',
  },
  phone: '123456789',
  email: 'kontakt@barbershop.pl',
  street: 'Floriańska 1',
  city: 'Kraków',
  postalCode: '31-019',
  timezone: 'Europe/Warsaw',
  freeCancellationHours: 24,
  slotIntervalMinutes: 15,
}

function renderBusinessPage(url: string) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })
  const router = createMemoryRouter(
    [{ path: '/businesses/:slug', element: <BusinessPage /> }],
    { initialEntries: [url] },
  )
  render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return router
}

describe('BusinessPage', () => {
  beforeEach(() => {
    vi.mocked(getBusiness).mockReset()
  })

  it('shows the business with contact links and cancellation policy', async () => {
    vi.mocked(getBusiness).mockResolvedValue(business)
    renderBusinessPage('/businesses/k3f9x2ab-barber-shop-krakow')

    expect(
      await screen.findByRole('heading', { name: 'Barber Shop' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '123456789' })).toHaveAttribute(
      'href',
      'tel:123456789',
    )
    expect(
      screen.getByRole('link', { name: 'kontakt@barbershop.pl' }),
    ).toHaveAttribute('href', 'mailto:kontakt@barbershop.pl')
    expect(screen.getByText('24 hours')).toBeInTheDocument()
  })

  it('replaces an outdated slug in the URL without fetching twice', async () => {
    vi.mocked(getBusiness).mockResolvedValue(business)
    const router = renderBusinessPage('/businesses/k3f9x2ab-old-name')

    await waitFor(() =>
      expect(router.state.location.pathname).toBe(
        '/businesses/k3f9x2ab-barber-shop-krakow',
      ),
    )
    expect(
      await screen.findByRole('heading', { name: 'Barber Shop' }),
    ).toBeInTheDocument()
    expect(getBusiness).toHaveBeenCalledTimes(1)
  })

  it('shows a not found message for an unknown business', async () => {
    vi.mocked(getBusiness).mockRejectedValue(
      new ApiError({
        status: 404,
        detail: 'Business with id zzzzzzzz not found',
      }),
    )
    renderBusinessPage('/businesses/zzzzzzzz')

    expect(
      await screen.findByRole('heading', { name: 'Business not found' }),
    ).toBeInTheDocument()
  })
})
