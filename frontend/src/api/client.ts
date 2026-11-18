import createClient, { type Middleware } from 'openapi-fetch'
import type { paths } from '@/api/schema'

export type FieldValidationError = {
  field: string
  message: string
}

export type ProblemDetail = {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  code?: string
  errors?: FieldValidationError[]
}

const XSRF_COOKIE = 'XSRF-TOKEN'
const XSRF_HEADER = 'X-XSRF-TOKEN'
const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

function readCookie(name: string): string | undefined {
  const entry = document.cookie
    .split('; ')
    .find((cookie) => cookie.startsWith(`${name}=`))
  return entry ? decodeURIComponent(entry.slice(name.length + 1)) : undefined
}

async function xsrfToken(): Promise<string | undefined> {
  const existing = readCookie(XSRF_COOKIE)
  if (existing) {
    return existing
  }
  await fetch('/api/v1/csrf')
  return readCookie(XSRF_COOKIE)
}

export const csrfMiddleware: Middleware = {
  async onRequest({ request }) {
    if (!UNSAFE_METHODS.has(request.method)) {
      return undefined
    }
    const token = await xsrfToken()
    if (token) {
      request.headers.set(XSRF_HEADER, token)
    }
    return request
  },
}

export const client = createClient<paths>()
client.use(csrfMiddleware)
