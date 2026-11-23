import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import type { Employee } from '@/services/employees'

const accountSchema = z.object({
  email: z
    .string()
    .trim()
    .pipe(
      z
        .email('Enter a valid email address')
        .max(255, 'Use at most 255 characters'),
    ),
})

type AccountFormValues = z.infer<typeof accountSchema>

type EmployeeAccountFormProps = {
  employee: Employee
  onLink: (email: string) => Promise<unknown>
  onUnlink: () => Promise<unknown>
  onCancel: () => void
}

export function EmployeeAccountForm({
  employee,
  onLink,
  onUnlink,
  onCancel,
}: EmployeeAccountFormProps) {
  const [isUnlinking, setIsUnlinking] = useState(false)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<AccountFormValues>({
    resolver: zodResolver(accountSchema),
    defaultValues: {
      email: '',
    },
  })

  const showServerErrors = (error: unknown) => {
    if (error instanceof ApiError) {
      if (error.problem.code === 'EMPLOYEE_ACCOUNT_TAKEN') {
        setError('email', {
          message: 'This account already belongs to an employee',
        })
        return
      }
      if (error.problem.errors?.some(({ field }) => field === 'email')) {
        setError('email', {
          message: 'No Bookit account uses this email',
        })
        return
      }
    }
    setError('root', { message: 'Something went wrong. Please try again.' })
  }

  const link = async ({ email }: AccountFormValues) => {
    try {
      await onLink(email)
    } catch (error) {
      showServerErrors(error)
    }
  }

  const unlink = async () => {
    setIsUnlinking(true)
    try {
      await onUnlink()
    } catch {
      setError('root', { message: 'Something went wrong. Please try again.' })
    } finally {
      setIsUnlinking(false)
    }
  }

  return (
    <form
      noValidate
      onSubmit={handleSubmit(link)}
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

      {employee.accountEmail && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-muted px-4 py-3">
          <p className="text-sm">
            Linked to{' '}
            <span className="font-semibold">{employee.accountEmail}</span>
          </p>
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="text-destructive hover:text-destructive"
            disabled={isUnlinking}
            onClick={unlink}
          >
            {isUnlinking ? 'Unlinking…' : 'Unlink'}
          </Button>
        </div>
      )}

      <Field data-invalid={!!errors.email}>
        <FieldLabel htmlFor="employee-account-email">
          {employee.accountEmail ? 'Link another account' : 'Account email'}
        </FieldLabel>
        <Input
          id="employee-account-email"
          type="email"
          autoComplete="off"
          required
          maxLength={255}
          aria-invalid={!!errors.email}
          aria-describedby="employee-account-hint"
          {...register('email')}
        />
        <FieldDescription id="employee-account-hint">
          The person must already have a Bookit account. They will see the
          employee panel after they log in again.
        </FieldDescription>
        <FieldError errors={[errors.email]} />
      </Field>

      <div className="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-end">
        <Button type="button" variant="outline" onClick={onCancel}>
          Close
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Linking…' : 'Link account'}
        </Button>
      </div>
    </form>
  )
}
