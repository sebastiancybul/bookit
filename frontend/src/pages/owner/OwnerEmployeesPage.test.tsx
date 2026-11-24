import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/client'
import { OwnerEmployeesPage } from '@/pages/owner/OwnerEmployeesPage'
import {
  assignEmployeeOfferings,
  createEmployee,
  deleteEmployee,
  linkEmployeeAccount,
  listMyEmployees,
  unlinkEmployeeAccount,
  type Employee,
} from '@/services/employees'
import { listMyOfferings, type Offering } from '@/services/offerings'
import {
  getEmployeeSchedule,
  replaceEmployeeSchedule,
  type WorkSchedule,
} from '@/services/schedules'

vi.mock('@/services/employees', () => ({
  listMyEmployees: vi.fn(),
  createEmployee: vi.fn(),
  updateEmployee: vi.fn(),
  assignEmployeeOfferings: vi.fn(),
  linkEmployeeAccount: vi.fn(),
  unlinkEmployeeAccount: vi.fn(),
  deleteEmployee: vi.fn(),
}))

vi.mock('@/services/offerings', () => ({
  listMyOfferings: vi.fn(),
}))

vi.mock('@/services/schedules', () => ({
  getEmployeeSchedule: vi.fn(),
  replaceEmployeeSchedule: vi.fn(),
}))

vi.mock('@/services/auth', () => ({
  getMe: vi.fn().mockResolvedValue(null),
}))

const haircut: Offering = {
  id: '0193a1b2-0000-7000-8000-000000000001',
  name: 'Haircut',
  description: null,
  priceAmount: 8000,
  currency: 'PLN',
  durationMinutes: 45,
  bufferMinutes: 0,
}

const beard: Offering = {
  ...haircut,
  id: '0193a1b2-0000-7000-8000-000000000002',
  name: 'Beard trim',
  durationMinutes: 30,
}

const anna: Employee = {
  id: '0193a1b2-0000-7000-8000-0000000000a1',
  displayName: 'Anna K.',
  position: 'Senior barber',
  offeringIds: [haircut.id, '0193a1b2-0000-7000-8000-0000000000ff'],
  accountEmail: null,
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
      <OwnerEmployeesPage />
    </QueryClientProvider>,
  )
}

describe('OwnerEmployeesPage', () => {
  beforeEach(() => {
    vi.mocked(listMyEmployees).mockReset()
    vi.mocked(createEmployee).mockReset()
    vi.mocked(assignEmployeeOfferings).mockReset()
    vi.mocked(linkEmployeeAccount).mockReset()
    vi.mocked(unlinkEmployeeAccount).mockReset()
    vi.mocked(deleteEmployee).mockReset()
    vi.mocked(listMyOfferings).mockReset()
    vi.mocked(listMyOfferings).mockResolvedValue([haircut, beard])
    vi.mocked(getEmployeeSchedule).mockReset()
    vi.mocked(replaceEmployeeSchedule).mockReset()
  })

  it('lists employees with position, known service names and account', async () => {
    vi.mocked(listMyEmployees).mockResolvedValue([
      anna,
      {
        ...anna,
        id: '0193a1b2-0000-7000-8000-0000000000b2',
        displayName: 'Bartek W.',
        position: null,
        offeringIds: [],
        accountEmail: 'bartek@test.pl',
      },
    ])
    renderPage()

    const annaRow = (await screen.findByText('Anna K.')).closest('tr')!
    expect(within(annaRow).getByText('Senior barber')).toBeInTheDocument()
    expect(await within(annaRow).findByText('Haircut')).toBeInTheDocument()
    expect(within(annaRow).getByText('Not linked')).toBeInTheDocument()

    const bartekRow = screen.getByText('Bartek W.').closest('tr')!
    expect(within(bartekRow).getByText('No services')).toBeInTheDocument()
    expect(within(bartekRow).getByText('bartek@test.pl')).toBeInTheDocument()
  })

  it('shows an empty state when there are no employees', async () => {
    vi.mocked(listMyEmployees).mockResolvedValue([])
    renderPage()

    expect(
      await screen.findByRole('heading', { name: 'No employees yet' }),
    ).toBeInTheDocument()
  })

  it('adds an employee and shows a taken name on the field', async () => {
    const user = userEvent.setup()
    vi.mocked(listMyEmployees).mockResolvedValue([])
    vi.mocked(createEmployee)
      .mockRejectedValueOnce(
        new ApiError({ status: 409, code: 'EMPLOYEE_NAME_TAKEN' }),
      )
      .mockResolvedValueOnce(anna)
    renderPage()

    await screen.findByRole('heading', { name: 'No employees yet' })
    await user.click(screen.getByRole('button', { name: 'Add employee' }))
    const dialog = await screen.findByRole('dialog')
    await user.type(within(dialog).getByLabelText('Name'), 'Anna K.')
    await user.click(
      within(dialog).getByRole('button', { name: 'Add employee' }),
    )

    expect(
      await within(dialog).findByText(
        'You already have an employee with this name',
      ),
    ).toBeInTheDocument()

    vi.mocked(listMyEmployees).mockResolvedValue([anna])
    await user.click(
      within(dialog).getByRole('button', { name: 'Add employee' }),
    )

    await waitFor(() =>
      expect(createEmployee).toHaveBeenLastCalledWith(
        {
          displayName: 'Anna K.',
          position: undefined,
        },
        expect.anything(),
      ),
    )
    expect(await screen.findByText('Anna K.')).toBeInTheDocument()
  })

  it('assigns services with checkboxes and drops ids of deleted services', async () => {
    const user = userEvent.setup()
    vi.mocked(listMyEmployees).mockResolvedValue([anna])
    vi.mocked(assignEmployeeOfferings).mockResolvedValue(anna)
    renderPage()

    await user.click(
      await screen.findByRole('button', { name: 'Services of Anna K.' }),
    )
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByLabelText(/Haircut/)).toBeChecked()
    expect(within(dialog).getByLabelText(/Beard trim/)).not.toBeChecked()
    await user.click(within(dialog).getByLabelText(/Beard trim/))
    await user.click(
      within(dialog).getByRole('button', { name: 'Save services' }),
    )

    await waitFor(() =>
      expect(assignEmployeeOfferings).toHaveBeenCalledWith(
        {
          id: anna.id,
          offeringIds: [haircut.id, beard.id],
        },
        expect.anything(),
      ),
    )
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    )
  })

  it('links an account and shows server errors on the email field', async () => {
    const user = userEvent.setup()
    vi.mocked(listMyEmployees).mockResolvedValue([anna])
    vi.mocked(linkEmployeeAccount)
      .mockRejectedValueOnce(
        new ApiError({
          status: 400,
          errors: [
            {
              field: 'email',
              message: 'No Bookit account with this email',
            },
          ],
        }),
      )
      .mockRejectedValueOnce(
        new ApiError({ status: 409, code: 'EMPLOYEE_ACCOUNT_TAKEN' }),
      )
      .mockResolvedValueOnce({ ...anna, accountEmail: 'anna@test.pl' })
    renderPage()

    await user.click(
      await screen.findByRole('button', { name: 'Account of Anna K.' }),
    )
    const dialog = await screen.findByRole('dialog')
    const email = within(dialog).getByLabelText('Account email')
    const submit = within(dialog).getByRole('button', { name: 'Link account' })

    await user.type(email, 'not-an-email')
    await user.click(submit)
    expect(
      await within(dialog).findByText('Enter a valid email address'),
    ).toBeInTheDocument()
    expect(linkEmployeeAccount).not.toHaveBeenCalled()

    await user.clear(email)
    await user.type(email, '  anna@test.pl ')
    await user.click(submit)
    expect(
      await within(dialog).findByText('No Bookit account uses this email'),
    ).toBeInTheDocument()

    await user.click(submit)
    expect(
      await within(dialog).findByText(
        'This account already belongs to an employee',
      ),
    ).toBeInTheDocument()

    await user.click(submit)
    await waitFor(() =>
      expect(linkEmployeeAccount).toHaveBeenLastCalledWith(
        {
          id: anna.id,
          email: 'anna@test.pl',
        },
        expect.anything(),
      ),
    )
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    )
  })

  it('unlinks a linked account', async () => {
    const user = userEvent.setup()
    vi.mocked(listMyEmployees).mockResolvedValue([
      { ...anna, accountEmail: 'anna@test.pl' },
    ])
    vi.mocked(unlinkEmployeeAccount).mockResolvedValue(undefined)
    renderPage()

    await user.click(
      await screen.findByRole('button', { name: 'Account of Anna K.' }),
    )
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText('anna@test.pl')).toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: 'Unlink' }))

    await waitFor(() =>
      expect(unlinkEmployeeAccount).toHaveBeenCalledWith(
        anna.id,
        expect.anything(),
      ),
    )
  })

  it('deletes an employee after confirmation', async () => {
    const user = userEvent.setup()
    vi.mocked(listMyEmployees).mockResolvedValue([anna])
    vi.mocked(deleteEmployee).mockResolvedValue(undefined)
    renderPage()

    await user.click(
      await screen.findByRole('button', { name: 'Delete Anna K.' }),
    )
    const confirm = await screen.findByRole('alertdialog')
    vi.mocked(listMyEmployees).mockResolvedValue([])
    await user.click(within(confirm).getByRole('button', { name: 'Delete' }))

    await waitFor(() =>
      expect(deleteEmployee).toHaveBeenCalledWith(anna.id, expect.anything()),
    )
    expect(
      await screen.findByRole('heading', { name: 'No employees yet' }),
    ).toBeInTheDocument()
  })

  it('edits working hours only after the saved schedule has loaded', async () => {
    const user = userEvent.setup()
    const saved: WorkSchedule = {
      intervals: [{ dayOfWeek: 1, startTime: '09:00', endTime: '17:00' }],
    }
    let resolveSchedule: (schedule: WorkSchedule) => void = () => {}
    vi.mocked(listMyEmployees).mockResolvedValue([anna])
    vi.mocked(getEmployeeSchedule).mockReturnValue(
      new Promise((resolve) => {
        resolveSchedule = resolve
      }),
    )
    vi.mocked(replaceEmployeeSchedule).mockImplementation(
      async ({ intervals }) => ({ intervals }),
    )
    renderPage()

    await user.click(
      await screen.findByRole('button', { name: 'Working hours of Anna K.' }),
    )
    const dialog = await screen.findByRole('dialog')
    expect(
      within(dialog).getByRole('heading', { name: 'Working hours: Anna K.' }),
    ).toBeInTheDocument()
    expect(within(dialog).getByText('Loading…')).toBeInTheDocument()
    expect(
      within(dialog).queryByRole('button', { name: 'Save hours' }),
    ).not.toBeInTheDocument()

    resolveSchedule(saved)
    expect(await within(dialog).findByLabelText('Monday')).toBeChecked()
    await user.click(
      within(dialog).getByRole('button', { name: 'Copy Monday to weekdays' }),
    )
    await user.click(within(dialog).getByRole('button', { name: 'Save hours' }))

    await waitFor(() =>
      expect(replaceEmployeeSchedule).toHaveBeenCalledWith(
        {
          employeeId: anna.id,
          intervals: [1, 2, 3, 4, 5].map((dayOfWeek) => ({
            dayOfWeek,
            startTime: '09:00',
            endTime: '17:00',
          })),
        },
        expect.anything(),
      ),
    )
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    )
    expect(getEmployeeSchedule).toHaveBeenCalledWith(anna.id)
  })

  it('reloads the latest hours after a concurrent save', async () => {
    const user = userEvent.setup()
    vi.mocked(listMyEmployees).mockResolvedValue([anna])
    vi.mocked(getEmployeeSchedule)
      .mockResolvedValueOnce({ intervals: [] })
      .mockResolvedValueOnce({
        intervals: [{ dayOfWeek: 3, startTime: '10:00', endTime: '14:00' }],
      })
    vi.mocked(replaceEmployeeSchedule).mockRejectedValue(
      new ApiError({ status: 409, code: 'CONCURRENT_UPDATE' }),
    )
    renderPage()

    await user.click(
      await screen.findByRole('button', { name: 'Working hours of Anna K.' }),
    )
    const dialog = await screen.findByRole('dialog')
    await user.click(await within(dialog).findByLabelText('Monday'))
    await user.click(within(dialog).getByRole('button', { name: 'Save hours' }))
    await user.click(
      await within(dialog).findByRole('button', { name: 'Reload' }),
    )

    await waitFor(() =>
      expect(within(dialog).getByLabelText('Wednesday')).toBeChecked(),
    )
    expect(within(dialog).getByLabelText('Monday')).not.toBeChecked()
    expect(
      within(dialog).queryByText(/Someone else saved/),
    ).not.toBeInTheDocument()
  })

  it('shows an error when the working hours cannot be loaded', async () => {
    const user = userEvent.setup()
    vi.mocked(listMyEmployees).mockResolvedValue([anna])
    vi.mocked(getEmployeeSchedule).mockRejectedValue(
      new ApiError({ status: 403 }),
    )
    renderPage()

    await user.click(
      await screen.findByRole('button', { name: 'Working hours of Anna K.' }),
    )

    expect(
      await screen.findByText(
        "We couldn't load the working hours. Please refresh the page.",
      ),
    ).toBeInTheDocument()
  })

  it('waits for fresh hours when the dialog is opened again', async () => {
    const user = userEvent.setup()
    let resolveSecond: (schedule: WorkSchedule) => void = () => {}
    vi.mocked(listMyEmployees).mockResolvedValue([anna])
    vi.mocked(getEmployeeSchedule)
      .mockResolvedValueOnce({
        intervals: [{ dayOfWeek: 1, startTime: '09:00', endTime: '17:00' }],
      })
      .mockReturnValueOnce(
        new Promise((resolve) => {
          resolveSecond = resolve
        }),
      )
    renderPage()

    const hours = await screen.findByRole('button', {
      name: 'Working hours of Anna K.',
    })
    await user.click(hours)
    await screen.findByLabelText('Monday')
    await user.click(screen.getByRole('button', { name: 'Cancel' }))
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    )

    await user.click(hours)
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText('Loading…')).toBeInTheDocument()

    resolveSecond({
      intervals: [{ dayOfWeek: 3, startTime: '10:00', endTime: '14:00' }],
    })
    expect(await within(dialog).findByLabelText('Wednesday')).toBeChecked()
    expect(within(dialog).getByLabelText('Monday')).not.toBeChecked()
  })

  it('says when the employee no longer exists', async () => {
    const user = userEvent.setup()
    vi.mocked(listMyEmployees).mockResolvedValue([anna])
    vi.mocked(getEmployeeSchedule).mockRejectedValue(
      new ApiError({ status: 404 }),
    )
    renderPage()

    await user.click(
      await screen.findByRole('button', { name: 'Working hours of Anna K.' }),
    )

    expect(
      await screen.findByText('This employee no longer exists.'),
    ).toBeInTheDocument()
    await waitFor(() => expect(listMyEmployees).toHaveBeenCalledTimes(2))
  })
})
