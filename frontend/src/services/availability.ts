import { client, unwrap } from '@/api/client'
import type { components } from '@/api/schema'

export type AvailableSlot = components['schemas']['AvailableSlotResponse']

export type AvailabilityQuery = {
  slug: string
  offeringId: string
  employeeId: string
  date: string
}

export async function getAvailableSlots({
  slug,
  offeringId,
  employeeId,
  date,
}: AvailabilityQuery): Promise<AvailableSlot[]> {
  return unwrap(
    await client.GET('/api/v1/businesses/{slug}/availability', {
      params: {
        path: { slug },
        query: { offeringId, employeeId, date },
      },
    }),
  )
}
