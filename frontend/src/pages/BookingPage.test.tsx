import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { BookingPage } from '@/pages/BookingPage'
import { BusinessPage } from '@/pages/BusinessPage'
import { getAvailableSlots } from '@/services/availability'
import { getBusiness, type Business } from '@/services/businesses'
import {
  listBusinessEmployees,
  type PublicEmployee,
} from '@/services/employees'
import {
  listBusinessOfferings,
  type PublicOffering,
} from '@/services/offerings'

vi.mock('@/services/businesses', () => ({
  getBusiness: vi.fn(),
}))

vi.mock('@/services/employees', () => ({
  listBusinessEmployees: vi.fn(),
}))

vi.mock('@/services/offerings', () => ({
  listBusinessOfferings: vi.fn(),
}))

vi.mock('@/services/availability', () => ({
  getAvailableSlots: vi.fn(),
}))

const SLUG = 'k3f9x2ab-barber-shop-krakow'

const business: Business = {
  publicId: 'k3f9x2ab',
  slug: SLUG,
  name: 'Barber Shop',
  description: null,
  category: {
    code: 'BARBER',
    name: 'Barber',
  },
  phone: '123456789',
  email: null,
  street: 'Floriańska 1',
  city: 'Kraków',
  postalCode: '31-019',
  timezone: 'Europe/Warsaw',
  freeCancellationHours: 24,
  slotIntervalMinutes: 15,
}

const haircut: PublicOffering = {
  id: 'off-haircut',
  name: "Men's haircut",
  description: null,
  priceAmount: 8000,
  currency: 'PLN',
  durationMinutes: 45,
}

const beard: PublicOffering = {
  id: 'off-beard',
  name: 'Beard trim',
  description: null,
  priceAmount: 4000,
  currency: 'PLN',
  durationMinutes: 30,
}

const anna: PublicEmployee = {
  id: 'emp-anna',
  displayName: 'Anna K.',
  position: 'Senior barber',
  offeringIds: [haircut.id, beard.id],
}

const bartek: PublicEmployee = {
  id: 'emp-bartek',
  displayName: 'Bartek W.',
  position: null,
  offeringIds: [haircut.id],
}

const celina: PublicEmployee = {
  id: 'emp-celina',
  displayName: 'Celina Z.',
  position: null,
  offeringIds: [beard.id],
}

function renderAt(url: string) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })
  const router = createMemoryRouter(
    [
      { path: '/businesses/:slug', element: <BusinessPage /> },
      { path: '/businesses/:slug/book', element: <BookingPage /> },
    ],
    { initialEntries: [url] },
  )
  render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return router
}

function summary() {
  return within(screen.getByRole('complementary', { name: 'Summary' }))
}

describe('BookingPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-11-26T09:20:00Z'))
    vi.mocked(getBusiness).mockReset()
    vi.mocked(getBusiness).mockResolvedValue(business)
    vi.mocked(listBusinessOfferings).mockReset()
    vi.mocked(listBusinessOfferings).mockResolvedValue([haircut, beard])
    vi.mocked(listBusinessEmployees).mockReset()
    vi.mocked(listBusinessEmployees).mockResolvedValue([anna, bartek, celina])
    vi.mocked(getAvailableSlots).mockReset()
    vi.mocked(getAvailableSlots).mockResolvedValue([
      { startTime: '2026-11-26T10:30:00Z', localTime: '11:30' },
      { startTime: '2026-11-26T10:45:00Z', localTime: '11:45' },
    ])
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('walks from specialist to time and fills the summary', async () => {
    const user = userEvent.setup()
    renderAt(`/businesses/${SLUG}/book?offeringId=${haircut.id}`)

    expect(
      await screen.findByRole('heading', { name: 'Book a visit' }),
    ).toBeInTheDocument()
    expect(
      await screen.findByText('Choose a specialist to see free times.'),
    ).toBeInTheDocument()
    expect(
      summary().getByRole('button', { name: 'Pick a time to continue' }),
    ).toBeDisabled()

    await user.click(await screen.findByRole('button', { name: /Anna K\./ }))
    await user.click(await screen.findByRole('button', { name: '11:30' }))

    expect(getAvailableSlots).toHaveBeenCalledWith({
      slug: SLUG,
      offeringId: haircut.id,
      employeeId: anna.id,
      date: '2026-11-26',
    })
    expect(summary().getByText("Men's haircut")).toBeInTheDocument()
    expect(summary().getByText('Anna K.')).toBeInTheDocument()
    expect(summary().getByText('Thu 26 Nov, 11:30')).toBeInTheDocument()
    expect(summary().getByText(/80/)).toBeInTheDocument()
    expect(
      summary().getByRole('button', { name: 'Confirm booking' }),
    ).toBeDisabled()
  })

  it('lists only specialists who perform the service', async () => {
    renderAt(`/businesses/${SLUG}/book?offeringId=${haircut.id}`)

    const specialists = await screen.findByRole('group', { name: 'Specialist' })

    expect(
      within(specialists).getByRole('button', { name: /Anna K\./ }),
    ).toBeInTheDocument()
    expect(
      within(specialists).getByRole('button', { name: /Bartek W\./ }),
    ).toBeInTheDocument()
    expect(
      within(specialists).queryByRole('button', { name: /Celina Z\./ }),
    ).not.toBeInTheDocument()
  })

  it('clears the picked time when the specialist changes', async () => {
    const user = userEvent.setup()
    renderAt(`/businesses/${SLUG}/book?offeringId=${haircut.id}`)

    await user.click(await screen.findByRole('button', { name: /Anna K\./ }))
    await user.click(await screen.findByRole('button', { name: '11:30' }))
    expect(summary().getByText('Thu 26 Nov, 11:30')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Bartek W\./ }))

    expect(summary().queryByText('Thu 26 Nov, 11:30')).not.toBeInTheDocument()
    expect(
      summary().getByRole('button', { name: 'Pick a time to continue' }),
    ).toBeDisabled()
  })

  it('moves a day that has passed to today after midnight', async () => {
    const user = userEvent.setup()
    renderAt(`/businesses/${SLUG}/book?offeringId=${haircut.id}`)
    await user.click(await screen.findByRole('button', { name: /Anna K\./ }))
    await user.click(await screen.findByRole('button', { name: 'Fri 27' }))
    await user.click(screen.getByRole('button', { name: 'Thu 26' }))
    await screen.findByRole('button', { name: '11:30' })

    vi.setSystemTime(new Date('2026-11-26T23:30:00Z'))
    act(() => {
      window.dispatchEvent(new Event('focus'))
    })
    await user.click(screen.getByRole('button', { name: /Bartek W\./ }))

    expect(screen.getByRole('button', { name: 'Fri 27' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
    expect(getAvailableSlots).toHaveBeenLastCalledWith(
      expect.objectContaining({ employeeId: bartek.id, date: '2026-11-27' }),
    )
  })

  it('explains an unknown service and links back to the list', async () => {
    renderAt(`/businesses/${SLUG}/book?offeringId=unknown`)

    expect(
      await screen.findByText(
        'This service is not available. Please pick one from the list.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'See services' })).toHaveAttribute(
      'href',
      `/businesses/${SLUG}`,
    )
  })

  it('is reached from the Book button on the business page', async () => {
    const user = userEvent.setup()
    const router = renderAt(`/businesses/${SLUG}`)

    await user.click(
      await screen.findByRole('link', { name: "Book Men's haircut" }),
    )

    expect(router.state.location.pathname).toBe(`/businesses/${SLUG}/book`)
    expect(router.state.location.search).toBe(`?offeringId=${haircut.id}`)
    expect(
      await screen.findByRole('heading', { name: 'Book a visit' }),
    ).toBeInTheDocument()
  })
})
