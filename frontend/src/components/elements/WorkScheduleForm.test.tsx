import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/client'
import { WorkScheduleForm } from '@/components/elements/WorkScheduleForm'
import type { WorkSchedule } from '@/services/schedules'

const splitMonday: WorkSchedule = {
  intervals: [
    { dayOfWeek: 1, startTime: '09:00', endTime: '13:00' },
    { dayOfWeek: 1, startTime: '14:00', endTime: '18:00' },
  ],
}

function renderForm(
  schedule: WorkSchedule = { intervals: [] },
  onSubmit = vi.fn().mockResolvedValue(undefined),
  onReload = vi.fn(),
) {
  render(
    <WorkScheduleForm
      schedule={schedule}
      onSubmit={onSubmit}
      onCancel={vi.fn()}
      onReload={onReload}
    />,
  )
  return { onSubmit, onReload }
}

describe('WorkScheduleForm', () => {
  it('shows saved hours and marks other days as days off', () => {
    renderForm(splitMonday)

    expect(screen.getByLabelText('Monday')).toBeChecked()
    expect(screen.getByLabelText('Monday start 1')).toHaveValue('09:00')
    expect(screen.getByLabelText('Monday end 2')).toHaveValue('18:00')
    expect(screen.getByLabelText('Sunday')).not.toBeChecked()
    expect(screen.getAllByText('Day off')).toHaveLength(6)
  })

  it('enables a day with default hours, adds a break and saves the week', async () => {
    const user = userEvent.setup()
    const { onSubmit } = renderForm()

    await user.click(screen.getByLabelText('Tuesday'))
    expect(screen.getByLabelText('Tuesday start 1')).toHaveValue('09:00')
    expect(screen.getByLabelText('Tuesday end 1')).toHaveValue('17:00')

    await user.clear(screen.getByLabelText('Tuesday end 1'))
    await user.type(screen.getByLabelText('Tuesday end 1'), '13:00')
    await user.click(
      screen.getByRole('button', { name: 'Add hours on Tuesday' }),
    )
    expect(screen.getByLabelText('Tuesday start 2')).toHaveValue('14:00')
    await user.clear(screen.getByLabelText('Tuesday end 2'))
    await user.type(screen.getByLabelText('Tuesday end 2'), '18:00')
    await user.click(screen.getByRole('button', { name: 'Save hours' }))

    expect(onSubmit).toHaveBeenCalledWith([
      { dayOfWeek: 2, startTime: '09:00', endTime: '13:00' },
      { dayOfWeek: 2, startTime: '14:00', endTime: '18:00' },
    ])
  })

  it('sends an empty week when every day is off', async () => {
    const user = userEvent.setup()
    const { onSubmit } = renderForm(splitMonday)

    await user.click(screen.getByLabelText('Monday'))
    await user.click(screen.getByRole('button', { name: 'Save hours' }))

    expect(onSubmit).toHaveBeenCalledWith([])
  })

  it('removes a range and turns the day off after the last one', async () => {
    const user = userEvent.setup()
    renderForm(splitMonday)

    await user.click(
      screen.getByRole('button', { name: 'Remove Monday hours 2' }),
    )
    expect(screen.queryByLabelText('Monday start 2')).not.toBeInTheDocument()
    await user.click(
      screen.getByRole('button', { name: 'Remove Monday hours 1' }),
    )

    expect(screen.getByLabelText('Monday')).not.toBeChecked()
  })

  it('shows overlapping hours on the day and does not submit', async () => {
    const user = userEvent.setup()
    const { onSubmit } = renderForm(splitMonday)

    await user.clear(screen.getByLabelText('Monday start 2'))
    await user.type(screen.getByLabelText('Monday start 2'), '12:00')
    await user.click(screen.getByRole('button', { name: 'Save hours' }))

    expect(
      screen.getByText('Fix the highlighted days before saving.'),
    ).toBeInTheDocument()
    expect(screen.getByText('Hours on this day overlap')).toBeInTheDocument()
    expect(screen.getByLabelText('Monday start 1')).toHaveAccessibleDescription(
      'Hours on this day overlap',
    )
    expect(onSubmit).not.toHaveBeenCalled()

    await user.clear(screen.getByLabelText('Monday start 2'))
    await user.type(screen.getByLabelText('Monday start 2'), '13:00')
    expect(
      screen.queryByText('Hours on this day overlap'),
    ).not.toBeInTheDocument()
  })

  it('copies Monday to the weekdays', async () => {
    const user = userEvent.setup()
    const { onSubmit } = renderForm(splitMonday)

    await user.click(
      screen.getByRole('button', { name: 'Copy Monday to weekdays' }),
    )
    expect(screen.getByLabelText('Friday end 2')).toHaveValue('18:00')
    expect(screen.getByLabelText('Saturday')).not.toBeChecked()
    await user.click(screen.getByRole('button', { name: 'Save hours' }))

    expect(onSubmit.mock.calls[0][0]).toHaveLength(10)
  })

  it('disables copying when Monday is a day off', () => {
    renderForm()

    expect(
      screen.getByRole('button', { name: 'Copy Monday to weekdays' }),
    ).toBeDisabled()
  })

  it('disables copying when it would pass the weekly limit', () => {
    const tenOnMonday = Array.from({ length: 10 }, (_, index) => {
      const hour = String(index + 8).padStart(2, '0')
      return {
        dayOfWeek: 1,
        startTime: `${hour}:00`,
        endTime: `${hour}:30`,
      }
    })
    renderForm({
      intervals: [
        ...tenOnMonday,
        { dayOfWeek: 6, startTime: '10:00', endTime: '14:00' },
      ],
    })

    expect(
      screen.getByRole('button', { name: 'Copy Monday to weekdays' }),
    ).toBeDisabled()
  })

  it('offers a reload after a concurrent save', async () => {
    const user = userEvent.setup()
    const { onReload } = renderForm(
      splitMonday,
      vi
        .fn()
        .mockRejectedValue(
          new ApiError({ status: 409, code: 'CONCURRENT_UPDATE' }),
        ),
    )

    await user.click(screen.getByRole('button', { name: 'Save hours' }))
    expect(
      await screen.findByText(/Someone else saved these hours/),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Reload' }))

    expect(onReload).toHaveBeenCalled()
  })

  it('shows the field message from a 400 response', async () => {
    const user = userEvent.setup()
    renderForm(
      splitMonday,
      vi.fn().mockRejectedValue(
        new ApiError({
          status: 400,
          errors: [
            {
              field: 'intervals',
              message: 'Times must be in 5-minute steps',
            },
          ],
        }),
      ),
    )

    await user.click(screen.getByRole('button', { name: 'Save hours' }))

    expect(
      await screen.findByText('Times must be in 5-minute steps'),
    ).toBeInTheDocument()
  })
})
