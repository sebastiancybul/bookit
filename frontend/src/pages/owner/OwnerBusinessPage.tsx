import { useState } from 'react'
import { BusinessForm } from '@/components/elements/BusinessForm'
import { useMyBusiness } from '@/hooks/useMyBusiness'
import { useUpdateMyBusiness } from '@/hooks/useUpdateMyBusiness'
import type { BusinessRequest } from '@/services/businesses'

export function OwnerBusinessPage() {
  const { data: business, isPending, isError } = useMyBusiness()
  const updateMutation = useUpdateMyBusiness()
  const [saved, setSaved] = useState(false)

  const saveBusiness = async (request: BusinessRequest) => {
    setSaved(false)
    await updateMutation.mutateAsync(request)
    setSaved(true)
  }

  return (
    <div className="flex max-w-2xl flex-col gap-8">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-4xl">Business details</h1>
        <p className="text-muted-foreground">
          What clients see on your public page.
        </p>
      </div>

      {isPending && <p className="text-muted-foreground">Loading…</p>}

      {isError && !business && (
        <p
          role="alert"
          className="rounded-lg bg-destructive/10 px-3.5 py-3 text-sm font-medium text-destructive"
        >
          We couldn't load your business. Please refresh the page.
        </p>
      )}

      {business && (
        <div
          onChange={() => setSaved(false)}
          className="flex flex-col gap-4 rounded-[20px] border border-border bg-card p-6 sm:p-8"
        >
          {saved && (
            <p
              role="status"
              className="rounded-lg bg-secondary px-3.5 py-3 text-sm font-medium"
            >
              Changes saved.
            </p>
          )}
          <BusinessForm
            business={business}
            submitLabel="Save changes"
            pendingLabel="Saving…"
            onSubmit={saveBusiness}
          />
        </div>
      )}
    </div>
  )
}
