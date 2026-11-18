import { afterEach, describe, expect, it, vi } from 'vitest'
import { csrfMiddleware } from '@/api/client'

type OnRequestParams = Parameters<
  NonNullable<typeof csrfMiddleware.onRequest>
>[0]

function runMiddleware(method: string) {
  const request = new Request('http://localhost/api/v1/auth/login', { method })
  return csrfMiddleware.onRequest!({ request } as OnRequestParams)
}

function setXsrfCookie(value: string) {
  document.cookie = `XSRF-TOKEN=${value}; path=/`
}

describe('csrfMiddleware', () => {
  afterEach(() => {
    document.cookie =
      'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
    vi.unstubAllGlobals()
  })

  it('leaves safe methods untouched', async () => {
    setXsrfCookie('existing-token')

    expect(await runMiddleware('GET')).toBeUndefined()
  })

  it.each(['POST', 'PUT', 'PATCH', 'DELETE'])(
    'copies the XSRF-TOKEN cookie into the header for %s',
    async (method) => {
      setXsrfCookie('existing-token')
      const fetchSpy = vi.fn()
      vi.stubGlobal('fetch', fetchSpy)

      const request = await runMiddleware(method)

      expect(request).toBeInstanceOf(Request)
      expect((request as Request).headers.get('X-XSRF-TOKEN')).toBe(
        'existing-token',
      )
      expect(fetchSpy).not.toHaveBeenCalled()
    },
  )

  it('fetches a token first when the cookie is missing', async () => {
    const fetchSpy = vi.fn(async () => {
      setXsrfCookie('fresh-token')
      return new Response(null, { status: 204 })
    })
    vi.stubGlobal('fetch', fetchSpy)

    const request = await runMiddleware('POST')

    expect(fetchSpy).toHaveBeenCalledWith('/api/v1/csrf')
    expect((request as Request).headers.get('X-XSRF-TOKEN')).toBe('fresh-token')
  })
})
