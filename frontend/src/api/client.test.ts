import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  ApiError,
  csrfMiddleware,
  unwrap,
  type ProblemDetail,
} from '@/api/client'

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

function thrownBy(action: () => unknown): unknown {
  try {
    action()
  } catch (error) {
    return error
  }
  throw new Error('Expected the action to throw')
}

describe('unwrap', () => {
  it('returns data for a 2xx response', () => {
    const data = {
      id: '1',
      email: 'jan@test.pl',
    }

    const result = unwrap({
      data,
      response: new Response(null, { status: 200 }),
    })

    expect(result).toBe(data)
  })

  it('throws ApiError carrying the ProblemDetail from the body', () => {
    const problem: ProblemDetail = {
      status: 409,
      title: 'Conflict',
      detail: 'An account with this email already exists',
      code: 'EMAIL_TAKEN',
    }

    const error = thrownBy(() =>
      unwrap({
        error: problem,
        response: new Response(null, { status: 409 }),
      }),
    )

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(409)
    expect((error as ApiError).problem).toEqual(problem)
    expect((error as ApiError).message).toBe(
      'An account with this email already exists',
    )
  })

  it('falls back to the response status when the body is not a ProblemDetail', () => {
    const error = thrownBy(() =>
      unwrap({
        error: '<html>Bad Gateway</html>',
        response: new Response(null, {
          status: 502,
          statusText: 'Bad Gateway',
        }),
      }),
    )

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).problem).toEqual({
      status: 502,
      title: 'Bad Gateway',
    })
  })
})
