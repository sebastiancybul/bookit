import { useState } from 'react'
import { PlusIcon } from 'lucide-react'
import { OfferingForm } from '@/components/elements/OfferingForm'
import { PersonAvatar } from '@/components/elements/PersonAvatar'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useCreateOffering } from '@/hooks/useCreateOffering'
import { useDeleteOffering } from '@/hooks/useDeleteOffering'
import { useMyEmployees } from '@/hooks/useMyEmployees'
import { useMyOfferings } from '@/hooks/useMyOfferings'
import { useUpdateOffering } from '@/hooks/useUpdateOffering'
import { formatDuration, formatPrice } from '@/lib/format'
import type { Employee } from '@/services/employees'
import type { Offering, OfferingRequest } from '@/services/offerings'

type EditorState =
  | {
      mode: 'create'
    }
  | {
      mode: 'edit'
      offering: Offering
    }

export function OwnerOfferingsPage() {
  const { data: offerings, isPending, isError } = useMyOfferings()
  const { data: employees = [] } = useMyEmployees()
  const createMutation = useCreateOffering()
  const updateMutation = useUpdateOffering()
  const deleteMutation = useDeleteOffering()
  const [editor, setEditor] = useState<EditorState | null>(null)
  const [deleting, setDeleting] = useState<Offering | null>(null)

  const closeEditor = () => setEditor(null)

  const saveOffering = async (request: OfferingRequest) => {
    if (editor?.mode === 'edit') {
      await updateMutation.mutateAsync({
        id: editor.offering.id,
        body: request,
      })
    } else {
      await createMutation.mutateAsync(request)
    }
    closeEditor()
  }

  const openDelete = (offering: Offering) => {
    deleteMutation.reset()
    setDeleting(offering)
  }

  const confirmDelete = () => {
    if (deleting) {
      deleteMutation.mutate(deleting.id, { onSuccess: () => setDeleting(null) })
    }
  }

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-4xl">Services</h1>
          <p className="text-muted-foreground">
            What clients can book. Assign specialists in Employees.
          </p>
        </div>
        <Button
          size="lg"
          className="self-start sm:self-auto"
          onClick={() => setEditor({ mode: 'create' })}
        >
          <PlusIcon aria-hidden="true" />
          Add service
        </Button>
      </div>

      {isPending && <p className="text-muted-foreground">Loading…</p>}

      {isError && !offerings && (
        <p
          role="alert"
          className="rounded-lg bg-destructive/10 px-3.5 py-3 text-sm font-medium text-destructive"
        >
          We couldn't load your services. Please refresh the page.
        </p>
      )}

      {offerings && offerings.length === 0 && (
        <div className="flex flex-col items-start gap-2 rounded-[20px] border border-dashed border-border bg-card p-8">
          <h2 className="text-xl">No services yet</h2>
          <p className="text-muted-foreground">
            Add your first service so clients can book it.
          </p>
        </div>
      )}

      {offerings && offerings.length > 0 && (
        <div className="overflow-x-auto rounded-[20px] border border-border bg-card">
          <table className="w-full min-w-[42rem] border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-border">
                <th scope="col" className="px-5 py-4 font-semibold">
                  Service
                </th>
                <th scope="col" className="px-5 py-4 font-semibold">
                  Duration
                </th>
                <th scope="col" className="px-5 py-4 font-semibold">
                  Price
                </th>
                <th scope="col" className="px-5 py-4 font-semibold">
                  Specialists
                </th>
                <th scope="col" className="px-5 py-4">
                  <span className="sr-only">Actions</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {offerings.map((offering) => (
                <tr
                  key={offering.id}
                  className="border-b border-border last:border-b-0"
                >
                  <td className="px-5 py-4">
                    <span className="font-semibold">{offering.name}</span>
                    {offering.bufferMinutes > 0 && (
                      <span className="block text-xs text-muted-foreground">
                        + {formatDuration(offering.bufferMinutes)} break
                      </span>
                    )}
                  </td>
                  <td className="px-5 py-4">
                    {formatDuration(offering.durationMinutes)}
                  </td>
                  <td className="px-5 py-4">
                    {formatPrice(offering.priceAmount, offering.currency)}
                  </td>
                  <td className="px-5 py-4">
                    <Specialists
                      people={employees.filter((employee) =>
                        employee.offeringIds.includes(offering.id),
                      )}
                    />
                  </td>
                  <td className="px-5 py-4 text-right whitespace-nowrap">
                    <Button
                      variant="outline"
                      size="sm"
                      aria-label={`Edit ${offering.name}`}
                      onClick={() => setEditor({ mode: 'edit', offering })}
                    >
                      Edit
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      className="ml-1.5 text-destructive hover:text-destructive"
                      aria-label={`Delete ${offering.name}`}
                      onClick={() => openDelete(offering)}
                    >
                      Delete
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Dialog
        open={editor !== null}
        onOpenChange={(open) => !open && closeEditor()}
      >
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle className="text-xl">
              {editor?.mode === 'edit' ? 'Edit service' : 'Add service'}
            </DialogTitle>
            <DialogDescription>
              Clients see the name, description, duration and price.
            </DialogDescription>
          </DialogHeader>
          {editor && (
            <OfferingForm
              key={editor.mode === 'edit' ? editor.offering.id : 'new'}
              offering={editor.mode === 'edit' ? editor.offering : undefined}
              onSubmit={saveOffering}
              onCancel={closeEditor}
            />
          )}
        </DialogContent>
      </Dialog>

      <AlertDialog
        open={deleting !== null}
        onOpenChange={(open) => !open && setDeleting(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete {deleting?.name}?</AlertDialogTitle>
            <AlertDialogDescription>
              Clients won't be able to book it anymore.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deleteMutation.isError && (
            <p role="alert" className="text-sm font-medium text-destructive">
              We couldn't delete this service. Please try again.
            </p>
          )}
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={deleteMutation.isPending}
              onClick={confirmDelete}
            >
              {deleteMutation.isPending ? 'Deleting…' : 'Delete'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}

type SpecialistsProps = {
  people: Employee[]
}

function Specialists({ people }: SpecialistsProps) {
  if (people.length === 0) {
    return <span className="text-muted-foreground">None</span>
  }
  return (
    <span className="flex">
      <span className="sr-only">
        {people.map((person) => person.displayName).join(', ')}
      </span>
      {people.map((person, index) => (
        <span key={person.id} title={person.displayName}>
          <PersonAvatar
            name={person.displayName}
            seed={person.id}
            size="sm"
            className={
              index === 0 ? 'ring-2 ring-card' : '-ml-2 ring-2 ring-card'
            }
          />
        </span>
      ))}
    </span>
  )
}
