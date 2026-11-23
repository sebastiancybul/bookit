import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import type { Employee, EmployeeRequest } from '@/services/employees'

const employeeSchema = z.object({
  displayName: z
    .string()
    .trim()
    .min(1, 'Enter the name clients will see')
    .max(80, 'Use at most 80 characters'),
  position: z.string().trim().max(80, 'Use at most 80 characters'),
})

type EmployeeFormValues = z.infer<typeof employeeSchema>

const serverFieldNames: Record<string, keyof EmployeeFormValues> = {
  displayName: 'displayName',
  position: 'position',
}

function toRequest(values: EmployeeFormValues): EmployeeRequest {
  return {
    displayName: values.displayName,
    position: values.position || undefined,
  }
}

type EmployeeFormProps = {
  employee?: Employee
  onSubmit: (request: EmployeeRequest) => Promise<unknown>
  onCancel: () => void
}

export function EmployeeForm({
  employee,
  onSubmit,
  onCancel,
}: EmployeeFormProps) {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<EmployeeFormValues>({
    resolver: zodResolver(employeeSchema),
    defaultValues: {
      displayName: employee?.displayName ?? '',
      position: employee?.position ?? '',
    },
  })

  const showServerErrors = (error: unknown) => {
    if (error instanceof ApiError) {
      if (error.problem.code === 'EMPLOYEE_NAME_TAKEN') {
        setError('displayName', {
          message: 'You already have an employee with this name',
        })
        return
      }
      const fieldErrors = (error.problem.errors ?? []).filter((fieldError) =>
        Object.hasOwn(serverFieldNames, fieldError.field),
      )
      if (fieldErrors.length > 0) {
        fieldErrors.forEach((fieldError) =>
          setError(serverFieldNames[fieldError.field], {
            message: fieldError.message,
          }),
        )
        return
      }
    }
    setError('root', { message: 'Something went wrong. Please try again.' })
  }

  const submit = async (values: EmployeeFormValues) => {
    try {
      await onSubmit(toRequest(values))
    } catch (error) {
      showServerErrors(error)
    }
  }

  const submitLabel = employee ? 'Save changes' : 'Add employee'

  return (
    <form
      noValidate
      onSubmit={handleSubmit(submit)}
      className="flex flex-col gap-4"
    >
      {errors.root && (
        <p
          role="alert"
          className="rounded-lg bg-destructive/10 px-3.5 py-3 text-sm font-medium text-destructive"
        >
          {errors.root.message}
        </p>
      )}

      <Field data-invalid={!!errors.displayName}>
        <FieldLabel htmlFor="employee-display-name">Name</FieldLabel>
        <Input
          id="employee-display-name"
          required
          maxLength={80}
          placeholder="Anna K."
          aria-invalid={!!errors.displayName}
          {...register('displayName')}
        />
        <FieldError errors={[errors.displayName]} />
      </Field>

      <Field data-invalid={!!errors.position}>
        <FieldLabel htmlFor="employee-position">Position (optional)</FieldLabel>
        <Input
          id="employee-position"
          maxLength={80}
          placeholder="Senior barber"
          aria-invalid={!!errors.position}
          {...register('position')}
        />
        <FieldError errors={[errors.position]} />
      </Field>

      <div className="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-end">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Saving…' : submitLabel}
        </Button>
      </div>
    </form>
  )
}
