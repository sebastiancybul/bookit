import { client, unwrap } from '@/api/client'
import type { components } from '@/api/schema'

export type Employee = components['schemas']['EmployeeResponse']
export type EmployeeRequest = components['schemas']['EmployeeRequest']
export type PublicEmployee = components['schemas']['PublicEmployeeResponse']

export async function listMyEmployees(): Promise<Employee[]> {
  return unwrap(await client.GET('/api/v1/owner/business/employees'))
}

export async function createEmployee(body: EmployeeRequest): Promise<Employee> {
  return unwrap(await client.POST('/api/v1/owner/business/employees', { body }))
}

export async function updateEmployee({
  id,
  body,
}: {
  id: string
  body: EmployeeRequest
}): Promise<Employee> {
  return unwrap(
    await client.PUT('/api/v1/owner/business/employees/{id}', {
      params: { path: { id } },
      body,
    }),
  )
}

export async function assignEmployeeOfferings({
  id,
  offeringIds,
}: {
  id: string
  offeringIds: string[]
}): Promise<Employee> {
  return unwrap(
    await client.PUT('/api/v1/owner/business/employees/{id}/offerings', {
      params: { path: { id } },
      body: { offeringIds },
    }),
  )
}

export async function linkEmployeeAccount({
  id,
  email,
}: {
  id: string
  email: string
}): Promise<Employee> {
  return unwrap(
    await client.PUT('/api/v1/owner/business/employees/{id}/account', {
      params: { path: { id } },
      body: { email },
    }),
  )
}

export async function unlinkEmployeeAccount(id: string): Promise<void> {
  unwrap(
    await client.DELETE('/api/v1/owner/business/employees/{id}/account', {
      params: { path: { id } },
    }),
  )
}

export async function deleteEmployee(id: string): Promise<void> {
  unwrap(
    await client.DELETE('/api/v1/owner/business/employees/{id}', {
      params: { path: { id } },
    }),
  )
}

export async function listBusinessEmployees(
  slug: string,
): Promise<PublicEmployee[]> {
  return unwrap(
    await client.GET('/api/v1/businesses/{slug}/employees', {
      params: { path: { slug } },
    }),
  )
}
