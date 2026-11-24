import { useState } from 'react'
import { PlusIcon } from 'lucide-react'
import { EmployeeAccountForm } from '@/components/elements/EmployeeAccountForm'
import { EmployeeForm } from '@/components/elements/EmployeeForm'
import { EmployeeOfferingsForm } from '@/components/elements/EmployeeOfferingsForm'
import { EmployeeScheduleEditor } from '@/components/elements/EmployeeScheduleEditor'
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
import { useAssignEmployeeOfferings } from '@/hooks/useAssignEmployeeOfferings'
import { useCreateEmployee } from '@/hooks/useCreateEmployee'
import { useDeleteEmployee } from '@/hooks/useDeleteEmployee'
import { useLinkEmployeeAccount } from '@/hooks/useLinkEmployeeAccount'
import { useMyEmployees } from '@/hooks/useMyEmployees'
import { useMyOfferings } from '@/hooks/useMyOfferings'
import { useUnlinkEmployeeAccount } from '@/hooks/useUnlinkEmployeeAccount'
import { useUpdateEmployee } from '@/hooks/useUpdateEmployee'
import type { Employee, EmployeeRequest } from '@/services/employees'

type DialogState =
  | {
      kind: 'create'
    }
  | {
      kind: 'edit'
      employee: Employee
    }
  | {
      kind: 'offerings'
      employee: Employee
    }
  | {
      kind: 'account'
      employee: Employee
    }
  | {
      kind: 'schedule'
      employee: Employee
    }

const dialogTitles: Record<DialogState['kind'], string> = {
  create: 'Add employee',
  edit: 'Edit employee',
  offerings: 'Services',
  account: 'Bookit account',
  schedule: 'Working hours',
}

const dialogDescriptions: Record<DialogState['kind'], string> = {
  create: 'Clients see the name and position when they book.',
  edit: 'Clients see the name and position when they book.',
  offerings: 'Choose what this person can be booked for.',
  account: 'A linked account lets this person see their own appointments.',
  schedule:
    'Clients can book this person only within these hours. Existing appointments stay as they are.',
}

export function OwnerEmployeesPage() {
  const { data: employees, isPending, isError } = useMyEmployees()
  const { data: offerings = [] } = useMyOfferings()
  const createMutation = useCreateEmployee()
  const updateMutation = useUpdateEmployee()
  const assignMutation = useAssignEmployeeOfferings()
  const linkMutation = useLinkEmployeeAccount()
  const unlinkMutation = useUnlinkEmployeeAccount()
  const deleteMutation = useDeleteEmployee()
  const [dialog, setDialog] = useState<DialogState | null>(null)
  const [wideDialog, setWideDialog] = useState(false)
  const [deleting, setDeleting] = useState<Employee | null>(null)

  const openDialog = (next: DialogState) => {
    setWideDialog(next.kind === 'schedule')
    setDialog(next)
  }

  const closeDialog = () => setDialog(null)

  const closeScheduleOf = (employee: Employee) =>
    setDialog((current) =>
      current?.kind === 'schedule' && current.employee.id === employee.id
        ? null
        : current,
    )

  const offeringNames = (employee: Employee) =>
    offerings
      .filter((offering) => employee.offeringIds.includes(offering.id))
      .map((offering) => offering.name)

  const saveEmployee = async (request: EmployeeRequest) => {
    if (dialog?.kind === 'edit') {
      await updateMutation.mutateAsync({
        id: dialog.employee.id,
        body: request,
      })
    } else {
      await createMutation.mutateAsync(request)
    }
    closeDialog()
  }

  const saveOfferings = async (employee: Employee, offeringIds: string[]) => {
    await assignMutation.mutateAsync({ id: employee.id, offeringIds })
    closeDialog()
  }

  const linkAccount = async (employee: Employee, email: string) => {
    await linkMutation.mutateAsync({ id: employee.id, email })
    closeDialog()
  }

  const unlinkAccount = async (employee: Employee) => {
    await unlinkMutation.mutateAsync(employee.id)
    closeDialog()
  }

  const openDelete = (employee: Employee) => {
    deleteMutation.reset()
    setDeleting(employee)
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
          <h1 className="text-4xl">Employees</h1>
          <p className="text-muted-foreground">
            Who works here and what they can be booked for.
          </p>
        </div>
        <Button
          size="lg"
          className="self-start sm:self-auto"
          onClick={() => openDialog({ kind: 'create' })}
        >
          <PlusIcon aria-hidden="true" />
          Add employee
        </Button>
      </div>

      {isPending && <p className="text-muted-foreground">Loading…</p>}

      {isError && !employees && (
        <p
          role="alert"
          className="rounded-lg bg-destructive/10 px-3.5 py-3 text-sm font-medium text-destructive"
        >
          We couldn't load your employees. Please refresh the page.
        </p>
      )}

      {employees && employees.length === 0 && (
        <div className="flex flex-col items-start gap-2 rounded-[20px] border border-dashed border-border bg-card p-8">
          <h2 className="text-xl">No employees yet</h2>
          <p className="text-muted-foreground">
            Add the people clients can book, including yourself if you work with
            clients.
          </p>
        </div>
      )}

      {employees && employees.length > 0 && (
        <div className="overflow-x-auto rounded-[20px] border border-border bg-card">
          <table className="w-full min-w-[52rem] border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-border">
                <th scope="col" className="px-5 py-4 font-semibold">
                  Employee
                </th>
                <th scope="col" className="px-5 py-4 font-semibold">
                  Services
                </th>
                <th scope="col" className="px-5 py-4 font-semibold">
                  Account
                </th>
                <th scope="col" className="px-5 py-4">
                  <span className="sr-only">Actions</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {employees.map((employee) => {
                const names = offeringNames(employee)
                return (
                  <tr
                    key={employee.id}
                    className="border-b border-border last:border-b-0"
                  >
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-3">
                        <PersonAvatar
                          name={employee.displayName}
                          seed={employee.id}
                        />
                        <div className="flex min-w-0 flex-col">
                          <span className="font-semibold">
                            {employee.displayName}
                          </span>
                          {employee.position && (
                            <span className="text-xs text-muted-foreground">
                              {employee.position}
                            </span>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-4">
                      {names.length > 0 ? (
                        names.join(', ')
                      ) : (
                        <span className="text-muted-foreground">
                          No services
                        </span>
                      )}
                    </td>
                    <td className="px-5 py-4">
                      {employee.accountEmail ?? (
                        <span className="text-muted-foreground">
                          Not linked
                        </span>
                      )}
                    </td>
                    <td className="px-5 py-4 text-right whitespace-nowrap">
                      <Button
                        variant="outline"
                        size="sm"
                        aria-label={`Edit ${employee.displayName}`}
                        onClick={() => openDialog({ kind: 'edit', employee })}
                      >
                        Edit
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="ml-1.5"
                        aria-label={`Services of ${employee.displayName}`}
                        onClick={() =>
                          openDialog({ kind: 'offerings', employee })
                        }
                      >
                        Services
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="ml-1.5"
                        aria-label={`Working hours of ${employee.displayName}`}
                        onClick={() =>
                          openDialog({ kind: 'schedule', employee })
                        }
                      >
                        Hours
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="ml-1.5"
                        aria-label={`Account of ${employee.displayName}`}
                        onClick={() =>
                          openDialog({ kind: 'account', employee })
                        }
                      >
                        Account
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="ml-1.5 text-destructive hover:text-destructive"
                        aria-label={`Delete ${employee.displayName}`}
                        onClick={() => openDelete(employee)}
                      >
                        Delete
                      </Button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      <Dialog
        open={dialog !== null}
        onOpenChange={(open) => !open && closeDialog()}
      >
        <DialogContent
          className={
            wideDialog
              ? 'max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-2xl'
              : 'sm:max-w-lg'
          }
        >
          {dialog && (
            <>
              <DialogHeader>
                <DialogTitle className="text-xl">
                  {dialog.kind === 'create' || dialog.kind === 'edit'
                    ? dialogTitles[dialog.kind]
                    : `${dialogTitles[dialog.kind]}: ${dialog.employee.displayName}`}
                </DialogTitle>
                <DialogDescription>
                  {dialogDescriptions[dialog.kind]}
                </DialogDescription>
              </DialogHeader>

              {dialog.kind === 'create' && (
                <EmployeeForm onSubmit={saveEmployee} onCancel={closeDialog} />
              )}
              {dialog.kind === 'edit' && (
                <EmployeeForm
                  key={dialog.employee.id}
                  employee={dialog.employee}
                  onSubmit={saveEmployee}
                  onCancel={closeDialog}
                />
              )}
              {dialog.kind === 'offerings' && (
                <EmployeeOfferingsForm
                  key={dialog.employee.id}
                  employee={dialog.employee}
                  offerings={offerings}
                  onSubmit={(offeringIds) =>
                    saveOfferings(dialog.employee, offeringIds)
                  }
                  onCancel={closeDialog}
                />
              )}
              {dialog.kind === 'schedule' && (
                <EmployeeScheduleEditor
                  key={dialog.employee.id}
                  employee={dialog.employee}
                  onSaved={() => closeScheduleOf(dialog.employee)}
                  onCancel={closeDialog}
                />
              )}
              {dialog.kind === 'account' && (
                <EmployeeAccountForm
                  key={dialog.employee.id}
                  employee={dialog.employee}
                  onLink={(email) => linkAccount(dialog.employee, email)}
                  onUnlink={() => unlinkAccount(dialog.employee)}
                  onCancel={closeDialog}
                />
              )}
            </>
          )}
        </DialogContent>
      </Dialog>

      <AlertDialog
        open={deleting !== null}
        onOpenChange={(open) => !open && setDeleting(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete {deleting?.displayName}?</AlertDialogTitle>
            <AlertDialogDescription>
              Clients won't be able to book them anymore. A linked account is
              unlinked.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deleteMutation.isError && (
            <p role="alert" className="text-sm font-medium text-destructive">
              We couldn't delete this employee. Please try again.
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
