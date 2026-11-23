import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/client'
import { OfferingForm } from '@/components/elements/OfferingForm'
import type { Offering } from '@/services/offerings'

const offering: Offering = {
  id: '0193a1b2-0000-7000-8000-000000000001',
  name: 'Haircut',
  description: null,
  priceAmount: 8050,
  currency: 'PLN',
  durationMinutes: 45,
  bufferMinutes: 10,
}

function renderForm(onSubmit = vi.fn(), initialOffering?: Offering) {
  render(
    <OfferingForm
      offering={initialOffering}
      onSubmit={onSubmit}
      onCancel={vi.fn()}
    />,
  )
  return onSubmit
}

describe('OfferingForm', () => {
  it('sends the price in grosze and minutes as numbers', async () => {
    const user = userEvent.setup()
    const onSubmit = renderForm(vi.fn().mockResolvedValue(undefined))

    await user.type(screen.getByLabelText('Service name'), '  Beard trim ')
    await user.type(screen.getByLabelText('Price (zł)'), '79,99')
    await user.selectOptions(screen.getByLabelText('Duration'), '45')
    await user.selectOptions(screen.getByLabelText('Break after'), '15')
    await user.click(screen.getByRole('button', { name: 'Add service' }))

    expect(onSubmit).toHaveBeenCalledWith({
      name: 'Beard trim',
      description: undefined,
      priceAmount: 7999,
      durationMinutes: 45,
      bufferMinutes: 15,
    })
  })

  it('shows validation errors without submitting', async () => {
    const user = userEvent.setup()
    const onSubmit = renderForm()

    await user.type(screen.getByLabelText('Price (zł)'), '80.505')
    await user.click(screen.getByRole('button', { name: 'Add service' }))

    expect(
      await screen.findByText('Enter the service name'),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Enter a price like 80 or 79,99'),
    ).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('fills the form with an existing offering', () => {
    renderForm(vi.fn(), offering)

    expect(screen.getByLabelText('Service name')).toHaveValue('Haircut')
    expect(screen.getByLabelText('Price (zł)')).toHaveValue('80,50')
    expect(screen.getByLabelText('Duration')).toHaveValue('45')
    expect(screen.getByLabelText('Break after')).toHaveValue('10')
    expect(
      screen.getByRole('button', { name: 'Save changes' }),
    ).toBeInTheDocument()
  })

  it('shows a duplicate name from the server next to the name field', async () => {
    const user = userEvent.setup()
    renderForm(
      vi.fn().mockRejectedValue(
        new ApiError({
          status: 409,
          code: 'OFFERING_NAME_TAKEN',
          detail: 'An offering with this name already exists',
        }),
      ),
      offering,
    )

    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(
      await screen.findByText('You already have a service with this name'),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Service name')).toHaveAttribute(
      'aria-invalid',
      'true',
    )
  })
})
