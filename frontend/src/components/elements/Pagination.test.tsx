import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Pagination } from '@/components/elements/Pagination'

function pageButtonLabels() {
  return screen
    .getAllByRole('button', { name: /^Page / })
    .map((button) => button.textContent)
}

describe('Pagination', () => {
  it('renders nothing for a single page', () => {
    const { container } = render(
      <Pagination page={0} totalPages={1} onPageChange={vi.fn()} />,
    )

    expect(container).toBeEmptyDOMElement()
  })

  it('shows first, last and neighbours of the current page with gaps', () => {
    render(<Pagination page={4} totalPages={12} onPageChange={vi.fn()} />)

    expect(pageButtonLabels()).toEqual(['1', '4', '5', '6', '12'])
    expect(screen.getAllByText('…')).toHaveLength(2)
    expect(screen.getByRole('button', { name: 'Page 5' })).toHaveAttribute(
      'aria-current',
      'page',
    )
  })

  it('disables Previous on the first page and Next on the last page', () => {
    const { rerender } = render(
      <Pagination page={0} totalPages={2} onPageChange={vi.fn()} />,
    )
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled()

    rerender(<Pagination page={1} totalPages={2} onPageChange={vi.fn()} />)
    expect(screen.getByRole('button', { name: 'Previous' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
  })

  it('reports the chosen zero-based page', async () => {
    const user = userEvent.setup()
    const onPageChange = vi.fn()
    render(<Pagination page={0} totalPages={3} onPageChange={onPageChange} />)

    await user.click(screen.getByRole('button', { name: 'Page 3' }))
    await user.click(screen.getByRole('button', { name: 'Next' }))

    expect(onPageChange.mock.calls).toEqual([[2], [1]])
  })
})
