import { useState } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/client'
import { SlotPicker } from '@/components/elements/SlotPicker'
import { getAvailableSlots } from '@/services/availability'

vi.mock('@/services/availability', () => ({
  getAvailableSlots: vi.fn(),
}))

const TODAY = '2026-11-26'

function Harness({ initialDate }: { initialDate: string }) {
  const [date, setDate] = useState(initialDate)
  const [selected, setSelected] = useState<string | null>(null)
  return (
    <SlotPicker
      slug="k3f9x2ab-barber-shop-krakow"
      offeringId="off-1"
      employeeId="emp-anna"
      timeZone="Europe/Warsaw"
      today={TODAY}
      date={date}
      onDateChange={(day) => {
        setDate(day)
        setSelected(null)
      }}
      selectedStartTime={selected}
      onSelectSlot={(slot) => setSelected(slot.startTime)}
    />
  )
}

function renderPicker(initialDate = TODAY): QueryClient {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        retryDelay: 0,
      },
    },
  })
  render(
    <QueryClientProvider client={queryClient}>
      <Harness initialDate={initialDate} />
    </QueryClientProvider>,
  )
  return queryClient
}

describe('SlotPicker', () => {
  beforeEach(() => {
    vi.mocked(getAvailableSlots).mockReset()
    vi.mocked(getAvailableSlots).mockResolvedValue([
      { startTime: '2026-11-26T10:30:00Z', localTime: '11:30' },
      { startTime: '2026-11-26T10:45:00Z', localTime: '11:45' },
    ])
  })

  it('shows free times of the selected day and marks the picked one', async () => {
    const user = userEvent.setup()
    renderPicker()

    const time = await screen.findByRole('button', { name: '11:45' })
    expect(getAvailableSlots).toHaveBeenCalledWith({
      slug: 'k3f9x2ab-barber-shop-krakow',
      offeringId: 'off-1',
      employeeId: 'emp-anna',
      date: TODAY,
    })
    expect(screen.getByText(/Times in Europe\/Warsaw/)).toBeInTheDocument()

    await user.click(time)

    expect(time).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: '11:30' })).toHaveAttribute(
      'aria-pressed',
      'false',
    )
  })

  it('disables past days and loads times of another day when clicked', async () => {
    const user = userEvent.setup()
    renderPicker()
    await screen.findByRole('button', { name: '11:30' })

    expect(screen.getByRole('button', { name: 'Wed 25' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Thu 26' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )

    await user.click(screen.getByRole('button', { name: 'Fri 27' }))

    expect(screen.getByRole('button', { name: 'Fri 27' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
    expect(getAvailableSlots).toHaveBeenLastCalledWith(
      expect.objectContaining({ date: '2026-11-27' }),
    )
  })

  it('selects the first bookable day when moving between weeks', async () => {
    const user = userEvent.setup()
    renderPicker()

    expect(screen.getByText('23–29 Nov')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Previous week' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Next week' }))

    expect(screen.getByText('30 Nov – 6 Dec')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Mon 30' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
    expect(getAvailableSlots).toHaveBeenLastCalledWith(
      expect.objectContaining({ date: '2026-11-30' }),
    )

    await user.click(screen.getByRole('button', { name: 'Previous week' }))

    expect(screen.getByRole('button', { name: 'Thu 26' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
  })

  it('stops at the last bookable day', () => {
    renderPicker('2027-05-25')

    expect(screen.getByRole('button', { name: 'Next week' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Tue 25' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Wed 26' })).toBeDisabled()
  })

  it('tells the client when the day has no free times', async () => {
    vi.mocked(getAvailableSlots).mockResolvedValue([])
    renderPicker()

    expect(
      await screen.findByText(
        'No free times on this day. Try another day or specialist.',
      ),
    ).toBeInTheDocument()
  })

  it('explains a specialist who can no longer be booked without offering a retry', async () => {
    vi.mocked(getAvailableSlots).mockRejectedValue(
      new ApiError({
        status: 404,
        title: 'Not Found',
      }),
    )
    renderPicker()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'This specialist no longer offers this service',
    )
    expect(
      screen.queryByRole('button', { name: 'Try again' }),
    ).not.toBeInTheDocument()
  })

  it('offers a retry when free times could not be loaded', async () => {
    const user = userEvent.setup()
    vi.mocked(getAvailableSlots)
      .mockRejectedValueOnce(new Error('offline'))
      .mockRejectedValueOnce(new Error('offline'))
      .mockRejectedValueOnce(new Error('offline'))
    renderPicker()

    await user.click(await screen.findByRole('button', { name: 'Try again' }))

    expect(
      await screen.findByRole('button', { name: '11:30' }),
    ).toBeInTheDocument()
  })

  it('asks for another day when the server rejects the date', async () => {
    vi.mocked(getAvailableSlots).mockRejectedValue(
      new ApiError({
        status: 400,
        title: 'Bad Request',
      }),
    )
    renderPicker()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      "This day can't be booked. Please pick another day.",
    )
    expect(
      screen.queryByRole('button', { name: 'Try again' }),
    ).not.toBeInTheDocument()
  })

  it('keeps loaded times when a background refresh fails', async () => {
    const queryClient = renderPicker()
    await screen.findByRole('button', { name: '11:30' })
    vi.mocked(getAvailableSlots).mockRejectedValue(new Error('offline'))

    await queryClient.refetchQueries()

    expect(screen.getByRole('button', { name: '11:30' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
