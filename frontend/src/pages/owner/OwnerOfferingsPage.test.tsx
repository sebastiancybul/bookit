import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { OwnerOfferingsPage } from '@/pages/owner/OwnerOfferingsPage'
import { listMyEmployees } from '@/services/employees'
import {
  createOffering,
  deleteOffering,
  listMyOfferings,
  type Offering,
} from '@/services/offerings'

vi.mock('@/services/employees', () => ({
  listMyEmployees: vi.fn(),
}))

vi.mock('@/services/offerings', () => ({
  listMyOfferings: vi.fn(),
  createOffering: vi.fn(),
  updateOffering: vi.fn(),
  deleteOffering: vi.fn(),
}))

const haircut: Offering = {
  id: '0193a1b2-0000-7000-8000-000000000001',
  name: 'Haircut',
  description: null,
  priceAmount: 8000,
  currency: 'PLN',
  durationMinutes: 45,
  bufferMinutes: 10,
}

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })
  render(
    <QueryClientProvider client={queryClient}>
      <OwnerOfferingsPage />
    </QueryClientProvider>,
  )
}

describe('OwnerOfferingsPage', () => {
  beforeEach(() => {
    vi.mocked(listMyOfferings).mockReset()
    vi.mocked(createOffering).mockReset()
    vi.mocked(deleteOffering).mockReset()
    vi.mocked(listMyEmployees).mockReset()
    vi.mocked(listMyEmployees).mockResolvedValue([])
  })

  it('shows assigned employees as specialists', async () => {
    vi.mocked(listMyOfferings).mockResolvedValue([haircut])
    vi.mocked(listMyEmployees).mockResolvedValue([
      {
        id: '0193a1b2-0000-7000-8000-0000000000a1',
        displayName: 'Anna K.',
        position: null,
        offeringIds: [haircut.id],
        accountEmail: null,
      },
      {
        id: '0193a1b2-0000-7000-8000-0000000000b2',
        displayName: 'Bartek W.',
        position: null,
        offeringIds: [],
        accountEmail: null,
      },
    ])
    renderPage()

    const row = (await screen.findByText('Haircut')).closest('tr')!
    expect(await within(row).findByText('Anna K.')).toBeInTheDocument()
    expect(within(row).queryByText(/Bartek/)).not.toBeInTheDocument()
  })

  it('shows None when nobody performs the offering', async () => {
    vi.mocked(listMyOfferings).mockResolvedValue([haircut])
    renderPage()

    const row = (await screen.findByText('Haircut')).closest('tr')!
    expect(await within(row).findByText('None')).toBeInTheDocument()
  })

  it('lists offerings with formatted duration, price and break', async () => {
    vi.mocked(listMyOfferings).mockResolvedValue([haircut])
    renderPage()

    const row = (await screen.findByText('Haircut')).closest('tr')!
    expect(within(row).getByText('45 min')).toBeInTheDocument()
    expect(within(row).getByText(/80\szł/)).toBeInTheDocument()
    expect(within(row).getByText('+ 10 min break')).toBeInTheDocument()
  })

  it('shows an empty state when there are no offerings', async () => {
    vi.mocked(listMyOfferings).mockResolvedValue([])
    renderPage()

    expect(
      await screen.findByRole('heading', { name: 'No services yet' }),
    ).toBeInTheDocument()
  })

  it('adds an offering from the dialog and refreshes the list', async () => {
    const user = userEvent.setup()
    vi.mocked(listMyOfferings).mockResolvedValue([])
    vi.mocked(createOffering).mockResolvedValue(haircut)
    renderPage()

    await screen.findByRole('heading', { name: 'No services yet' })
    vi.mocked(listMyOfferings).mockResolvedValue([haircut])
    await user.click(screen.getByRole('button', { name: 'Add service' }))
    const dialog = await screen.findByRole('dialog')
    await user.type(within(dialog).getByLabelText('Service name'), 'Haircut')
    await user.type(within(dialog).getByLabelText('Price (zł)'), '80')
    await user.click(
      within(dialog).getByRole('button', { name: 'Add service' }),
    )

    await waitFor(() =>
      expect(createOffering).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'Haircut',
          priceAmount: 8000,
        }),
        expect.anything(),
      ),
    )
    expect(await screen.findByText('Haircut')).toBeInTheDocument()
  })

  it('deletes an offering after confirmation', async () => {
    const user = userEvent.setup()
    vi.mocked(listMyOfferings).mockResolvedValue([haircut])
    vi.mocked(deleteOffering).mockResolvedValue(undefined)
    renderPage()

    await user.click(
      await screen.findByRole('button', { name: 'Delete Haircut' }),
    )
    const confirm = await screen.findByRole('alertdialog')
    expect(within(confirm).getByText('Delete Haircut?')).toBeInTheDocument()
    vi.mocked(listMyOfferings).mockResolvedValue([])
    await user.click(within(confirm).getByRole('button', { name: 'Delete' }))

    await waitFor(() =>
      expect(deleteOffering).toHaveBeenCalledWith(
        haircut.id,
        expect.anything(),
      ),
    )
    expect(
      await screen.findByRole('heading', { name: 'No services yet' }),
    ).toBeInTheDocument()
  })
})
