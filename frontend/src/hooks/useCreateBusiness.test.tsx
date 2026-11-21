import type { ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/client'
import { useCreateBusiness } from '@/hooks/useCreateBusiness'
import { meQueryKey } from '@/hooks/useMe'
import { myBusinessQueryKey } from '@/hooks/useMyBusiness'
import type { User } from '@/services/auth'
import { createBusiness, type BusinessRequest } from '@/services/businesses'

vi.mock('@/services/businesses', () => ({
  createBusiness: vi.fn(),
}))

const client: User = {
  id: '01a1e961-b3ee-7311-b858-2b0d43a5cd74',
  firstName: 'Jan',
  lastName: 'Testowy',
  email: 'jan@test.pl',
  roles: ['ROLE_CLIENT'],
}

const request: BusinessRequest = {
  name: 'Barber Shop',
  categoryCode: 'BARBER',
  phone: '123456789',
  street: 'Floriańska 1',
  city: 'Kraków',
  postalCode: '31-019',
}

function renderCreateBusiness() {
  const queryClient = new QueryClient({
    defaultOptions: {
      mutations: {
        retry: false,
      },
    },
  })
  queryClient.setQueryData(meQueryKey, client)
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  const { result } = renderHook(() => useCreateBusiness(), { wrapper })
  return { result, queryClient }
}

describe('useCreateBusiness', () => {
  beforeEach(() => {
    vi.mocked(createBusiness).mockReset()
  })

  it('adds ROLE_OWNER to the cached user after the server creates the business', async () => {
    vi.mocked(createBusiness).mockResolvedValue({
      publicId: 'k3f9x2ab',
      slug: 'k3f9x2ab-barber-shop-krakow',
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
    })
    const { result, queryClient } = renderCreateBusiness()

    await act(() => result.current.mutateAsync(request))

    expect(queryClient.getQueryData<User>(meQueryKey)?.roles).toEqual([
      'ROLE_CLIENT',
      'ROLE_OWNER',
    ])
    expect(queryClient.getQueryData(myBusinessQueryKey)).toMatchObject({
      publicId: 'k3f9x2ab',
    })
  })

  it('leaves the cached user unchanged when the server rejects the request', async () => {
    vi.mocked(createBusiness).mockRejectedValue(
      new ApiError({
        status: 409,
        detail: 'You already have a business',
        code: 'BUSINESS_ALREADY_EXISTS',
      }),
    )
    const { result, queryClient } = renderCreateBusiness()

    await act(async () => {
      await expect(result.current.mutateAsync(request)).rejects.toThrow(
        'You already have a business',
      )
    })

    expect(queryClient.getQueryData<User>(meQueryKey)?.roles).toEqual([
      'ROLE_CLIENT',
    ])
    expect(queryClient.getQueryData(myBusinessQueryKey)).toBeUndefined()
  })
})
