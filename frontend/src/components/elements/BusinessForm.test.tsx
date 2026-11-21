import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent, { type UserEvent } from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/client'
import { BusinessForm } from '@/components/elements/BusinessForm'
import { listCategories, type Business } from '@/services/businesses'

vi.mock('@/services/businesses', () => ({
  listCategories: vi.fn(),
}))

const business: Business = {
  publicId: 'k3f9x2ab',
  slug: 'k3f9x2ab-studio-urody-lodz',
  name: 'Studio Urody',
  description: null,
  category: {
    code: 'BEAUTY',
    name: 'Beauty',
  },
  phone: '123456789',
  email: null,
  street: 'Piotrkowska 10',
  city: 'Łódź',
  postalCode: '90-001',
  timezone: 'Europe/Warsaw',
  freeCancellationHours: 24,
  slotIntervalMinutes: 15,
}

function renderForm(onSubmit = vi.fn(), initialBusiness?: Business) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })
  render(
    <QueryClientProvider client={queryClient}>
      <BusinessForm
        business={initialBusiness}
        submitLabel="Save"
        pendingLabel="Saving…"
        onSubmit={onSubmit}
      />
    </QueryClientProvider>,
  )
  return onSubmit
}

async function fillValidForm(user: UserEvent) {
  await screen.findByRole('option', { name: 'Barber' })
  await user.type(screen.getByLabelText('Business name'), '  Barber Shop  ')
  await user.selectOptions(screen.getByLabelText('Category'), 'BARBER')
  await user.type(screen.getByLabelText('Phone'), '+48 600-123 456')
  await user.type(
    screen.getByLabelText('Contact email (optional)'),
    ' Kontakt@Salon.pl ',
  )
  await user.type(screen.getByLabelText('Street and number'), 'Floriańska 1')
  await user.type(screen.getByLabelText('City'), 'Kraków')
  await user.type(screen.getByLabelText('Postal code'), '31-019')
}

describe('BusinessForm', () => {
  beforeEach(() => {
    vi.mocked(listCategories).mockReset()
    vi.mocked(listCategories).mockResolvedValue([
      { code: 'BARBER', name: 'Barber' },
      { code: 'BEAUTY', name: 'Beauty' },
    ])
  })

  it('shows validation messages and does not submit an empty form', async () => {
    const user = userEvent.setup()
    const onSubmit = renderForm()
    await screen.findByRole('option', { name: 'Barber' })

    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(
      await screen.findByText('Enter the business name'),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Choose a category', {
        selector: '[data-slot="field-error"]',
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('Use the format 00-000')).toBeInTheDocument()
    expect(screen.getByLabelText('Category')).toHaveAttribute(
      'aria-invalid',
      'true',
    )
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('submits normalized values without an empty description', async () => {
    const user = userEvent.setup()
    const onSubmit = renderForm(vi.fn().mockResolvedValue(undefined))

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(onSubmit).toHaveBeenCalledTimes(1)
    expect(onSubmit.mock.calls[0][0]).toStrictEqual({
      name: 'Barber Shop',
      categoryCode: 'BARBER',
      phone: '+48600123456',
      email: 'Kontakt@Salon.pl',
      description: undefined,
      street: 'Floriańska 1',
      city: 'Kraków',
      postalCode: '31-019',
    })
  })

  it('shows a server field error under the matching field', async () => {
    const user = userEvent.setup()
    renderForm(
      vi.fn().mockRejectedValue(
        new ApiError({
          status: 400,
          detail: 'Request validation failed',
          errors: [
            {
              field: 'categoryCode',
              message: 'Unknown category',
            },
          ],
        }),
      ),
    )

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Unknown category')).toBeInTheDocument()
    expect(screen.getByLabelText('Category')).toHaveAttribute(
      'aria-invalid',
      'true',
    )
  })

  it('shows a business rule error above the form', async () => {
    const user = userEvent.setup()
    renderForm(
      vi.fn().mockRejectedValue(
        new ApiError({
          status: 409,
          detail: 'You already have a business',
          code: 'BUSINESS_ALREADY_EXISTS',
        }),
      ),
    )

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'You already have a business',
    )
  })

  it('prefills the fields from an existing business', async () => {
    renderForm(vi.fn(), business)

    await screen.findByRole('option', { name: 'Beauty' })

    expect(screen.getByLabelText('Business name')).toHaveValue('Studio Urody')
    expect(screen.getByLabelText('Category')).toHaveValue('BEAUTY')
    expect(screen.getByLabelText('City')).toHaveValue('Łódź')
    expect(screen.getByLabelText('Contact email (optional)')).toHaveValue('')
    expect(screen.getByLabelText('Description (optional)')).toHaveValue('')
  })
})
