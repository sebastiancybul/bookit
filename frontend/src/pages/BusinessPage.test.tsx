import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/client'
import { BusinessPage } from '@/pages/BusinessPage'
import { getBusiness, type Business } from '@/services/businesses'
import { listBusinessEmployees } from '@/services/employees'
import { listBusinessOfferings } from '@/services/offerings'

vi.mock('@/services/businesses', () => ({
  getBusiness: vi.fn(),
}))

vi.mock('@/services/employees', () => ({
  listBusinessEmployees: vi.fn(),
}))

vi.mock('@/services/offerings', () => ({
  listBusinessOfferings: vi.fn(),
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
    vi.mocked(listBusinessOfferings).mockReset()
    vi.mocked(listBusinessOfferings).mockResolvedValue([])
    vi.mocked(listBusinessEmployees).mockReset()
    vi.mocked(listBusinessEmployees).mockResolvedValue([])
  })

  it('shows the team with the services each person performs', async () => {
    const haircut = {
      id: '0193a1b2-0000-7000-8000-000000000001',
      name: 'Haircut',
      description: null,
      priceAmount: 8000,
      currency: 'PLN',
      durationMinutes: 45,
    }
    const beard = {
      ...haircut,
      id: '0193a1b2-0000-7000-8000-000000000002',
      name: 'Beard trim',
    }
    vi.mocked(getBusiness).mockResolvedValue(business)
    vi.mocked(listBusinessOfferings).mockResolvedValue([haircut, beard])
    vi.mocked(listBusinessEmployees).mockResolvedValue([
      {
        id: '0193a1b2-0000-7000-8000-0000000000a1',
        displayName: 'Anna Kowalska',
        position: 'Senior barber',
        offeringIds: [haircut.id, beard.id],
      },
      {
        id: '0193a1b2-0000-7000-8000-0000000000b2',
        displayName: 'Bartek Wiśniewski',
        position: null,
        offeringIds: [beard.id],
      },
    ])
    renderBusinessPage('/businesses/k3f9x2ab-barber-shop-krakow')

    const team = await screen.findByRole('region', { name: 'Team' })
    expect(within(team).getByText('2 specialists')).toBeInTheDocument()
    const anna = within(team).getByText('Anna Kowalska').closest('li')!
    expect(within(anna).getByText('Senior barber')).toBeInTheDocument()
    expect(within(anna).getByText('All services')).toBeInTheDocument()
    const bartek = within(team).getByText('Bartek Wiśniewski').closest('li')!
    expect(within(bartek).getByText('Beard trim')).toBeInTheDocument()
  })

  it('hides the team section when the business has no employees', async () => {
    vi.mocked(getBusiness).mockResolvedValue(business)
    renderBusinessPage('/businesses/k3f9x2ab-barber-shop-krakow')

    await screen.findByRole('heading', { name: 'Barber Shop' })
    await waitFor(() => expect(listBusinessEmployees).toHaveBeenCalled())
    expect(
      screen.queryByRole('region', { name: 'Team' }),
    ).not.toBeInTheDocument()
  })

  it('lists the services of the business', async () => {
    vi.mocked(getBusiness).mockResolvedValue(business)
    vi.mocked(listBusinessOfferings).mockResolvedValue([
      {
        id: '0193a1b2-0000-7000-8000-000000000001',
        name: 'Beard trim',
        description: 'Shape and line-up',
        priceAmount: 5000,
        currency: 'PLN',
        durationMinutes: 30,
      },
    ])
    renderBusinessPage('/businesses/k3f9x2ab-barber-shop-krakow')

    const services = await screen.findByRole('region', { name: 'Services' })
    expect(await within(services).findByText('Beard trim')).toBeInTheDocument()
    expect(
      within(services).getByText('30 min · Shape and line-up'),
    ).toBeInTheDocument()
    expect(within(services).getByText(/50\szł/)).toBeInTheDocument()
    expect(listBusinessOfferings).toHaveBeenCalledWith(
      'k3f9x2ab-barber-shop-krakow',
    )
  })

  it('shows a message when the business has no services', async () => {
    vi.mocked(getBusiness).mockResolvedValue(business)
    renderBusinessPage('/businesses/k3f9x2ab-barber-shop-krakow')

    expect(
      await screen.findByText("This business hasn't added any services yet."),
    ).toBeInTheDocument()
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
