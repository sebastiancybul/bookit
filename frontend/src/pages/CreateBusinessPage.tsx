import { Navigate, useNavigate } from 'react-router'
import { BusinessForm } from '@/components/elements/BusinessForm'
import { useCreateBusiness } from '@/hooks/useCreateBusiness'
import { useMe } from '@/hooks/useMe'
import type { BusinessRequest } from '@/services/businesses'

export function CreateBusinessPage() {
  const { data: user } = useMe()
  const createMutation = useCreateBusiness()
  const navigate = useNavigate()

  if (user?.roles.includes('ROLE_OWNER')) {
    return <Navigate to="/owner" replace />
  }

  const createBusiness = async (request: BusinessRequest) => {
    await createMutation.mutateAsync(request)
    await navigate('/owner', { replace: true })
  }

  return (
    <section className="mx-auto w-full max-w-2xl px-4 py-10 sm:py-16">
      <div className="flex flex-col gap-6 rounded-3xl border border-border bg-card p-6 sm:p-9">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-3xl">Create your business</h1>
          <p className="text-muted-foreground">
            Add your salon or studio. You can change these details any time in
            the owner panel.
          </p>
        </div>

        <BusinessForm
          submitLabel="Create business"
          pendingLabel="Creating business…"
          onSubmit={createBusiness}
        />
      </div>
    </section>
  )
}
