import { client, unwrap } from '@/api/client'
import type { components } from '@/api/schema'

export type Business = components['schemas']['BusinessResponse']
export type BusinessRequest = components['schemas']['BusinessRequest']
export type BusinessCategory = components['schemas']['BusinessCategoryResponse']

export async function listCategories(): Promise<BusinessCategory[]> {
  return unwrap(await client.GET('/api/v1/categories'))
}

export async function getMyBusiness(): Promise<Business> {
  return unwrap(await client.GET('/api/v1/owner/business'))
}

export async function createBusiness(body: BusinessRequest): Promise<Business> {
  return unwrap(await client.POST('/api/v1/owner/business', { body }))
}

export async function updateMyBusiness(
  body: BusinessRequest,
): Promise<Business> {
  return unwrap(await client.PUT('/api/v1/owner/business', { body }))
}
