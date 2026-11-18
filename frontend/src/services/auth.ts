import { ApiError, client, unwrap } from '@/api/client'
import type { components } from '@/api/schema'

export type User = components['schemas']['UserResponse']
export type LoginRequest = components['schemas']['LoginRequest']
export type RegisterRequest = components['schemas']['RegisterRequest']

export async function getMe(): Promise<User | null> {
  try {
    return unwrap(await client.GET('/api/v1/auth/me'))
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null
    }
    throw error
  }
}

export async function login(body: LoginRequest): Promise<User> {
  return unwrap(await client.POST('/api/v1/auth/login', { body }))
}

export async function register(body: RegisterRequest): Promise<User> {
  return unwrap(await client.POST('/api/v1/auth/register', { body }))
}

export async function logout(): Promise<void> {
  unwrap(await client.POST('/api/v1/auth/logout'))
}
