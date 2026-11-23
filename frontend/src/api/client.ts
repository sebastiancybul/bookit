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

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `HTTP ${problem.status}`)
    this.name = 'ApiError'
    this.status = problem.status
    this.problem = problem
  }
}

const MAX_RETRIES = 2

export function retryUnlessClientError(failureCount: number, error: Error) {
  if (error instanceof ApiError && error.status < 500) {
    return false
  }
  return failureCount < MAX_RETRIES
}

function isProblemDetail(value: unknown): value is ProblemDetail {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ProblemDetail).status === 'number'
  )
}

type ApiResult<T> = {
  data?: T
  error?: unknown
  response: Response
}

export function unwrap<T>({ data, error, response }: ApiResult<T>): T {
  if (!response.ok) {
    throw new ApiError(
      isProblemDetail(error)
        ? error
        : { status: response.status, title: response.statusText },
    )
  }
  return data as T
}

export const client = createClient<paths>()
client.use(csrfMiddleware)
