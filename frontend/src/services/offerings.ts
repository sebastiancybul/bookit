import { client, unwrap } from '@/api/client'
import type { components } from '@/api/schema'

export type Offering = components['schemas']['OfferingResponse']
export type OfferingRequest = components['schemas']['OfferingRequest']
export type PublicOffering = components['schemas']['PublicOfferingResponse']

export async function listMyOfferings(): Promise<Offering[]> {
  return unwrap(await client.GET('/api/v1/owner/business/offerings'))
}

export async function createOffering(body: OfferingRequest): Promise<Offering> {
  return unwrap(await client.POST('/api/v1/owner/business/offerings', { body }))
}

export async function updateOffering({
  id,
  body,
}: {
  id: string
  body: OfferingRequest
}): Promise<Offering> {
  return unwrap(
    await client.PUT('/api/v1/owner/business/offerings/{id}', {
      params: { path: { id } },
      body,
    }),
  )
}

export async function deleteOffering(id: string): Promise<void> {
  unwrap(
    await client.DELETE('/api/v1/owner/business/offerings/{id}', {
      params: { path: { id } },
    }),
  )
}

export async function listBusinessOfferings(
  slug: string,
): Promise<PublicOffering[]> {
  return unwrap(
    await client.GET('/api/v1/businesses/{slug}/offerings', {
      params: { path: { slug } },
    }),
  )
}
