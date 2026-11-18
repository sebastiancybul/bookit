import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/client'
import { RegisterPage } from '@/pages/RegisterPage'
import { register } from '@/services/auth'

vi.mock('@/services/auth', () => ({
  register: vi.fn(),
}))

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      mutations: {
        retry: false,
      },
    },
  })
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.mocked(register).mockReset()
  })

  it('shows validation messages and does not call the API for an empty form', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('Enter your first name')).toBeInTheDocument()
    expect(screen.getByText('Enter your last name')).toBeInTheDocument()
    expect(screen.getByText('Enter a valid email address')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveAttribute(
      'aria-invalid',
      'true',
    )
    expect(register).not.toHaveBeenCalled()
  })

  it('shows EMAIL_TAKEN from the server under the email field', async () => {
    vi.mocked(register).mockRejectedValue(
      new ApiError({
        status: 409,
        title: 'Conflict',
        detail: 'An account with this email already exists',
        code: 'EMAIL_TAKEN',
      }),
    )
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByLabelText('First name'), 'Jan')
    await user.type(screen.getByLabelText('Last name'), 'Kowalski')
    await user.type(screen.getByLabelText('Email'), 'jan@test.pl')
    await user.type(screen.getByLabelText('Phone'), '+48600123456')
    await user.type(screen.getByLabelText('Password'), 'Secret123!x')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(
      await screen.findByText('An account with this email already exists'),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveAttribute(
      'aria-invalid',
      'true',
    )
    expect(register).toHaveBeenCalledTimes(1)
  })
})
